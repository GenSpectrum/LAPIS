package ch.ethz.lapis.source.ng;

import ch.ethz.lapis.util.DeflateSeqCompressor;
import ch.ethz.lapis.util.FastaEntry;
import ch.ethz.lapis.util.FastaFileReader;
import ch.ethz.lapis.util.ReferenceGenomeData;
import ch.ethz.lapis.util.SeqCompressor;
import ch.ethz.lapis.util.Utils;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;

public class NextstrainGenbankMutationAAWorker {

    private final int id;
    private final ComboPooledDataSource databasePool;
    private final Path workDir;
    private final Path referenceFasta;
    private final Path geneMapGff;
    private final Path nextalignPath;
    private final SeqCompressor nucSeqCompressor = new DeflateSeqCompressor(DeflateSeqCompressor.DICT.REFERENCE);
    public NextstrainGenbankMutationAAWorker(
        int id,
        ComboPooledDataSource databasePool,
        Path workDir,
        Path referenceFasta,
        Path geneMapGff,
        Path nextalignPath) {
        this.id = id;
        this.databasePool = databasePool;
        this.workDir = workDir;
        this.referenceFasta = referenceFasta;
        this.geneMapGff = geneMapGff;
        this.nextalignPath = nextalignPath;
    }

    public void run(List<FastaEntry> batch) throws Exception {
        System.out.println("[" + id + "] Received " + batch.size() + " sequences.");

        // Find sequences that already exist and have not changed
        Map<String, String> seqMap = new HashMap<>();
        for (FastaEntry fastaEntry : batch) {
            seqMap.put(fastaEntry.getSampleName(), fastaEntry.getSeq());
        }
        String fetchSql = """
                select strain, seq_aligned_compressed
                from y_nextstrain_genbank
                where strain = any(?::text[]) and seq_aligned_compressed is not null;
            """;
        try (Connection conn = databasePool.getConnection()) {
            try (PreparedStatement statement = conn.prepareStatement(fetchSql)) {
                statement.setArray(1, conn.createArrayOf("text", seqMap.keySet().toArray()));
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        String sampleName = rs.getString("strain");
                        String seqUncompressed = nucSeqCompressor.decompress(
                            rs.getBytes("seq_aligned_compressed"));
                        if (seqMap.get(sampleName).equalsIgnoreCase(seqUncompressed)) {
                            seqMap.remove(sampleName);
                        }
                    }
                }
            }
        }
        System.out.println("[" + id + "] " + batch.size() + " sequences were changed.");
        if (batch.isEmpty()) {
            System.out.println("[" + id + "] Nothing to do. Bye bye.");
            return;
        }

        // Run Nextalign and read amino acid mutation sequences
        Path seqFastaPath = workDir.resolve("aligned.fasta");
        System.out.println("[" + id + "] Write fasta to disk..");
        Files.writeString(seqFastaPath, formatSeqAsFasta(batch));
        System.out.println("[" + id + "] Run Nextalign..");
        Map<String, List<GeneAASeq>> geneAASeqs = runNextalign(seqFastaPath, batch);

        // Write to database
        System.out.println("[" + id + "] Write to database");
        String sql = """
                insert into y_nextstrain_genbank (strain, aa_seqs)
                values (?, ?)
                on conflict (strain) do update
                set aa_seqs = ?;
            """;
        try (Connection conn = databasePool.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                for (Map.Entry<String, List<GeneAASeq>> mapEntry : geneAASeqs.entrySet()) {
                    String sampleName = mapEntry.getKey();
                    String aaSeqs = formatGeneAASeqs(mapEntry.getValue());
                    statement.setString(1, sampleName);
                    statement.setString(2, aaSeqs);
                    statement.setString(3, aaSeqs);
                    statement.addBatch();
                    Utils.executeClearCommitBatch(conn, statement);
                }
            }
            conn.setAutoCommit(true);
        }

        // Clean up workdir
        System.out.println("[" + id + "] Clean up");
        try (DirectoryStream<Path> directory = Files.newDirectoryStream(workDir)) {
            for (Path path : directory) {
                if (Files.isDirectory(path)) {
                    FileUtils.deleteDirectory(path.toFile());
                } else {
                    Files.delete(path);
                }
            }
        }
        System.out.println("[" + id + "] Finished");
    }

    private Map<String, List<GeneAASeq>> runNextalign(
        Path seqFastaPath,
        List<FastaEntry> batch
    ) throws IOException, InterruptedException {
        List<String> genes = ReferenceGenomeData.getInstance().getGeneNames();
        Path outputPath = workDir.resolve("output");
        String command = nextalignPath.toAbsolutePath() +
            " --sequences=" + seqFastaPath.toAbsolutePath() +
            " --reference=" + referenceFasta.toAbsolutePath() +
            " --genemap=" + geneMapGff.toAbsolutePath() +
            " --genes=" + String.join(",", genes) +
            " --output-dir=" + outputPath.toAbsolutePath() +
            " --output-basename=nextalign" +
            " --silent" +
            " --jobs=1";

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
            FastaFileReader fastaReader = new FastaFileReader(outputPath.resolve("nextalign.gene." + gene + ".fasta"),
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

    private static class GeneAASeq {

        private final String gene;
        private final String seq;

        public GeneAASeq(String gene, String seq) {
            this.gene = gene;
            this.seq = seq;
        }
    }

}
