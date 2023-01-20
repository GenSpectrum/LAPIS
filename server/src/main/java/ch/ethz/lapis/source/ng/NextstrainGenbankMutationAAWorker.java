package ch.ethz.lapis.source.ng;

import ch.ethz.lapis.source.MutationFinder;
import ch.ethz.lapis.util.FastaEntry;
import ch.ethz.lapis.util.FastaFileReader;
import ch.ethz.lapis.util.ReferenceGenomeData;
import ch.ethz.lapis.util.SeqCompressor;
import ch.ethz.lapis.util.Utils;
import ch.ethz.lapis.util.ZstdSeqCompressor;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This class will also compute and write aa_unknowns and nuc_unknowns.
 */
@Slf4j
public class NextstrainGenbankMutationAAWorker {

    private final int id;
    private final ComboPooledDataSource databasePool;
    private final Path workDir;
    private final Path referenceFasta;
    private final Path geneMapGff;
    private final Path nextalignPath;
    private final SeqCompressor aaSeqCompressor = new ZstdSeqCompressor(ZstdSeqCompressor.DICT.AA_REFERENCE);

    public NextstrainGenbankMutationAAWorker(
        int id,
        ComboPooledDataSource databasePool,
        Path workDir,
        Path referenceFasta,
        Path geneMapGff,
        Path nextalignPath
    ) {
        this.id = id;
        this.databasePool = databasePool;
        this.workDir = workDir;
        this.referenceFasta = referenceFasta;
        this.geneMapGff = geneMapGff;
        this.nextalignPath = nextalignPath;
    }

    /**
     * It expects aligned sequences as input because it directly uses the provided sequences for finding the
     * nuc_unknowns.
     */
    public void run(List<FastaEntry> batch) throws Exception {
        log.info(LocalDateTime.now() + " [" + id + "] Received " + batch.size() + " sequences.");

        // Run Nextalign and read amino acid mutation sequences
        Path seqFastaPath = workDir.resolve("aligned.fasta");
        log.info(LocalDateTime.now() + " [" + id + "] Write fasta to disk..");
        Files.writeString(seqFastaPath, formatSeqAsFasta(batch));
        log.info(LocalDateTime.now() + " [" + id + "] Run Nextalign..");
        Map<String, List<GeneAASeq>> geneAASeqs = runNextalign(seqFastaPath, batch);

        // Find the nuc_unknowns and aa_unknowns
        Map<String, String> nucUnknownsMap = new HashMap<>();
        Map<String, String> aaUnknownsMap = new HashMap<>();
        for (FastaEntry fastaEntry : batch) {
            String seq = fastaEntry.getSeq();
            String nucUnknowns = String.join(",", MutationFinder.compressPositionsAsStrings(
                MutationFinder.findNucUnknowns(seq)));
            nucUnknownsMap.put(fastaEntry.getSampleName(), nucUnknowns);
        }
        geneAASeqs.forEach((sampleName, aaSeqs) -> {
            List<String> aaUnknownsComponents = new ArrayList<>();
            for (GeneAASeq aaSeq : aaSeqs) {
                List<String> thisAAUnknowns = MutationFinder.compressPositionsAsStrings(
                    MutationFinder.findAAUnknowns(aaSeq.seq));
                aaUnknownsComponents.addAll(thisAAUnknowns.stream()
                    .map(u -> aaSeq.gene + ":" + u)
                    .toList());
            }
            String aaUnknowns = String.join(",", aaUnknownsComponents);
            aaUnknownsMap.put(sampleName, aaUnknowns);
        });

        // Write to database
        log.info(LocalDateTime.now() + " [" + id + "] Write to database");
        String sql = """
                insert into y_nextstrain_genbank (strain, aa_seqs_compressed, nuc_unknowns, aa_unknowns)
                values (?, ?, ?, ?)
                on conflict (strain) do update
                set
                  aa_seqs_compressed = ?,
                  nuc_unknowns = ?,
                  aa_unknowns = ?;
            """;
        try (Connection conn = databasePool.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                for (Map.Entry<String, List<GeneAASeq>> mapEntry : geneAASeqs.entrySet()) {
                    String sampleName = mapEntry.getKey();
                    String aaSeqs = formatGeneAASeqs(mapEntry.getValue());
                    byte[] aaSeqsCompressed = aaSeqCompressor.compress(aaSeqs);
                    String nucUnknowns = nucUnknownsMap.get(sampleName);
                    String aaUnknowns = aaUnknownsMap.get(sampleName);
                    statement.setString(1, sampleName);
                    statement.setBytes(2, aaSeqsCompressed);
                    statement.setString(3, nucUnknowns);
                    statement.setString(4, aaUnknowns);
                    statement.setBytes(5, aaSeqsCompressed);
                    statement.setString(6, nucUnknowns);
                    statement.setString(7, aaUnknowns);
                    statement.addBatch();
                    Utils.executeClearCommitBatch(conn, statement);
                }
            }
            conn.setAutoCommit(true);
        }

