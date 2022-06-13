package ch.ethz.lapis.source.mpox;

public class NextcladeTsvEntry {
    private String seqName;
    private String clade;
    private Integer totalSubstitutions;
    private Integer totalDeletions;
    private Integer totalInsertions;
    private Integer totalFrameShifts;
    private Integer totalAminoacidSubstitutions;
    private Integer totalAminoacidDeletions;
    private Integer totalAminoacidInsertions;
    private Integer totalMissing;
    private Integer totalNonACGTNs;
    private Integer totalPcrPrimerChanges;
    private String pcrPrimerChanges;
    private Float alignmentScore;
    private Integer alignmentStart;
    private Integer alignmentEnd;
    private Float qcOverallScore;
    private String qcOverallStatus;
    private Float qcMissingDataMissingDataThreshold;
    private Float qcMissingDataScore;
    private String qcMissingDataStatus;
    private Integer qcMissingDataTotalMissing;
    private Float qcMixedSitesMixedSitesThreshold;
    private Float qcMixedSitesScore;
    private String qcMixedSitesStatus;
    private Integer qcMixedSitesTotalMixedSites;
    private Float qcPrivateMutationsCutoff;
    private Float qcPrivateMutationsExcess;
    private Float qcPrivateMutationsScore;
    private String qcPrivateMutationsStatus;
    private Integer qcPrivateMutationsTotal;
    private String qcSnpClustersClusteredSNPs;
    private Float qcSnpClustersScore;
    private String qcSnpClustersStatus;
    private Integer qcSnpClustersTotalSNPs;
    private String qcFrameShiftsFrameShifts;
    private Integer qcFrameShiftsTotalFrameShifts;
    private String qcFrameShiftsFrameShiftsIgnored;
    private Integer qcFrameShiftsTotalFrameShiftsIgnored;
    private Float qcFrameShiftsScore;
    private String qcFrameShiftsStatus;
    private String qcStopCodonsStopCodons;
    private Integer qcStopCodonsTotalStopCodons;
    private Float qcStopCodonsScore;
    private String qcStopCodonsStatus;
    private String errors;

    public String getSeqName() {
        return seqName;
    }

    public NextcladeTsvEntry setSeqName(String seqName) {
        this.seqName = seqName;
        return this;
    }

    public String getClade() {
        return clade;
    }

    public NextcladeTsvEntry setClade(String clade) {
        this.clade = clade;
        return this;
    }

    public Integer getTotalSubstitutions() {
        return totalSubstitutions;
    }

    public NextcladeTsvEntry setTotalSubstitutions(Integer totalSubstitutions) {
        this.totalSubstitutions = totalSubstitutions;
        return this;
    }

    public Integer getTotalDeletions() {
        return totalDeletions;
    }

    public NextcladeTsvEntry setTotalDeletions(Integer totalDeletions) {
        this.totalDeletions = totalDeletions;
        return this;
    }

    public Integer getTotalInsertions() {
        return totalInsertions;
    }

    public NextcladeTsvEntry setTotalInsertions(Integer totalInsertions) {
        this.totalInsertions = totalInsertions;
        return this;
    }

    public Integer getTotalFrameShifts() {
        return totalFrameShifts;
    }

    public NextcladeTsvEntry setTotalFrameShifts(Integer totalFrameShifts) {
        this.totalFrameShifts = totalFrameShifts;
        return this;
    }

    public Integer getTotalAminoacidSubstitutions() {
        return totalAminoacidSubstitutions;
    }

    public NextcladeTsvEntry setTotalAminoacidSubstitutions(Integer totalAminoacidSubstitutions) {
        this.totalAminoacidSubstitutions = totalAminoacidSubstitutions;
        return this;
    }

    public Integer getTotalAminoacidDeletions() {
        return totalAminoacidDeletions;
    }

