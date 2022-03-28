package ch.ethz.lapis.source.gisaid;

import ch.ethz.lapis.util.Utils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class NextcladeTsvFileReader
    implements Iterator<NextcladeTsvEntry>, Iterable<NextcladeTsvEntry>, AutoCloseable {

    private final Iterator<CSVRecord> iterator;
    private final InputStream in;

    public NextcladeTsvFileReader(InputStream in) {
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
    public NextcladeTsvEntry next() {
        CSVRecord csv = iterator.next();
        return new NextcladeTsvEntry()
            .setSeqName(cleanString(csv.get("seqName")))
            .setClade(cleanString(csv.get("clade")))
            .setClade(cleanString(csv.get("Nextclade_pango")))
            .setTotalSubstitutions(Utils.nullableIntegerValue(csv.get("totalSubstitutions")))
            .setTotalDeletions(Utils.nullableIntegerValue(csv.get("totalDeletions")))
            .setTotalInsertions(Utils.nullableIntegerValue(csv.get("totalInsertions")))
            .setTotalFrameShifts(Utils.nullableIntegerValue(csv.get("totalFrameShifts")))
            .setTotalAminoacidSubstitutions(Utils.nullableIntegerValue(csv.get("totalAminoacidSubstitutions")))
            .setTotalAminoacidDeletions(Utils.nullableIntegerValue(csv.get("totalAminoacidDeletions")))
            .setTotalAminoacidInsertions(Utils.nullableIntegerValue(csv.get("totalAminoacidInsertions")))
            .setTotalMissing(Utils.nullableIntegerValue(csv.get("totalMissing")))
            .setTotalNonACGTNs(Utils.nullableIntegerValue(csv.get("totalNonACGTNs")))
            .setTotalPcrPrimerChanges(Utils.nullableIntegerValue(csv.get("totalPcrPrimerChanges")))
            .setPcrPrimerChanges(Utils.nullableIntegerValue(csv.get("pcrPrimerChanges")))
            .setAlignmentScore(Utils.nullableIntegerValue(csv.get("alignmentScore")))
            .setAlignmentStart(Utils.nullableIntegerValue(csv.get("alignmentStart")))
            .setAlignmentEnd(Utils.nullableIntegerValue(csv.get("alignmentEnd")))
            .setQcOverallScore(Utils.nullableFloatValue(csv.get("qc.overallScore")))
            .setQcOverallStatus(cleanString(csv.get("qc.overallStatus")))
            .setQcMissingDataMissingDataThreshold(Utils.nullableIntegerValue(csv.get("qc.missingData.missingDataThreshold")))
            .setQcMissingDataScore(Utils.nullableFloatValue(csv.get("qc.missingData.score")))
            .setQcMissingDataStatus(cleanString(csv.get("qc.missingData.status")))
            .setQcMissingDataTotalMissing(Utils.nullableIntegerValue(csv.get("qc.missingData.totalMissing")))
            .setQcMixedSitesMixedSitesThreshold(Utils.nullableIntegerValue(csv.get("qc.mixedSites.mixedSitesThreshold")))
            .setQcMixedSitesScore(Utils.nullableIntegerValue(csv.get("qc.mixedSites.score")))
            .setQcMixedSitesStatus(cleanString(csv.get("qc.mixedSites.status")))
            .setQcMixedSitesTotalMixedSites(Utils.nullableIntegerValue(csv.get("qc.mixedSites.totalMixedSites")))
            .setQcPrivateMutationsCutoff(Utils.nullableIntegerValue(csv.get("qc.privateMutations.cutoff")))
            .setQcPrivateMutationsExcess(Utils.nullableIntegerValue(csv.get("qc.privateMutations.excess")))
            .setQcPrivateMutationsScore(Utils.nullableFloatValue(csv.get("qc.privateMutations.score")))
            .setQcPrivateMutationsStatus(cleanString(csv.get("qc.privateMutations.status")))
            .setQcPrivateMutationsTotal(Utils.nullableIntegerValue(csv.get("qc.privateMutations.total")))
            .setQcSnpClustersClusteredSNPs(cleanString(csv.get("qc.snpClusters.clusteredSNPs")))
            .setQcSnpClustersScore(Utils.nullableFloatValue(csv.get("qc.snpClusters.score")))
            .setQcSnpClustersStatus(cleanString(csv.get("qc.snpClusters.status")))
            .setQcSnpClustersTotalSNPs(Utils.nullableIntegerValue(csv.get("qc.snpClusters.totalSNPs")))
            .setQcFrameShiftsFrameShifts(cleanString(csv.get("qc.frameShifts.frameShifts")))
            .setQcFrameShiftsTotalFrameShifts(Utils.nullableIntegerValue(csv.get("qc.frameShifts.totalFrameShifts")))
            .setQcFrameShiftsFrameShiftsIgnored(cleanString(csv.get("qc.frameShifts.frameShiftsIgnored")))
            .setQcFrameShiftsTotalFrameShiftsIgnored(Utils.nullableIntegerValue(csv.get("qc.frameShifts.totalFrameShiftsIgnored")))
            .setQcFrameShiftsScore(Utils.nullableIntegerValue(csv.get("qc.frameShifts.score")))
            .setQcFrameShiftsStatus(cleanString(csv.get("qc.frameShifts.status")))
            .setQcStopCodonsStopCodons(cleanString(csv.get("qc.stopCodons.stopCodons")))
            .setQcStopCodonsTotalStopCodons(Utils.nullableIntegerValue(csv.get("qc.stopCodons.totalStopCodons")))
            .setQcStopCodonsScore(Utils.nullableIntegerValue(csv.get("qc.stopCodons.score")))
            .setQcStopCodonsStatus(cleanString(csv.get("qc.stopCodons.status")))
            .setErrors(cleanString(csv.get("errors")));
    }

    @Override
    public Iterator<NextcladeTsvEntry> iterator() {
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
