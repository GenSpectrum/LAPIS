package ch.ethz.lapis.source.gisaid;

import ch.ethz.lapis.source.MutationAA;
import ch.ethz.lapis.source.MutationFinder;
import ch.ethz.lapis.source.MutationNuc;
import ch.ethz.lapis.util.*;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class BatchProcessingWorker {

    private final int id;
    private final Path workDir;
    private final ComboPooledDataSource databasePool;
    private final boolean updateSubmitterInformation;
    private final SubmitterInformationFetcher submitterInformationFetcher = new SubmitterInformationFetcher();
    private final Path nextcladePath;
    private final SeqCompressor nucSeqCompressor;
    private final SeqCompressor aaSeqCompressor;
    private final Map<String, GisaidHashes> oldHashes;

    /**
     * @param id             An unique identifier for the worker
     * @param workDir        An empty work directory for the worker
     */
    public BatchProcessingWorker(
        int id,
        Path workDir,
        ComboPooledDataSource databasePool,
        boolean updateSubmitterInformation,
        Path nextcladePath,
        SeqCompressor nucSeqCompressor,
        SeqCompressor aaSeqCompressor,
        Map<String, GisaidHashes> oldHashes
    ) {
        this.databasePool = databasePool;
        this.id = id;
        this.workDir = workDir;
        this.updateSubmitterInformation = updateSubmitterInformation;
        this.nextcladePath = nextcladePath;
        this.nucSeqCompressor = nucSeqCompressor;
        this.aaSeqCompressor = aaSeqCompressor;
        this.oldHashes = oldHashes;
    }

    public BatchReport run(Batch batch) throws Exception {
        try {
            int batchSize = batch.getEntries().size();
            System.out.println(LocalDateTime.now() + " [" + id + "] Received a batch");

            // Remove entries from the batch where no sequence is provided -> very weird
            batch = new Batch(batch.getEntries().stream()
                .filter(s -> s.getSeqOriginal() != null && !s.getSeqOriginal().isBlank())
                .collect(Collectors.toList()));

            determineChangeSet(batch);

            // Fetch the submitter information for all APPEND sequences
            for (GisaidEntry entry : batch.getEntries()) {
                // Due to rate limitation, we only fetch submitter information for Swiss sequences for now
                if (entry.getImportMode() == ImportMode.APPEND && "Switzerland".equals(entry.getCountry())) {
                    Thread.sleep(3000);
                    var result = submitterInformationFetcher.fetchSubmitterInformation(entry.getGisaidEpiIsl());
                    if (result.getStatus() == SubmitterInformationFetcher.SubmitterInformationFetchingStatus.SUCCESSFUL) {
                        entry.setSubmitterInformation(result.getValue());
                    }
                }
            }

            // Determine the entries that are new and have a changed sequence. Those sequences need to be processed
            // Nextclade.
            List<GisaidEntry> sequencePreprocessingNeeded = batch.getEntries().stream()
                .filter(s -> s.getImportMode() == ImportMode.APPEND || s.isSequenceChanged())
                .collect(Collectors.toList());

            // Exclude sequences that contain invalid characters
            Pattern r = Pattern.compile("[\\sACGTUMRWSYKVHDBNX-]*", Pattern.CASE_INSENSITIVE);
            List<GisaidEntry> validSequences = new ArrayList<>();
            List<GisaidEntry> invalidSequences = new ArrayList<>();
            for (GisaidEntry entry : sequencePreprocessingNeeded) {
                if (r.matcher(entry.getSeqOriginal()).matches()) {
                    validSequences.add(entry);
                } else {
                    invalidSequences.add(entry);
                }
            }
            if (!invalidSequences.isEmpty()) {
                System.out.println(
                    LocalDateTime.now() + " [" + id + "] Found sequences that contain invalid characters: "
                        + invalidSequences.stream().map(GisaidEntry::getGisaidEpiIsl)
                        .collect(Collectors.joining(",")));
            }

            System.out.println(LocalDateTime.now() + " [" + id + "] " + validSequences.size() + " out of " + batchSize +
                " sequences are new or have changed sequence.");
            if (!validSequences.isEmpty()) {
                // Write the batch to a fasta file
                Path originalSeqFastaPath = workDir.resolve("original.fasta");
                System.out.println(LocalDateTime.now() + " [" + id + "] Write fasta to disk..");
                Files.writeString(originalSeqFastaPath, formatSeqAsFasta(validSequences));

                // Run Nextclade
                System.out.println(LocalDateTime.now() + " [" + id + "] Run Nextclade..");
                Map<String, NextcladeResultEntry> nextcladeResults = runNextclade(batch, originalSeqFastaPath);
                ReferenceGenomeData refGenome = ReferenceGenomeData.getInstance();
                for (GisaidEntry entry : batch.getEntries()) {
                    NextcladeResultEntry nre = nextcladeResults.get(entry.getGisaidEpiIsl());
                    if (nre != null) {
                        // Add the Nextclade results to the entry
                        entry
                            .setSeqAligned(nre.alignedNucSeq)
                            .setGeneAASeqsCompressed(aaSeqCompressor.compress(formatGeneAASeqs(nre.geneAASeqs)))
                            .setNextcladeTsvEntry(nre.nextcladeTsvEntry);

                        // Extract the amino acid and nucleotide mutations
                        if (entry.getSeqAligned() != null) {
                            // Nuc
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
                            String nucUnknowns = String.join(",", MutationFinder.compressPositionsAsStrings(
                                MutationFinder.findNucUnknowns(nre.alignedNucSeq)));
                            // AA
                            List<MutationAA> tmp = new ArrayList<>();
                            List<String> aaUnknownsComponents = new ArrayList<>();
                            for (GeneAASeq geneAASeq : nre.geneAASeqs) {
                                tmp.addAll(MutationFinder.findAAMutations(
                                    geneAASeq.gene, geneAASeq.seq
                                ));
                                List<String> thisAAUnknowns = MutationFinder.compressPositionsAsStrings(
                                    MutationFinder.findAAUnknowns(geneAASeq.seq));

                                aaUnknownsComponents.addAll(thisAAUnknowns.stream()
                                    .map(u -> geneAASeq.gene + ":" + u)
                                    .collect(Collectors.toList()));
                            }
                            String aaMutations = tmp.stream()
                                .map(m -> m.getGene() + ":" + refGenome.getGeneAABase(m.getGene(), m.getPosition())
                                    + m.getPosition() + m.getMutation())
                                .collect(Collectors.joining(","));
                            String aaUnknowns = String.join(",", aaUnknownsComponents);
                            // Setting the values
                            entry
                                .setNucSubstitutions(nucSubstitutions)
                                .setNucDeletions(nucDeletions)
                                .setNucUnknowns(nucUnknowns)
                                .setAaMutations(aaMutations)
                                .setAaUnknowns(aaUnknowns);
                        } else {
                            entry
                                .setNucSubstitutions("")
                                .setNucDeletions("")
                                .setNucUnknowns("")
                                .setAaMutations("")
                                .setAaUnknowns("");
                        }

                        // TODO Use the insertions
                    }
                }
            }

            // Write the data into the database
            System.out.println(LocalDateTime.now() + " [" + id + "] Write to database..");
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
            System.out.println(LocalDateTime.now() + " [" + id + "] Everything successful with no failed sequences");
            return new BatchReport()
                .setAddedEntries(addedEntries)
                .setUpdatedTotalEntries(updatedTotalEntries)
                .setUpdatedMetadataEntries(updatedMetadataEntries)
                .setUpdatedSequenceEntries(updatedSequenceEntries);
        } finally {
            // Clean up the work directory
            System.out.println(LocalDateTime.now() + " [" + id + "] Clean up");
            try (DirectoryStream<Path> directory = Files.newDirectoryStream(workDir)) {
                for (Path path : directory) {
                    if (Files.isDirectory(path)) {
                        FileUtils.deleteDirectory(path.toFile());
                    } else {
                        Files.delete(path);
                    }
                }
            }
            System.out.println(LocalDateTime.now() + " [" + id + "] Done!");
        }
    }

    /**
     * Use the oldHashes to determine changes in the downloaded data.  If an entry is already in the database and has
     * not changed, remove it from the batch. If an entry is already in the database and has changed, mark the entry as
     * an update candidate. The method also checks whether the metadata or the sequence was changed.
     */
    private void determineChangeSet(Batch batch) {
        List<GisaidEntry> toRemove = new ArrayList<>();
        for (GisaidEntry entry : batch.getEntries()) {
            String sampleName = entry.getGisaidEpiIsl();
            List<Object> metadataListForHashing = new ArrayList<>();
            metadataListForHashing.add(entry.getStrain());
            metadataListForHashing.add(entry.getDate());
            metadataListForHashing.add(entry.getDateOriginal());
            metadataListForHashing.add(entry.getDateSubmitted());
            metadataListForHashing.add(entry.getRegion());
            metadataListForHashing.add(entry.getCountry());
            metadataListForHashing.add(entry.getDivision());
            metadataListForHashing.add(entry.getLocation());
            metadataListForHashing.add(entry.getHost());
            metadataListForHashing.add(entry.getAge());
            metadataListForHashing.add(entry.getSex());
            metadataListForHashing.add(entry.getSamplingStrategy());
            metadataListForHashing.add(entry.getPangoLineage());
            metadataListForHashing.add(entry.getGisaidClade());
            String metadataHash = Utils.hashMd5(metadataListForHashing);
            String seqOriginalHash = Utils.hashMd5(entry.getSeqOriginal());
            entry.setMetadataHash(metadataHash);
            entry.setSeqOriginalHash(seqOriginalHash);
            if (!oldHashes.containsKey(sampleName)) {
                entry.setImportMode(ImportMode.APPEND);
                continue;
            }
            entry.setImportMode(ImportMode.UPDATE);
            String oldMetadataHash = oldHashes.get(sampleName).getMetadataHash();
            String oldSeqOriginalHash = oldHashes.get(sampleName).getSeqOriginalHash();
            entry.setMetadataChanged(!metadataHash.equals(oldMetadataHash));
            entry.setSequenceChanged(!seqOriginalHash.equals(oldSeqOriginalHash));
            if (!entry.isMetadataChanged() && !entry.isSequenceChanged()) {
                toRemove.add(entry);
            }
        }
        batch.getEntries().removeAll(toRemove);
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

    private Map<String, NextcladeResultEntry> runNextclade(
        Batch batch,
        Path originalSeqFastaPath
    ) throws IOException, InterruptedException {
        List<String> genes = ReferenceGenomeData.getInstance().getGeneNames();
        Path outputPath = workDir.resolve("output");
        String command = nextcladePath.toAbsolutePath() +
            " run" +
            " --input-dataset=/app/nextclade-data" + // TODO Move it to the configs
            " --output-all=" + outputPath.toAbsolutePath() +
            " --output-basename=nextclade" +
            " --silent" +
            " --jobs=1" +
            " " + originalSeqFastaPath.toAbsolutePath();

        Process process = Runtime.getRuntime().exec(command);
        boolean exited = process.waitFor(20, TimeUnit.MINUTES);
        if (!exited) {
            String sequenceIdsCsv = batch.getEntries().stream()
                .map(GisaidEntry::getGisaidEpiIsl).collect(Collectors.joining(","));
            process.destroyForcibly();
            throw new RuntimeException("Nextclade timed out (after 20 minutes); sequences: " + sequenceIdsCsv);
        }
        if (process.exitValue() != 0) {
            String sequenceIdsCsv = batch.getEntries().stream()
                .map(GisaidEntry::getGisaidEpiIsl).collect(Collectors.joining(","));
            throw new RuntimeException("Nextclade exited with code " + process.exitValue() + "; sequences:"
                + sequenceIdsCsv);
        }

        // Read nextclade.tsv (which contains QC data and more)
        NextcladeTsvFileReader nextcladeTsvReader = new NextcladeTsvFileReader(new BufferedInputStream(
            new FileInputStream(outputPath.resolve("nextclade.tsv").toFile())));
        Map<String, NextcladeTsvEntry> nextcladeTsvEntries = new HashMap<>();
        for (NextcladeTsvEntry nextcladeTsvEntry : nextcladeTsvReader) {
            nextcladeTsvEntries.put(nextcladeTsvEntry.getSeqName(), nextcladeTsvEntry);
        }

        // Read the aligned nucleotide sequence
        Map<String, String> nucSeqs = new HashMap<>();
        FastaFileReader nucFastaReader = new FastaFileReader(outputPath.resolve("nextclade.aligned.fasta"), false);
        for (FastaEntry fastaEntry : nucFastaReader) {
            nucSeqs.put(fastaEntry.getSampleName(), fastaEntry.getSeq());
        }

        // Read the amino acid sequences
        Map<String, List<GeneAASeq>> geneAASeqs = new HashMap<>();
        for (GisaidEntry entry : batch.getEntries()) {
            geneAASeqs.put(entry.getGisaidEpiIsl(), new ArrayList<>());
        }
        for (String gene : genes) {
            FastaFileReader geneFastaReader = new FastaFileReader(
                outputPath.resolve("nextclade_gene_" + gene + ".translation.fasta"),
                false);
            for (FastaEntry fastaEntry : geneFastaReader) {
                geneAASeqs.get(fastaEntry.getSampleName()).add(
                    new GeneAASeq(gene, fastaEntry.getSeq())
                );
            }
        }

        Map<String, NextcladeResultEntry> result = new HashMap<>();
        nextcladeTsvEntries.forEach((sampleName, nextcladeTsvEntry) -> {
            result.put(sampleName, new NextcladeResultEntry(nucSeqs.get(sampleName), geneAASeqs.get(sampleName),
                nextcladeTsvEntry));
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
        // If UPDATE mode + metadata changed: update metadata and "updated_at" timestamp
        // If UPDATE mode + sequence changed: update the sequences, the Nextclade results, and "updated_at" timestamp
        // TODO Currently, we do two updates if both metadata and sequences have changed.
        //  It would be more efficient to one query for both.
        List<GisaidEntry> toUpdateMetadata = new ArrayList<>();
        List<GisaidEntry> toUpdateSequence = new ArrayList<>();
        List<GisaidEntry> toInsert = new ArrayList<>();
        for (GisaidEntry sequence : batch.getEntries()) {
            if (sequence.getImportMode() == ImportMode.APPEND) {
                toInsert.add(sequence);
            } else if (sequence.getImportMode() == ImportMode.UPDATE) {
                if (sequence.isMetadataChanged()) {
                    toUpdateMetadata.add(sequence);
                }
                if (sequence.isSequenceChanged()) {
                    toUpdateSequence.add(sequence);
                }
            }
        }
        try (Connection conn = databasePool.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Update the metadata
            String updateMetadataSql = """
                    update y_gisaid
                    set
                      updated_at = now(),
                      strain = ?,
                      date = ?,
                      year = ?,
                      month = ?,
                      day = ?,
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
                      authors = coalesce(?, authors),
                      metadata_hash = ?
                    where gisaid_epi_isl = ?;
                """;
            try (PreparedStatement statement = conn.prepareStatement(updateMetadataSql)) {
                for (GisaidEntry entry : toUpdateMetadata) {
                    SubmitterInformation si = entry.getSubmitterInformation();
                    statement.setString(1, entry.getStrain());
                    statement.setDate(2, entry.getDate() != null ?
                        Date.valueOf(entry.getDate()) : null);
                    statement.setObject(3, entry.getYear());
                    statement.setObject(4, entry.getMonth());
                    statement.setObject(5, entry.getDay());
                    statement.setString(6, entry.getDateOriginal());
                    statement.setDate(7, entry.getDateSubmitted() != null ?
                        Date.valueOf(entry.getDateSubmitted()) : null);
                    statement.setString(8, entry.getRegion());
                    statement.setString(9, entry.getCountry());
                    statement.setString(10, entry.getDivision());
                    statement.setString(11, entry.getLocation());
                    statement.setString(12, entry.getHost());
                    statement.setObject(13, entry.getAge());
                    statement.setString(14, entry.getSex());
                    statement.setString(15, entry.getSamplingStrategy());
                    statement.setString(16, entry.getPangoLineage());
                    statement.setString(17, entry.getGisaidClade());
                    statement.setString(18, si != null ? si.getOriginatingLab() : null);
                    statement.setString(19, si != null ? si.getSubmittingLab() : null);
                    statement.setString(20, si != null ? si.getAuthors() : null);
                    statement.setString(21, entry.getMetadataHash());
                    statement.setString(22, entry.getGisaidEpiIsl());
                    statement.addBatch();
                }
                statement.executeBatch();
                statement.clearBatch();
            }

            // 2. Update sequences
            String updateSequenceSql = """
                    update y_gisaid
                    set
                      updated_at = now(),
                      seq_original_hash = ?,
                      seq_original_compressed = ?,
                      seq_aligned_compressed = ?,
                      aa_seqs_compressed = ?,
                      aa_mutations = ?,
                      aa_unknowns = ?,
                      nuc_substitutions = ?,
                      nuc_deletions = ?,
                      nuc_insertions = ?,
                      nuc_unknowns = ?,
                      nextclade_clade = ?,
                      nextclade_clade_long = ?,
                      nextclade_pango_lineage = ?,
                      nextclade_total_substitutions = ?,
                      nextclade_total_deletions = ?,
                      nextclade_total_insertions = ?,
                      nextclade_total_frame_shifts = ?,
                      nextclade_total_aminoacid_substitutions = ?,
                      nextclade_total_aminoacid_deletions = ?,
                      nextclade_total_aminoacid_insertions = ?,
                      nextclade_total_missing = ?,
                      nextclade_total_non_acgtns = ?,
                      nextclade_total_pcr_primer_changes = ?,
                      nextclade_pcr_primer_changes = ?,
                      nextclade_alignment_score = ?,
                      nextclade_alignment_start = ?,
                      nextclade_alignment_end = ?,
                      nextclade_qc_overall_score = ?,
                      nextclade_qc_overall_status = ?,
                      nextclade_qc_missing_data_missing_data_threshold = ?,
                      nextclade_qc_missing_data_score = ?,
                      nextclade_qc_missing_data_status = ?,
                      nextclade_qc_missing_data_total_missing = ?,
                      nextclade_qc_mixed_sites_mixed_sites_threshold = ?,
                      nextclade_qc_mixed_sites_score = ?,
                      nextclade_qc_mixed_sites_status = ?,
                      nextclade_qc_mixed_sites_total_mixed_sites = ?,
                      nextclade_qc_private_mutations_cutoff = ?,
                      nextclade_qc_private_mutations_excess = ?,
                      nextclade_qc_private_mutations_score = ?,
                      nextclade_qc_private_mutations_status = ?,
                      nextclade_qc_private_mutations_total = ?,
                      nextclade_qc_snp_clusters_clustered_snps = ?,
                      nextclade_qc_snp_clusters_score = ?,
                      nextclade_qc_snp_clusters_status = ?,
                      nextclade_qc_snp_clusters_total_snps = ?,
                      nextclade_qc_frame_shifts_frame_shifts = ?,
                      nextclade_qc_frame_shifts_total_frame_shifts = ?,
                      nextclade_qc_frame_shifts_frame_shifts_ignored = ?,
                      nextclade_qc_frame_shifts_total_frame_shifts_ignored = ?,
                      nextclade_qc_frame_shifts_score = ?,
                      nextclade_qc_frame_shifts_status = ?,
                      nextclade_qc_stop_codons_stop_codons = ?,
                      nextclade_qc_stop_codons_total_stop_codons = ?,
                      nextclade_qc_stop_codons_score = ?,
                      nextclade_qc_stop_codons_status = ?,
                      nextclade_coverage = ?,
                      nextclade_errors = ?,
                      aa_insertions = ?
                    where gisaid_epi_isl = ?;
                """;
            try (PreparedStatement statement = conn.prepareStatement(updateSequenceSql)) {
                for (GisaidEntry entry : toUpdateSequence) {
                    NextcladeTsvEntry nc = entry.getNextcladeTsvEntry();
                    statement.setString(1, entry.getSeqOriginalHash());
                    statement.setBytes(2, entry.getSeqOriginal() != null ?
                        nucSeqCompressor.compress(entry.getSeqOriginal()) : null);
                    statement.setBytes(3, entry.getSeqAligned() != null ?
                        nucSeqCompressor.compress(entry.getSeqAligned()) : null);
                    statement.setBytes(4, entry.getGeneAASeqsCompressed());
                    statement.setString(5, entry.getAaMutations());
                    statement.setString(6, entry.getAaUnknowns());
                    statement.setString(7, entry.getNucSubstitutions());
                    statement.setString(8, entry.getNucDeletions());
                    statement.setString(9, nc != null ? nc.getInsertions() : null);
                    statement.setString(10, entry.getNucUnknowns());
                    // Nextclade
                    statement.setString(11, nc != null ? nc.getClade() : null);
                    statement.setString(12, nc != null ? nc.getCladeLong() : null);
                    statement.setString(13, nc != null ? nc.getPangoLineage() : null);
                    statement.setObject(14, nc != null ? nc.getTotalSubstitutions() : null);
                    statement.setObject(15, nc != null ? nc.getTotalDeletions() : null);
                    statement.setObject(16, nc != null ? nc.getTotalInsertions() : null);
                    statement.setObject(17, nc != null ? nc.getTotalFrameShifts() : null);
                    statement.setObject(18, nc != null ? nc.getTotalAminoacidSubstitutions() : null);
                    statement.setObject(19, nc != null ? nc.getTotalAminoacidDeletions() : null);
                    statement.setObject(20, nc != null ? nc.getTotalAminoacidInsertions() : null);
                    statement.setObject(21, nc != null ? nc.getTotalMissing() : null);
                    statement.setObject(22, nc != null ? nc.getTotalNonACGTNs() : null);
                    statement.setObject(23, nc != null ? nc.getTotalPcrPrimerChanges() : null);
                    statement.setObject(24, nc != null ? nc.getPcrPrimerChanges() : null);
                    statement.setObject(25, nc != null ? nc.getAlignmentScore() : null);
                    statement.setObject(26, nc != null ? nc.getAlignmentStart() : null);
                    statement.setObject(27, nc != null ? nc.getAlignmentEnd() : null);
                    statement.setObject(28, nc != null ? nc.getQcOverallScore() : null);
                    statement.setString(29, nc != null ? nc.getQcOverallStatus() : null);
                    statement.setObject(30, nc != null ? nc.getQcMissingDataMissingDataThreshold() : null);
                    statement.setObject(31, nc != null ? nc.getQcMissingDataScore() : null);
                    statement.setString(32, nc != null ? nc.getQcMissingDataStatus() : null);
                    statement.setObject(33, nc != null ? nc.getQcMissingDataTotalMissing() : null);
                    statement.setObject(34, nc != null ? nc.getQcMixedSitesMixedSitesThreshold() : null);
                    statement.setObject(35, nc != null ? nc.getQcMixedSitesScore() : null);
                    statement.setString(36, nc != null ? nc.getQcMixedSitesStatus() : null);
                    statement.setObject(37, nc != null ? nc.getQcMixedSitesTotalMixedSites() : null);
                    statement.setObject(38, nc != null ? nc.getQcPrivateMutationsCutoff() : null);
                    statement.setObject(39, nc != null ? nc.getQcPrivateMutationsExcess() : null);
                    statement.setObject(40, nc != null ? nc.getQcPrivateMutationsScore() : null);
                    statement.setString(41, nc != null ? nc.getQcPrivateMutationsStatus() : null);
                    statement.setObject(42, nc != null ? nc.getQcPrivateMutationsTotal() : null);
                    statement.setString(43, nc != null ? nc.getQcSnpClustersClusteredSNPs() : null);
                    statement.setObject(44, nc != null ? nc.getQcSnpClustersScore() : null);
                    statement.setString(45, nc != null ? nc.getQcSnpClustersStatus() : null);
                    statement.setObject(46, nc != null ? nc.getQcSnpClustersTotalSNPs() : null);
                    statement.setString(47, nc != null ? nc.getQcFrameShiftsFrameShifts() : null);
                    statement.setObject(48, nc != null ? nc.getQcFrameShiftsTotalFrameShifts() : null);
                    statement.setString(49, nc != null ? nc.getQcFrameShiftsFrameShiftsIgnored() : null);
                    statement.setObject(50, nc != null ? nc.getQcFrameShiftsTotalFrameShiftsIgnored() : null);
                    statement.setObject(51, nc != null ? nc.getQcFrameShiftsScore() : null);
                    statement.setString(52, nc != null ? nc.getQcFrameShiftsStatus() : null);
                    statement.setString(53, nc != null ? nc.getQcStopCodonsStopCodons() : null);
                    statement.setObject(54, nc != null ? nc.getQcStopCodonsTotalStopCodons() : null);
                    statement.setObject(55, nc != null ? nc.getQcStopCodonsScore() : null);
                    statement.setString(56, nc != null ? nc.getQcStopCodonsStatus() : null);
                    statement.setObject(57, nc != null ? nc.getCoverage() : null);
                    statement.setString(58, nc != null ? nc.getErrors() : null);
                    statement.setString(59, nc != null ? nc.getAaInsertions() : null);
                    statement.setString(60, entry.getGisaidEpiIsl());
                    statement.addBatch();
                    statement.clearParameters();
                }
                statement.executeBatch();
                statement.clearBatch();
            }

            // 3. Insert into y_gisaid
            String insertSequenceSql = """
                    insert into y_gisaid (
                      updated_at,
                      gisaid_epi_isl, strain, date, year, month, day, date_original, date_submitted,
                      region, country, division, location,
                      region_exposure, country_exposure, division_exposure, host, age, sex, sampling_strategy,
                      pango_lineage, gisaid_clade, originating_lab, submitting_lab, authors,
                      seq_original_compressed, seq_aligned_compressed, aa_seqs_compressed, aa_mutations, aa_unknowns,
                      nuc_substitutions, nuc_deletions, nuc_insertions, nuc_unknowns,
                      metadata_hash, seq_original_hash,
                      nextclade_clade, nextclade_clade_long, nextclade_pango_lineage, nextclade_total_substitutions, nextclade_total_deletions,
                      nextclade_total_insertions, nextclade_total_frame_shifts, nextclade_total_aminoacid_substitutions,
                      nextclade_total_aminoacid_deletions, nextclade_total_aminoacid_insertions, nextclade_total_missing,
                      nextclade_total_non_acgtns, nextclade_total_pcr_primer_changes, nextclade_pcr_primer_changes,
                      nextclade_alignment_score, nextclade_alignment_start, nextclade_alignment_end,
                      nextclade_qc_overall_score, nextclade_qc_overall_status, nextclade_qc_missing_data_missing_data_threshold,
                      nextclade_qc_missing_data_score, nextclade_qc_missing_data_status, nextclade_qc_missing_data_total_missing,
                      nextclade_qc_mixed_sites_mixed_sites_threshold, nextclade_qc_mixed_sites_score, nextclade_qc_mixed_sites_status,
                      nextclade_qc_mixed_sites_total_mixed_sites, nextclade_qc_private_mutations_cutoff, nextclade_qc_private_mutations_excess,
                      nextclade_qc_private_mutations_score, nextclade_qc_private_mutations_status, nextclade_qc_private_mutations_total,
                      nextclade_qc_snp_clusters_clustered_snps, nextclade_qc_snp_clusters_score, nextclade_qc_snp_clusters_status,
                      nextclade_qc_snp_clusters_total_snps, nextclade_qc_frame_shifts_frame_shifts, nextclade_qc_frame_shifts_total_frame_shifts,
                      nextclade_qc_frame_shifts_frame_shifts_ignored, nextclade_qc_frame_shifts_total_frame_shifts_ignored,
                      nextclade_qc_frame_shifts_score, nextclade_qc_frame_shifts_status, nextclade_qc_stop_codons_stop_codons,
                      nextclade_qc_stop_codons_total_stop_codons, nextclade_qc_stop_codons_score, nextclade_qc_stop_codons_status,
                      nextclade_coverage, nextclade_errors, aa_insertions
                    )
                    values (
                      now(),
                      ?, ?, ?, ?, ?, ?, ?,
                      ?, ?, ?, ?, ?, ?, ?,
                      ?, ?, ?, ?, ?, ?, ?,
                      ?, ?, ?, ?, ?, ?, ?,
                      ?, ?, ?, ?, ?, ?, ?,
                      ?, ?, ?, ?, ?, ?, ?, ?,
                      ?, ?, ?, ?, ?, ?, ?,
                      ?, ?, ?, ?, ?, ?, ?,
                      ?, ?, ?, ?, ?, ?, ?,
                      ?, ?, ?, ?, ?, ?, ?,
                      ?, ?, ?, ?, ?, ?, ?,
                      ?, ?, ?, ?, ?, ?
                    );
                """;
            try (PreparedStatement insertStatement = conn.prepareStatement(insertSequenceSql)) {
                for (GisaidEntry entry : toInsert) {
                    NextcladeTsvEntry nc = entry.getNextcladeTsvEntry();
                    SubmitterInformation si = entry.getSubmitterInformation();
                    insertStatement.setString(1, entry.getGisaidEpiIsl());
                    insertStatement.setString(2, entry.getStrain());
                    insertStatement.setDate(3, entry.getDate() != null ?
                        Date.valueOf(entry.getDate()) : null);
                    insertStatement.setObject(4, entry.getYear());
                    insertStatement.setObject(5, entry.getMonth());
                    insertStatement.setObject(6, entry.getDay());
                    insertStatement.setString(7, entry.getDateOriginal());
                    insertStatement.setDate(8, entry.getDateSubmitted() != null ?
                        Date.valueOf(entry.getDateSubmitted()) : null);
                    insertStatement.setString(9, entry.getRegion());
                    insertStatement.setString(10, entry.getCountry());
                    insertStatement.setString(11, entry.getDivision());
                    insertStatement.setString(12, entry.getLocation());
                    insertStatement.setString(13, null);
                    insertStatement.setString(14, null);
                    insertStatement.setString(15, null);
                    insertStatement.setString(16, entry.getHost());
                    insertStatement.setObject(17, entry.getAge());
                    insertStatement.setString(18, entry.getSex());
                    insertStatement.setString(19, entry.getSamplingStrategy());
                    insertStatement.setString(20, entry.getPangoLineage());
                    insertStatement.setString(21, entry.getGisaidClade());
                    insertStatement.setString(22, si != null ? si.getOriginatingLab() : null);
                    insertStatement.setString(23, si != null ? si.getSubmittingLab() : null);
                    insertStatement.setString(24, si != null ? si.getAuthors() : null);
                    insertStatement.setBytes(25, entry.getSeqOriginal() != null ?
                        nucSeqCompressor.compress(entry.getSeqOriginal()) : null);
                    insertStatement.setBytes(26, entry.getSeqAligned() != null ?
                        nucSeqCompressor.compress(entry.getSeqAligned()) : null);
                    insertStatement.setBytes(27, entry.getGeneAASeqsCompressed());
                    insertStatement.setString(28, entry.getAaMutations());
                    insertStatement.setString(29, entry.getAaUnknowns());
                    insertStatement.setString(30, entry.getNucSubstitutions());
                    insertStatement.setString(31, entry.getNucDeletions());
                    insertStatement.setString(32, nc != null ? nc.getInsertions() : null);
                    insertStatement.setString(33, entry.getNucUnknowns());
                    insertStatement.setString(34, entry.getMetadataHash());
                    insertStatement.setString(35, entry.getSeqOriginalHash());
                    // Nextclade
                    insertStatement.setString(36, nc != null ? nc.getClade() : null);
                    insertStatement.setString(37, nc != null ? nc.getCladeLong() : null);
                    insertStatement.setString(38, nc != null ? nc.getPangoLineage() : null);
                    insertStatement.setObject(39, nc != null ? nc.getTotalSubstitutions() : null);
                    insertStatement.setObject(40, nc != null ? nc.getTotalDeletions() : null);
                    insertStatement.setObject(41, nc != null ? nc.getTotalInsertions() : null);
                    insertStatement.setObject(42, nc != null ? nc.getTotalFrameShifts() : null);
                    insertStatement.setObject(43, nc != null ? nc.getTotalAminoacidSubstitutions() : null);
                    insertStatement.setObject(44, nc != null ? nc.getTotalAminoacidDeletions() : null);
                    insertStatement.setObject(45, nc != null ? nc.getTotalAminoacidInsertions() : null);
                    insertStatement.setObject(46, nc != null ? nc.getTotalMissing() : null);
                    insertStatement.setObject(47, nc != null ? nc.getTotalNonACGTNs() : null);
                    insertStatement.setObject(48, nc != null ? nc.getTotalPcrPrimerChanges() : null);
                    insertStatement.setString(49, nc != null ? nc.getPcrPrimerChanges() : null);
                    insertStatement.setObject(50, nc != null ? nc.getAlignmentScore() : null);
                    insertStatement.setObject(51, nc != null ? nc.getAlignmentStart() : null);
                    insertStatement.setObject(52, nc != null ? nc.getAlignmentEnd() : null);
                    insertStatement.setObject(53, nc != null ? nc.getQcOverallScore() : null);
                    insertStatement.setString(54, nc != null ? nc.getQcOverallStatus() : null);
                    insertStatement.setObject(55, nc != null ? nc.getQcMissingDataMissingDataThreshold() : null);
                    insertStatement.setObject(56, nc != null ? nc.getQcMissingDataScore() : null);
                    insertStatement.setString(57, nc != null ? nc.getQcMissingDataStatus() : null);
                    insertStatement.setObject(58, nc != null ? nc.getQcMissingDataTotalMissing() : null);
                    insertStatement.setObject(59, nc != null ? nc.getQcMixedSitesMixedSitesThreshold() : null);
                    insertStatement.setObject(60, nc != null ? nc.getQcMixedSitesScore() : null);
                    insertStatement.setString(61, nc != null ? nc.getQcMixedSitesStatus() : null);
                    insertStatement.setObject(62, nc != null ? nc.getQcMixedSitesTotalMixedSites() : null);
                    insertStatement.setObject(63, nc != null ? nc.getQcPrivateMutationsCutoff() : null);
                    insertStatement.setObject(64, nc != null ? nc.getQcPrivateMutationsExcess() : null);
                    insertStatement.setObject(65, nc != null ? nc.getQcPrivateMutationsScore() : null);
                    insertStatement.setString(66, nc != null ? nc.getQcPrivateMutationsStatus() : null);
                    insertStatement.setObject(67, nc != null ? nc.getQcPrivateMutationsTotal() : null);
                    insertStatement.setString(68, nc != null ? nc.getQcSnpClustersClusteredSNPs() : null);
                    insertStatement.setObject(69, nc != null ? nc.getQcSnpClustersScore() : null);
                    insertStatement.setString(70, nc != null ? nc.getQcSnpClustersStatus() : null);
                    insertStatement.setObject(71, nc != null ? nc.getQcSnpClustersTotalSNPs() : null);
                    insertStatement.setString(72, nc != null ? nc.getQcFrameShiftsFrameShifts() : null);
                    insertStatement.setObject(73, nc != null ? nc.getQcFrameShiftsTotalFrameShifts() : null);
                    insertStatement.setString(74, nc != null ? nc.getQcFrameShiftsFrameShiftsIgnored() : null);
                    insertStatement.setObject(75, nc != null ? nc.getQcFrameShiftsTotalFrameShiftsIgnored() : null);
                    insertStatement.setObject(76, nc != null ? nc.getQcFrameShiftsScore() : null);
                    insertStatement.setString(77, nc != null ? nc.getQcFrameShiftsStatus() : null);
                    insertStatement.setString(78, nc != null ? nc.getQcStopCodonsStopCodons() : null);
                    insertStatement.setObject(79, nc != null ? nc.getQcStopCodonsTotalStopCodons() : null);
                    insertStatement.setObject(80, nc != null ? nc.getQcStopCodonsScore() : null);
                    insertStatement.setString(81, nc != null ? nc.getQcStopCodonsStatus() : null);
                    insertStatement.setObject(82, nc != null ? nc.getCoverage() : null);
                    insertStatement.setString(83, nc != null ? nc.getErrors() : null);
                    insertStatement.setString(84, nc != null ? nc.getAaInsertions() : null);
                    insertStatement.addBatch();
                    insertStatement.clearParameters();
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

    private static class NextcladeResultEntry {

        private final String alignedNucSeq;
        private final List<GeneAASeq> geneAASeqs;
        private final NextcladeTsvEntry nextcladeTsvEntry;

        public NextcladeResultEntry(
            String alignedNucSeq,
            List<GeneAASeq> geneAASeqs,
            NextcladeTsvEntry nextcladeTsvEntry
        ) {
            this.alignedNucSeq = alignedNucSeq;
            this.geneAASeqs = geneAASeqs;
            this.nextcladeTsvEntry = nextcladeTsvEntry;
        }
    }
}