    public NextcladeTsvEntry setTotalAminoacidDeletions(Integer totalAminoacidDeletions) {
        this.totalAminoacidDeletions = totalAminoacidDeletions;
        return this;
    }

    public Integer getTotalAminoacidInsertions() {
        return totalAminoacidInsertions;
    }

    public NextcladeTsvEntry setTotalAminoacidInsertions(Integer totalAminoacidInsertions) {
        this.totalAminoacidInsertions = totalAminoacidInsertions;
        return this;
    }

    public Integer getTotalMissing() {
        return totalMissing;
    }

    public NextcladeTsvEntry setTotalMissing(Integer totalMissing) {
        this.totalMissing = totalMissing;
        return this;
    }

    public Integer getTotalNonACGTNs() {
        return totalNonACGTNs;
    }

    public NextcladeTsvEntry setTotalNonACGTNs(Integer totalNonACGTNs) {
        this.totalNonACGTNs = totalNonACGTNs;
        return this;
    }

    public Integer getTotalPcrPrimerChanges() {
        return totalPcrPrimerChanges;
    }

    public NextcladeTsvEntry setTotalPcrPrimerChanges(Integer totalPcrPrimerChanges) {
        this.totalPcrPrimerChanges = totalPcrPrimerChanges;
        return this;
    }

    public String getPcrPrimerChanges() {
        return pcrPrimerChanges;
    }

    public NextcladeTsvEntry setPcrPrimerChanges(String pcrPrimerChanges) {
        this.pcrPrimerChanges = pcrPrimerChanges;
        return this;
    }

    public Float getAlignmentScore() {
        return alignmentScore;
    }

    public NextcladeTsvEntry setAlignmentScore(Float alignmentScore) {
        this.alignmentScore = alignmentScore;
        return this;
    }

    public Integer getAlignmentStart() {
        return alignmentStart;
    }

    public NextcladeTsvEntry setAlignmentStart(Integer alignmentStart) {
        this.alignmentStart = alignmentStart;
        return this;
    }

    public Integer getAlignmentEnd() {
        return alignmentEnd;
    }

    public NextcladeTsvEntry setAlignmentEnd(Integer alignmentEnd) {
        this.alignmentEnd = alignmentEnd;
        return this;
    }

    public Float getQcOverallScore() {
        return qcOverallScore;
    }

    public NextcladeTsvEntry setQcOverallScore(Float qcOverallScore) {
        this.qcOverallScore = qcOverallScore;
        return this;
    }

    public String getQcOverallStatus() {
        return qcOverallStatus;
    }

    public NextcladeTsvEntry setQcOverallStatus(String qcOverallStatus) {
        this.qcOverallStatus = qcOverallStatus;
        return this;
    }

    public Float getQcMissingDataMissingDataThreshold() {
        return qcMissingDataMissingDataThreshold;
    }

    public NextcladeTsvEntry setQcMissingDataMissingDataThreshold(Float qcMissingDataMissingDataThreshold) {
        this.qcMissingDataMissingDataThreshold = qcMissingDataMissingDataThreshold;
        return this;
    }

    public Float getQcMissingDataScore() {
        return qcMissingDataScore;
    }

    public NextcladeTsvEntry setQcMissingDataScore(Float qcMissingDataScore) {
        this.qcMissingDataScore = qcMissingDataScore;
        return this;
    }

    public String getQcMissingDataStatus() {
        return qcMissingDataStatus;
    }

    public NextcladeTsvEntry setQcMissingDataStatus(String qcMissingDataStatus) {
        this.qcMissingDataStatus = qcMissingDataStatus;
        return this;
    }

    public Integer getQcMissingDataTotalMissing() {
        return qcMissingDataTotalMissing;
    }

    public NextcladeTsvEntry setQcMissingDataTotalMissing(Integer qcMissingDataTotalMissing) {
        this.qcMissingDataTotalMissing = qcMissingDataTotalMissing;
        return this;
    }