        // Clean up workdir
        log.info(LocalDateTime.now() + " [" + id + "] Clean up");
        try (DirectoryStream<Path> directory = Files.newDirectoryStream(workDir)) {
            for (Path path : directory) {
                if (Files.isDirectory(path)) {
                    FileUtils.deleteDirectory(path.toFile());
                } else {
                    Files.delete(path);
                }
            }
        }
        log.info(LocalDateTime.now() + " [" + id + "] Finished");
    }

    private Map<String, List<GeneAASeq>> runNextalign(
        Path seqFastaPath,
        List<FastaEntry> batch
    ) throws IOException, InterruptedException {
        List<String> genes = ReferenceGenomeData.getInstance().getGeneNames();
        Path outputPath = workDir.resolve("output");
        String command = nextalignPath.toAbsolutePath() +
            " run" +
            " --input-ref=" + referenceFasta.toAbsolutePath() +
            " --input-gene-map=" + geneMapGff.toAbsolutePath() +
            " --genes=" + String.join(",", genes) +
            " --output-all=" + outputPath.toAbsolutePath() +
            " --output-basename=nextalign" +
            " --silent" +
            " --jobs=1" +
            " " + seqFastaPath.toAbsolutePath();

        Process process = Runtime.getRuntime().exec(command);
        boolean exited = process.waitFor(20, TimeUnit.MINUTES);
        if (!exited) {
            process.destroyForcibly();
            throw new RuntimeException("Nextalign timed out (after 20 minutes)");
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("Nextalign exited with code " + process.exitValue());
        }

        // Read the output
        Map<String, List<GeneAASeq>> geneAASeqs = new HashMap<>();
        for (FastaEntry fastaEntry : batch) {
            geneAASeqs.put(fastaEntry.getSampleName(), new ArrayList<>());
        }
        for (String gene : genes) {
            FastaFileReader fastaReader = new FastaFileReader(outputPath.resolve("nextalign_gene_" + gene + ".translation.fasta"),
                false);
            for (FastaEntry fastaEntry : fastaReader) {
                geneAASeqs.get(fastaEntry.getSampleName()).add(
                    new GeneAASeq(gene, fastaEntry.getSeq())
                );
            }
        }

        return geneAASeqs;
    }

    /**
     * Format to the format: gene1:seq,gene2:seq,... where the genes are in alphabetical order. The dictionary used for
     * compression has the same format.
     */
    private String formatGeneAASeqs(List<GeneAASeq> geneAASeqs) {
        return geneAASeqs.stream()
            .sorted((s1, s2) -> s1.seq.compareTo(s2.gene))
            .map(s -> s.gene + ":" + s.seq)
            .collect(Collectors.joining(","));
    }

    private String formatSeqAsFasta(List<FastaEntry> sequences) {
        StringBuilder fasta = new StringBuilder();
        for (FastaEntry sequence : sequences) {
            fasta
                .append(">")
                .append(sequence.getSampleName())
                .append("\n")
                .append(sequence.getSeq())
                .append("\n\n");
        }
        return fasta.toString();
    }

    private record GeneAASeq(String gene, String seq) {
    }

}
