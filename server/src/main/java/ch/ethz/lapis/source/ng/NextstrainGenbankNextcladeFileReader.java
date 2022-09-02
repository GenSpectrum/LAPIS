package ch.ethz.lapis.source.ng;

import ch.ethz.lapis.util.ReferenceGenomeData;
import ch.ethz.lapis.util.Utils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NextstrainGenbankNextcladeFileReader
    implements Iterator<NextstrainGenbankNextcladeEntry>, Iterable<NextstrainGenbankNextcladeEntry>, AutoCloseable {

    private final Iterator<CSVRecord> iterator;
    private final InputStream in;

    public NextstrainGenbankNextcladeFileReader(InputStream in) {
        this.in = in;
        try {
            CSVFormat format = CSVFormat.TDF
                .withFirstRecordAsHeader();
            CSVParser parser = CSVParser.parse(in, StandardCharsets.UTF_8, format);
            iterator = parser.iterator();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public NextstrainGenbankNextcladeEntry next() {
        CSVRecord csv = iterator.next();
        String aaMutations = "";
        String aaSubs = csv.get("aaSubstitutions");
        String aaDels = csv.get("aaDeletions");
        if (!aaSubs.isBlank() && !aaDels.isBlank()) {
            aaMutations = aaSubs + "," + aaDels;
        } else if (!aaSubs.isBlank() && aaDels.isBlank()) {
            aaMutations = aaSubs;
        } else if (aaSubs.isBlank() && !aaDels.isBlank()) {
            aaMutations = aaDels;
        }
        ReferenceGenomeData refData = ReferenceGenomeData.getInstance();
        String nucDels = Arrays.stream(csv.get("deletions").split(","))
            .flatMap(nucDel -> {
                if (nucDel.isBlank()) {
                    return Stream.empty();
                }
                // Nextclade reports the deletions either as a single position (e.g., 28254) or as a range
                // (e.g., 22029-22034). We transform it into "22029-,22030-,22031-" ...
                Stream<Integer> posStream;
                if (!nucDel.contains("-")) {
                    int pos = Integer.parseInt(nucDel);
                    posStream = Stream.of(pos);
                } else {
                    String[] range = nucDel.split("-");
                    int rangeFrom = Integer.parseInt(range[0]);
                    int rangeTo = Integer.parseInt(range[1]);
                    posStream = IntStream.range(rangeFrom, rangeTo + 1).boxed();
                }
                return posStream.map(pos -> Character.toString(refData.getNucleotideBase(pos)) + pos + "-");
            })
            .collect(Collectors.joining(","));
        return new NextstrainGenbankNextcladeEntry()
            .setStrain(csv.get("seqName"))
            .setAaMutations(aaMutations)
            .setAaInsertions(csv.get("aaInsertions"))
            .setNucSubstitutions(csv.get("substitutions"))
            .setNucDeletions(nucDels)
            .setNucInsertions(csv.get("insertions"))
            .setTotalSubstitutions(Utils.nullableForcedIntegerValue(csv.get("totalSubstitutions")))
            .setTotalDeletions(Utils.nullableForcedIntegerValue(csv.get("totalDeletions")))
            .setTotalInsertions(Utils.nullableForcedIntegerValue(csv.get("totalInsertions")))
            .setTotalFrameShifts(Utils.nullableForcedIntegerValue(csv.get("totalFrameShifts")))
            .setTotalAminoacidSubstitutions(Utils.nullableForcedIntegerValue(csv.get("totalAminoacidSubstitutions")))
            .setTotalAminoacidDeletions(Utils.nullableForcedIntegerValue(csv.get("totalAminoacidDeletions")))
            .setTotalAminoacidInsertions(Utils.nullableForcedIntegerValue(csv.get("totalAminoacidInsertions")))
            .setTotalMissing(Utils.nullableForcedIntegerValue(csv.get("totalMissing")))
            .setTotalNonACGTNs(Utils.nullableForcedIntegerValue(csv.get("totalNonACGTNs")))
            .setTotalPcrPrimerChanges(Utils.nullableForcedIntegerValue(csv.get("totalPcrPrimerChanges")))
            .setPcrPrimerChanges(cleanString(csv.get("pcrPrimerChanges")))
            .setAlignmentScore(Utils.nullableFloatValue(csv.get("alignmentScore")))
            .setAlignmentStart(Utils.nullableForcedIntegerValue(csv.get("alignmentStart")))
            .setAlignmentEnd(Utils.nullableForcedIntegerValue(csv.get("alignmentEnd")))
            .setQcOverallScore(Utils.nullableFloatValue(csv.get("qc.overallScore")))
            .setQcOverallStatus(cleanString(csv.get("qc.overallStatus")))
            .setQcMissingDataMissingDataThreshold(Utils.nullableFloatValue(csv.get("qc.missingData.missingDataThreshold")))
            .setQcMissingDataScore(Utils.nullableFloatValue(csv.get("qc.missingData.score")))
            .setQcMissingDataStatus(cleanString(csv.get("qc.missingData.status")))
            .setQcMissingDataTotalMissing(Utils.nullableForcedIntegerValue(csv.get("qc.missingData.totalMissing")))
            .setQcMixedSitesMixedSitesThreshold(Utils.nullableFloatValue(csv.get("qc.mixedSites.mixedSitesThreshold")))
            .setQcMixedSitesScore(Utils.nullableFloatValue(csv.get("qc.mixedSites.score")))
            .setQcMixedSitesStatus(cleanString(csv.get("qc.mixedSites.status")))
            .setQcMixedSitesTotalMixedSites(Utils.nullableForcedIntegerValue(csv.get("qc.mixedSites.totalMixedSites")))
            .setQcPrivateMutationsCutoff(Utils.nullableFloatValue(csv.get("qc.privateMutations.cutoff")))
            .setQcPrivateMutationsExcess(Utils.nullableFloatValue(csv.get("qc.privateMutations.excess")))
            .setQcPrivateMutationsScore(Utils.nullableFloatValue(csv.get("qc.privateMutations.score")))
            .setQcPrivateMutationsStatus(cleanString(csv.get("qc.privateMutations.status")))
            .setQcPrivateMutationsTotal(Utils.nullableForcedIntegerValue(csv.get("qc.privateMutations.total")))
            .setQcSnpClustersClusteredSNPs(cleanString(csv.get("qc.snpClusters.clusteredSNPs")))
            .setQcSnpClustersScore(Utils.nullableFloatValue(csv.get("qc.snpClusters.score")))
            .setQcSnpClustersStatus(cleanString(csv.get("qc.snpClusters.status")))
            .setQcSnpClustersTotalSNPs(Utils.nullableForcedIntegerValue(csv.get("qc.snpClusters.totalSNPs")))
            .setQcFrameShiftsFrameShifts(cleanString(csv.get("qc.frameShifts.frameShifts")))
            .setQcFrameShiftsTotalFrameShifts(Utils.nullableForcedIntegerValue(csv.get("qc.frameShifts.totalFrameShifts")))
            .setQcFrameShiftsFrameShiftsIgnored(cleanString(csv.get("qc.frameShifts.frameShiftsIgnored")))
            .setQcFrameShiftsTotalFrameShiftsIgnored(Utils.nullableForcedIntegerValue(csv.get("qc.frameShifts.totalFrameShiftsIgnored")))
            .setQcFrameShiftsScore(Utils.nullableFloatValue(csv.get("qc.frameShifts.score")))
            .setQcFrameShiftsStatus(cleanString(csv.get("qc.frameShifts.status")))
            .setQcStopCodonsStopCodons(cleanString(csv.get("qc.stopCodons.stopCodons")))
            .setQcStopCodonsTotalStopCodons(Utils.nullableForcedIntegerValue(csv.get("qc.stopCodons.totalStopCodons")))
            .setQcStopCodonsScore(Utils.nullableFloatValue(csv.get("qc.stopCodons.score")))
            .setQcStopCodonsStatus(cleanString(csv.get("qc.stopCodons.status")))
            .setCoverage(Utils.nullableFloatValue(csv.get("coverage")))
            .setErrors(cleanString(csv.get("errors")));
    }

    @Override
    public Iterator<NextstrainGenbankNextcladeEntry> iterator() {
        return this;
    }

    @Override
    public void close() throws IOException {
        if (in != null) {
            in.close();
        }
    }

    private String cleanString(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        s = s.trim();
        // I don't know if this is still required. There were Nextclade/Nextstrain outputs that had ? and unknown, but
        // I didn't check whether this still happens with the Nextclade version that we use.
        if ("?".equals(s) || "unknown".equalsIgnoreCase(s)) {
            return null;
        }
        return s;
    }
}