    public Float getQcMixedSitesMixedSitesThreshold() {
        return qcMixedSitesMixedSitesThreshold;
    }

    public NextcladeTsvEntry setQcMixedSitesMixedSitesThreshold(Float qcMixedSitesMixedSitesThreshold) {
        this.qcMixedSitesMixedSitesThreshold = qcMixedSitesMixedSitesThreshold;
        return this;
    }

    public Float getQcMixedSitesScore() {
        return qcMixedSitesScore;
    }

    public NextcladeTsvEntry setQcMixedSitesScore(Float qcMixedSitesScore) {
        this.qcMixedSitesScore = qcMixedSitesScore;
        return this;
    }

    public String getQcMixedSitesStatus() {
        return qcMixedSitesStatus;
    }

    public NextcladeTsvEntry setQcMixedSitesStatus(String qcMixedSitesStatus) {
        this.qcMixedSitesStatus = qcMixedSitesStatus;
        return this;
    }

    public Integer getQcMixedSitesTotalMixedSites() {
        return qcMixedSitesTotalMixedSites;
    }

    public NextcladeTsvEntry setQcMixedSitesTotalMixedSites(Integer qcMixedSitesTotalMixedSites) {
        this.qcMixedSitesTotalMixedSites = qcMixedSitesTotalMixedSites;
        return this;
    }

    public Float getQcPrivateMutationsCutoff() {
        return qcPrivateMutationsCutoff;
    }

    public NextcladeTsvEntry setQcPrivateMutationsCutoff(Float qcPrivateMutationsCutoff) {
        this.qcPrivateMutationsCutoff = qcPrivateMutationsCutoff;
        return this;
    }

    public Float getQcPrivateMutationsExcess() {
        return qcPrivateMutationsExcess;
    }

    public NextcladeTsvEntry setQcPrivateMutationsExcess(Float qcPrivateMutationsExcess) {
        this.qcPrivateMutationsExcess = qcPrivateMutationsExcess;
        return this;
    }

    public Float getQcPrivateMutationsScore() {
        return qcPrivateMutationsScore;
    }

    public NextcladeTsvEntry setQcPrivateMutationsScore(Float qcPrivateMutationsScore) {
        this.qcPrivateMutationsScore = qcPrivateMutationsScore;
        return this;
    }

    public String getQcPrivateMutationsStatus() {
        return qcPrivateMutationsStatus;
    }

    public NextcladeTsvEntry setQcPrivateMutationsStatus(String qcPrivateMutationsStatus) {
        this.qcPrivateMutationsStatus = qcPrivateMutationsStatus;
        return this;
    }

    public Integer getQcPrivateMutationsTotal() {
        return qcPrivateMutationsTotal;
    }

    public NextcladeTsvEntry setQcPrivateMutationsTotal(Integer qcPrivateMutationsTotal) {
        this.qcPrivateMutationsTotal = qcPrivateMutationsTotal;
        return this;
    }

    public String getQcSnpClustersClusteredSNPs() {
        return qcSnpClustersClusteredSNPs;
    }

    public NextcladeTsvEntry setQcSnpClustersClusteredSNPs(String qcSnpClustersClusteredSNPs) {
        this.qcSnpClustersClusteredSNPs = qcSnpClustersClusteredSNPs;
        return this;
    }

    public Float getQcSnpClustersScore() {
        return qcSnpClustersScore;
    }

    public NextcladeTsvEntry setQcSnpClustersScore(Float qcSnpClustersScore) {
        this.qcSnpClustersScore = qcSnpClustersScore;
        return this;
    }

    public String getQcSnpClustersStatus() {
        return qcSnpClustersStatus;
    }

    public NextcladeTsvEntry setQcSnpClustersStatus(String qcSnpClustersStatus) {
        this.qcSnpClustersStatus = qcSnpClustersStatus;
        return this;
    }

    public Integer getQcSnpClustersTotalSNPs() {
        return qcSnpClustersTotalSNPs;
    }

