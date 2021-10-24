package ch.ethz.lapis.source.gisaid;

import ch.ethz.lapis.source.MutationAA;
import ch.ethz.lapis.source.MutationFinder;
import ch.ethz.lapis.source.MutationNuc;
import ch.ethz.lapis.util.*;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.commons.io.FileUtils;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;


public class BatchProcessingWorker {

    private final int id;
    private final Path workDir;
    private final Path referenceFasta;
    private final ComboPooledDataSource databasePool;
    private final boolean updateSubmitterInformation;
    private final SubmitterInformationFetcher submitterInformationFetcher = new SubmitterInformationFetcher();
    private final Path nextalignPath;
    private final Path geneMapGff;
    private final SeqCompressor seqCompressor;
    /**
     * @param id             An unique identifier for the worker
     * @param workDir        An empty work directory for the worker
     * @param referenceFasta The path to the fasta file containing the reference
     * @param nextalignPath
     * @param geneMapGff
     * @param seqCompressor
     */
    public BatchProcessingWorker(
        int id,
        Path workDir,
        Path referenceFasta,
        ComboPooledDataSource databasePool,
        boolean updateSubmitterInformation,
        Path nextalignPath,
        Path geneMapGff,
        SeqCompressor seqCompressor
    ) {
        this.databasePool = databasePool;
        this.id = id;
        this.workDir = workDir;
        this.referenceFasta = referenceFasta;
        this.updateSubmitterInformation = updateSubmitterInformation;
        this.nextalignPath = nextalignPath;
        this.geneMapGff = geneMapGff;
        this.seqCompressor = seqCompressor;
    }

    public BatchReport run(Batch batch) throws Exception {
        try {
            int batchSize = batch.getEntries().size();
            System.out.println("[" + id + "] Received a batch");

            // Remove entries from the batch where no sequence is provided -> very weird
            batch = new Batch(batch.getEntries().stream()
                .filter(s -> s.getSeqOriginal() != null && !s.getSeqOriginal().isBlank())
                .collect(Collectors.toList()));

            determineChangeSet(batch);

            // Fetch the submitter information for all APPEND sequences
            for (GisaidEntry entry : batch.getEntries()) {
                if (entry.getImportMode() == ImportMode.APPEND) {
                    submitterInformationFetcher.fetchSubmitterInformation(entry.getGisaidEpiIsl())
                        .ifPresent(entry::setSubmitterInformation);
                }
            }

            // Determine the entries that are new are have a changed sequence. Those sequences need to be processed
            // Nextalign.
            List<GisaidEntry> sequencePreprocessingNeeded = batch.getEntries().stream()
                .filter(s -> s.getImportMode() == ImportMode.APPEND || s.isSequenceChanged())
                .collect(Collectors.toList());

            System.out.println("[" + id + "] " + sequencePreprocessingNeeded.size() + " out of " + batchSize +
                " sequences are new or have changed sequence.");
            if (!sequencePreprocessingNeeded.isEmpty()) {
                // Write the batch to a fasta file
                Path originalSeqFastaPath = workDir.resolve("original.fasta");
                System.out.println("[" + id + "] Write fasta to disk..");
                Files.writeString(originalSeqFastaPath, formatSeqAsFasta(sequencePreprocessingNeeded));

                // Run Nextalign
                System.out.println("[" + id + "] Run Nextalign..");
                Map<String, NextalignResultEntry> nextalignResults = runNextalign(batch, originalSeqFastaPath);
                ReferenceGenomeData refGenome = ReferenceGenomeData.getInstance();
                for (GisaidEntry entry : batch.getEntries()) {
                    NextalignResultEntry nre = nextalignResults.get(entry.getGisaidEpiIsl());
                    if (nre != null) {
                        // Add the Nextalign results to the entry
                        entry
                            .setSeqAligned(nre.alignedNucSeq)
                            .setGeneAASeqs(formatGeneAASeqs(nre.geneAASeqs));

                        // Extract the amino acid and nucleotide mutations
                        if (entry.getSeqAligned() != null) {
                            List<MutationNuc> nucMutations = MutationFinder.findNucMutations(nre.alignedNucSeq);
                            String nucSubstitutions = nucMutations.stream()
                                .filter(m -> !m.getMutation().equals("-"))
                                .map(m -> String.valueOf(refGenome.getNucleotideBase(m.getPosition())) + m.getPosition()
                                    + m.getMutation())
                                .collect(Collectors.joining(","));
                            String nucDeletions = nucMutations.stream()
                                .filter(m -> m.getMutation().equals("-"))
                                .map(m -> String.valueOf(refGenome.getNucleotideBase(m.getPosition())) + m.getPosition()
                                    + m.getMutation())
                                .collect(Collectors.joining(","));
                            List<MutationAA> tmp = new ArrayList<>();
                            for (GeneAASeq geneAASeq : nre.geneAASeqs) {
                                tmp.addAll(MutationFinder.findAAMutations(
                                    geneAASeq.gene, geneAASeq.seq
                                ));
                            }
                            String aaMutations = tmp.stream()
                                .map(m -> m.getGene() + ":" + refGenome.getGeneAABase(m.getGene(), m.getPosition())
                                    + m.getPosition() + m.getMutation())
                                .collect(Collectors.joining(","));
                            entry
                                .setNucSubstitutions(nucSubstitutions)
                                .setNucDeletions(nucDeletions)
                                .setAaMutations(aaMutations);
                        } else {
                            entry
                                .setNucSubstitutions("")
                                .setNucDeletions("")
                                .setAaMutations("");
                        }

                        // TODO Use the insertions
                    }
                }
            }

            // Write the data into the database
            System.out.println("[" + id + "] Write to database..");
            writeToDatabase(batch);

            // Create the batch report
            int addedEntries = 0;
            int updatedTotalEntries = 0;
            int updatedMetadataEntries = 0;
            int updatedSequenceEntries = 0;
            for (GisaidEntry entry : batch.getEntries()) {
                if (entry.getImportMode() == ImportMode.APPEND) {
                    addedEntries++;
                } else if (entry.getImportMode() == ImportMode.UPDATE) {
                    updatedTotalEntries++;
                    if (entry.isMetadataChanged()) {
                        updatedMetadataEntries++;
                    }
                    if (entry.isSequenceChanged()) {
                        updatedSequenceEntries++;
                    }
                }
            }
            System.out.println("[" + id + "] Everything successful with no failed sequences");
            return new BatchReport()
                .setAddedEntries(addedEntries)
                .setUpdatedTotalEntries(updatedTotalEntries)
                .setUpdatedMetadataEntries(updatedMetadataEntries)
                .setUpdatedSequenceEntries(updatedSequenceEntries);
        } finally {
            // Clean up the work directory
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
            System.out.println("[" + id + "] Done!");
        }
    }