    public NextcladeTsvEntry setQcSnpClustersTotalSNPs(Integer qcSnpClustersTotalSNPs) {
        this.qcSnpClustersTotalSNPs = qcSnpClustersTotalSNPs;
        return this;
    }

    public String getQcFrameShiftsFrameShifts() {
        return qcFrameShiftsFrameShifts;
    }

    public NextcladeTsvEntry setQcFrameShiftsFrameShifts(String qcFrameShiftsFrameShifts) {
        this.qcFrameShiftsFrameShifts = qcFrameShiftsFrameShifts;
        return this;
    }

    public Integer getQcFrameShiftsTotalFrameShifts() {
        return qcFrameShiftsTotalFrameShifts;
    }

    public NextcladeTsvEntry setQcFrameShiftsTotalFrameShifts(Integer qcFrameShiftsTotalFrameShifts) {
        this.qcFrameShiftsTotalFrameShifts = qcFrameShiftsTotalFrameShifts;
        return this;
    }

    public String getQcFrameShiftsFrameShiftsIgnored() {
        return qcFrameShiftsFrameShiftsIgnored;
    }

    public NextcladeTsvEntry setQcFrameShiftsFrameShiftsIgnored(String qcFrameShiftsFrameShiftsIgnored) {
        this.qcFrameShiftsFrameShiftsIgnored = qcFrameShiftsFrameShiftsIgnored;
        return this;
    }

    public Integer getQcFrameShiftsTotalFrameShiftsIgnored() {
        return qcFrameShiftsTotalFrameShiftsIgnored;
    }

    public NextcladeTsvEntry setQcFrameShiftsTotalFrameShiftsIgnored(Integer qcFrameShiftsTotalFrameShiftsIgnored) {
        this.qcFrameShiftsTotalFrameShiftsIgnored = qcFrameShiftsTotalFrameShiftsIgnored;
        return this;
    }

    public Float getQcFrameShiftsScore() {
        return qcFrameShiftsScore;
    }

    public NextcladeTsvEntry setQcFrameShiftsScore(Float qcFrameShiftsScore) {
        this.qcFrameShiftsScore = qcFrameShiftsScore;
        return this;
    }

    public String getQcFrameShiftsStatus() {
        return qcFrameShiftsStatus;
    }

    public NextcladeTsvEntry setQcFrameShiftsStatus(String qcFrameShiftsStatus) {
        this.qcFrameShiftsStatus = qcFrameShiftsStatus;
        return this;
    }

    public String getQcStopCodonsStopCodons() {
        return qcStopCodonsStopCodons;
    }

    public NextcladeTsvEntry setQcStopCodonsStopCodons(String qcStopCodonsStopCodons) {
        this.qcStopCodonsStopCodons = qcStopCodonsStopCodons;
        return this;
    }

    public Integer getQcStopCodonsTotalStopCodons() {
        return qcStopCodonsTotalStopCodons;
    }

    public NextcladeTsvEntry setQcStopCodonsTotalStopCodons(Integer qcStopCodonsTotalStopCodons) {
        this.qcStopCodonsTotalStopCodons = qcStopCodonsTotalStopCodons;
        return this;
    }

    public Float getQcStopCodonsScore() {
        return qcStopCodonsScore;
    }

    public NextcladeTsvEntry setQcStopCodonsScore(Float qcStopCodonsScore) {
        this.qcStopCodonsScore = qcStopCodonsScore;
        return this;
    }

    public String getQcStopCodonsStatus() {
        return qcStopCodonsStatus;
    }

    public NextcladeTsvEntry setQcStopCodonsStatus(String qcStopCodonsStatus) {
        this.qcStopCodonsStatus = qcStopCodonsStatus;
        return this;
    }

    public String getErrors() {
        return errors;
    }

    public NextcladeTsvEntry setErrors(String errors) {
        this.errors = errors;
        return this;
    }
}