    /**
     * Fetch the data of the sequences from the database and compare them with the downloaded data. If an entry is
     * already in the database and has not changed, remove it from the batch. If an entry is already in the database and
     * has changed, mark the entry as an update candidate. The method also checks whether the metadata or the sequence
     * was changed.
     */
    private void determineChangeSet(Batch batch) throws SQLException {
        Map<String, GisaidEntry> sequenceMap = new HashMap<>();
        for (GisaidEntry entry : batch.getEntries()) {
            entry.setImportMode(ImportMode.APPEND);
            sequenceMap.put(entry.getGisaidEpiIsl(), entry);
        }
        String fetchSql = """
                select
                  gisaid_epi_isl,
                  strain,
                  date,
                  date_original,
                  date_submitted,
                  region,
                  country,
                  division,
                  location,
                  host,
                  age,
                  sex,
                  sampling_strategy,
                  pango_lineage,
                  gisaid_clade,
                  originating_lab,
                  submitting_lab,
                  authors,
                  seq_original_compressed
                from y_gisaid
                where gisaid_epi_isl = any(?);
            """;
        try (Connection conn = databasePool.getConnection()) {
            try (PreparedStatement statement = conn.prepareStatement(fetchSql)) {
                Object[] gisaidIds = batch.getEntries().stream().map(GisaidEntry::getGisaidEpiIsl).toArray();
                statement.setArray(1, conn.createArrayOf("text", gisaidIds));
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        GisaidEntry entry = sequenceMap.get(rs.getString("gisaid_epi_isl"));
                        entry.setImportMode(ImportMode.UPDATE);
                        // If updateSubmitterInformation is true, we need to fetch these data.
                        if (updateSubmitterInformation) {
                            Optional<SubmitterInformation> submitterInformationOpt
                                = submitterInformationFetcher.fetchSubmitterInformation(entry.getGisaidEpiIsl());
                            submitterInformationOpt.ifPresent(entry::setSubmitterInformation);
                        }
                        entry.setMetadataChanged(true);
                        entry.setSequenceChanged(true);
                        if (Objects.equals(entry.getStrain(), rs.getString("strain"))
                            && Objects.equals(entry.getDate(),
                            rs.getDate("date") != null ? rs.getDate("date").toLocalDate() : null)
                            && Objects.equals(entry.getDateOriginal(), rs.getString("date_original"))
                            && Objects.equals(entry.getDateSubmitted(),
                            rs.getDate("date_submitted") != null ? rs.getDate("date_submitted").toLocalDate() : null)
                            && Objects.equals(entry.getRegion(), rs.getString("region"))
                            && Objects.equals(entry.getCountry(), rs.getString("country"))
                            && Objects.equals(entry.getDivision(), rs.getString("division"))
                            && Objects.equals(entry.getLocation(), rs.getString("location"))
                            && Objects.equals(entry.getHost(), rs.getString("host"))
                            && Objects.equals(entry.getAge(), rs.getObject("age"))
                            && Objects.equals(entry.getSex(), rs.getString("sex"))
                            && Objects.equals(entry.getSamplingStrategy(), rs.getString("sampling_strategy"))
                            && Objects.equals(entry.getPangoLineage(), rs.getString("pango_lineage"))
                            && Objects.equals(entry.getGisaidClade(), rs.getString("gisaid_clade"))
                            // Compare submitter information if it has been fetched
                            && (entry.getSubmitterInformation() == null || (
                            Objects.equals(entry.getSubmitterInformation().getOriginatingLab(),
                                rs.getString("originating_lab"))
                                && Objects.equals(entry.getSubmitterInformation().getSubmittingLab(),
                                rs.getString("submitting_lab"))
                                && Objects.equals(entry.getSubmitterInformation().getAuthors(), rs.getString("authors"))
                        ))
                        ) {
                            entry.setMetadataChanged(false);
                        }
                        byte[] seqOriginalCompressed = rs.getBytes("seq_original_compressed");
                        if (Objects.equals(entry.getSeqOriginal(), seqOriginalCompressed != null ?
                            seqCompressor.decompress(seqOriginalCompressed) : null)) {
                            entry.setSequenceChanged(false);
                        }
                        if (!entry.isMetadataChanged() && !entry.isSequenceChanged()) {
                            batch.getEntries().remove(entry);
                        }
                    }
                }
            }
        }
    }

    private String formatSeqAsFasta(List<GisaidEntry> sequences) {
        StringBuilder fasta = new StringBuilder();
        for (GisaidEntry sequence : sequences) {
            fasta
                .append(">")
                .append(sequence.getGisaidEpiIsl())
                .append("\n")
                .append(sequence.getSeqOriginal())
                .append("\n\n");
        }
        return fasta.toString();
    }

    private Map<String, NextalignResultEntry> runNextalign(
        Batch batch,
        Path originalSeqFastaPath
    ) {
        Path outputPath = workDir.resolve("output");
        ExternalProcessHelper.execNextalign(outputPath, nextalignPath, originalSeqFastaPath, referenceFasta,
            geneMapGff);

        // Read the aligned nucleotide sequence
        Map<String, String> nucSeqs = new HashMap<>();
        FastaFileReader nucFastaReader = new FastaFileReader(outputPath.resolve("nextalign.aligned.fasta"), false);
        for (FastaEntry fastaEntry : nucFastaReader) {
            nucSeqs.put(fastaEntry.getSampleName(), fastaEntry.getSeq());
        }

        // Read the amino acid sequences
        List<String> genes = ReferenceGenomeData.getInstance().getGeneNames();
        Map<String, List<GeneAASeq>> geneAASeqs = new HashMap<>();
        for (GisaidEntry entry : batch.getEntries()) {
            geneAASeqs.put(entry.getGisaidEpiIsl(), new ArrayList<>());
        }
        for (String gene : genes) {
            FastaFileReader geneFastaReader = new FastaFileReader(
                outputPath.resolve("nextalign.gene." + gene + ".fasta"),
                false);
            for (FastaEntry fastaEntry : geneFastaReader) {
                geneAASeqs.get(fastaEntry.getSampleName()).add(
                    new GeneAASeq(gene, fastaEntry.getSeq())
                );
            }
        }

        Map<String, NextalignResultEntry> result = new HashMap<>();
        nucSeqs.forEach((sampleName, sequence) -> {
            result.put(sampleName, new NextalignResultEntry(sequence, geneAASeqs.get(sampleName)));
        });
        return result;
    }

    /**
     * Format to the format: gene1:seq,gene2:seq,... where the genes are in alphabetical order. The dictionary for
     * compression has the same format.
     */
    private String formatGeneAASeqs(List<GeneAASeq> geneAASeqs) {
        return geneAASeqs.stream()
            .sorted((s1, s2) -> s1.seq.compareTo(s2.gene))
            .map(s -> s.gene + ":" + s.seq)
            .collect(Collectors.joining(","));
    }

    private void writeToDatabase(Batch batch) throws SQLException {
        // If APPEND mode: Insert everything
        // If UPDATE mode + only metadata changed: update metadata and "updated_at" timestamp
        // If UPDATE mode + sequence changed: delete the old entry and re-insert
        List<GisaidEntry> toUpdateMetadata = new ArrayList<>();
        List<GisaidEntry> toDelete = new ArrayList<>();
        List<GisaidEntry> toInsert = new ArrayList<>();
        for (GisaidEntry sequence : batch.getEntries()) {
            if (sequence.getImportMode() == ImportMode.APPEND) {
                toInsert.add(sequence);
            } else if (sequence.getImportMode() == ImportMode.UPDATE) {
                if (!sequence.isSequenceChanged()) {
                    toUpdateMetadata.add(sequence);
                } else {
                    toDelete.add(sequence);
                    toInsert.add(sequence);
                }
            }
        }
        try (Connection conn = databasePool.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Update the metadata
            String updateSequenceSql = """
                    update y_gisaid
                    set
                      updated_at = now(),
                      strain = ?,
                      date = ?,
                      date_original = ?,
                      date_submitted = ?,
                      region = ?,
                      country = ?,
                      division = ?,
                      location = ?,
                      region_exposure = null,
                      country_exposure = null,
                      division_exposure = null,
                      host = ?,
                      age = ?,
                      sex = ?,
                      sampling_strategy = ?,
                      pango_lineage = ?,
                      gisaid_clade = ?,
                      originating_lab = coalesce(?, originating_lab),
                      submitting_lab = coalesce(?, submitting_lab),
                      authors = coalesce(?, authors)
                    where gisaid_epi_isl = ?;
                """;
            try (PreparedStatement statement = conn.prepareStatement(updateSequenceSql)) {
                for (GisaidEntry entry : toUpdateMetadata) {
                    SubmitterInformation si = entry.getSubmitterInformation();
                    statement.setString(1, entry.getStrain());
                    statement.setDate(2, entry.getDate() != null ?
                        Date.valueOf(entry.getDate()) : null);
                    statement.setString(3, entry.getDateOriginal());
                    statement.setDate(4, entry.getDateSubmitted() != null ?
                        Date.valueOf(entry.getDateSubmitted()) : null);
                    statement.setString(5, entry.getRegion());
                    statement.setString(6, entry.getCountry());
                    statement.setString(7, entry.getDivision());
                    statement.setString(8, entry.getLocation());
                    statement.setString(9, entry.getHost());
                    statement.setObject(10, entry.getAge());
                    statement.setString(11, entry.getSex());
                    statement.setString(12, entry.getSamplingStrategy());
                    statement.setString(13, entry.getPangoLineage());
                    statement.setString(14, entry.getGisaidClade());
                    statement.setString(15, si != null ? si.getOriginatingLab() : null);
                    statement.setString(16, si != null ? si.getSubmittingLab() : null);
                    statement.setString(17, si != null ? si.getAuthors() : null);
                    statement.setString(18, entry.getGisaidEpiIsl());
                    statement.addBatch();
                }
                statement.executeBatch();
                statement.clearBatch();
            }

            // 2. Delete sequences
            String deleteSequenceSql = """
                    delete from y_gisaid
                    where gisaid_epi_isl = ?;
                """;
            try (PreparedStatement statement = conn.prepareStatement(deleteSequenceSql)) {
                for (GisaidEntry entry : toDelete) {
                    statement.setString(1, entry.getGisaidEpiIsl());
                    statement.addBatch();
                }
                statement.executeBatch();
                statement.clearBatch();
            }

            // 3. Insert into gisaid_api_sequence
            String insertSequenceSql = """
                    insert into y_gisaid (
                      updated_at,
                      gisaid_epi_isl, strain, date, date_original, date_submitted, region, country, division, location,
                      region_exposure, country_exposure, division_exposure, host, age, sex, sampling_strategy,
                      pango_lineage, gisaid_clade, originating_lab, submitting_lab, authors,
                      seq_original_compressed, seq_aligned_compressed, aa_seqs, aa_mutations,
                      nuc_substitutions, nuc_deletions, nuc_insertions
                    )
                    values (
                      now(),
                      ?, ?, ?, ?, ?, ?, ?,
                      ?, ?, ?, ?, ?, ?, ?,
                      ?, ?, ?, ?, ?, ?, ?,
                      ?, ?, ?, ?, ?, ?, ?
                    );
                """;
            try (PreparedStatement insertStatement = conn.prepareStatement(insertSequenceSql)) {
                for (GisaidEntry entry : toInsert) {
                    SubmitterInformation si = entry.getSubmitterInformation();
                    insertStatement.setString(1, entry.getGisaidEpiIsl());
                    insertStatement.setString(2, entry.getStrain());
                    insertStatement.setDate(3, entry.getDate() != null ?
                        Date.valueOf(entry.getDate()) : null);
                    insertStatement.setString(4, entry.getDateOriginal());
                    insertStatement.setDate(5, entry.getDateSubmitted() != null ?
                        Date.valueOf(entry.getDateSubmitted()) : null);
                    insertStatement.setString(6, entry.getRegion());
                    insertStatement.setString(7, entry.getCountry());
                    insertStatement.setString(8, entry.getDivision());
                    insertStatement.setString(9, entry.getLocation());
                    insertStatement.setString(10, null);
                    insertStatement.setString(11, null);
                    insertStatement.setString(12, null);
                    insertStatement.setString(13, entry.getHost());
                    insertStatement.setObject(14, entry.getAge());
                    insertStatement.setString(15, entry.getSex());
                    insertStatement.setString(16, entry.getSamplingStrategy());
                    insertStatement.setString(17, entry.getPangoLineage());
                    insertStatement.setString(18, entry.getGisaidClade());
                    insertStatement.setString(19, si != null ? si.getOriginatingLab() : null);
                    insertStatement.setString(20, si != null ? si.getSubmittingLab() : null);
                    insertStatement.setString(21, si != null ? si.getAuthors() : null);
                    insertStatement.setBytes(22, entry.getSeqOriginal() != null ?
                        seqCompressor.compress(entry.getSeqOriginal()) : null);
                    insertStatement.setBytes(23, entry.getSeqAligned() != null ?
                        seqCompressor.compress(entry.getSeqAligned()) : null);
                    insertStatement.setString(24, entry.getGeneAASeqs());
                    insertStatement.setString(25, entry.getAaMutations());
                    insertStatement.setString(26, entry.getNucSubstitutions());
                    insertStatement.setString(27, entry.getNucDeletions());
                    insertStatement.setString(28, entry.getNucInsertions());
                    insertStatement.addBatch();
                }
                insertStatement.executeBatch();
                insertStatement.clearBatch();
            }

            // 4. Commit
            conn.commit();
            conn.setAutoCommit(true);
        }
    }

    private static class GeneAASeq {

        private final String gene;
        private final String seq;

        public GeneAASeq(String gene, String seq) {
            this.gene = gene;
            this.seq = seq;
        }
    }

    private static class NextalignResultEntry {

        private final String alignedNucSeq;
        private final List<GeneAASeq> geneAASeqs;

        public NextalignResultEntry(String alignedNucSeq, List<GeneAASeq> geneAASeqs) {
            this.alignedNucSeq = alignedNucSeq;
            this.geneAASeqs = geneAASeqs;
        }
    }
}
