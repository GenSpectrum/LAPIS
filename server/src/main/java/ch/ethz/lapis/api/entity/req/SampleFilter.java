package ch.ethz.lapis.api.entity.req;

import ch.ethz.lapis.api.entity.AAMutation;
import ch.ethz.lapis.api.entity.NucMutation;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class SampleFilter<T extends SampleFilter<T>> {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateSubmittedFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateSubmittedTo;

    private Integer yearFrom;
    private Integer yearTo;
    private String yearMonthFrom;
    private String yearMonthTo;

    private String region;
    private String country;
    private String division;
    private String location;
    private String regionExposure;
    private String countryExposure;
    private String divisionExposure;
    private Integer ageFrom;
    private Integer ageTo;
    private String sex;
    private Boolean hospitalized;
    private Boolean died;
    private Boolean fullyVaccinated;
    private List<String> host;
    private String samplingStrategy;
    private String clade;
    private String institution;
    private Float nextcladeQcOverallScoreFrom;
    private Float nextcladeQcOverallScoreTo;
    private Float nextcladeQcMissingDataScoreFrom;
    private Float nextcladeQcMissingDataScoreTo;
    private Float nextcladeQcMixedSitesScoreFrom;
    private Float nextcladeQcMixedSitesScoreTo;
    private Float nextcladeQcPrivateMutationsScoreFrom;
    private Float nextcladeQcPrivateMutationsScoreTo;
    private Float nextcladeQcSnpClustersScoreFrom;
    private Float nextcladeQcSnpClustersScoreTo;
    private Float nextcladeQcFrameShiftsScoreFrom;
    private Float nextcladeQcFrameShiftsScoreTo;
    private Float nextcladeQcStopCodonsScoreFrom;
    private Float nextcladeQcStopCodonsScoreTo;
    private Float nextcladeAlignmentScoreFrom;
    private Float nextcladeAlignmentScoreTo;
    private Integer nextcladeAlignmentStartFrom;
    private Integer nextcladeAlignmentStartTo;
    private Integer nextcladeAlignmentEndFrom;
    private Integer nextcladeAlignmentEndTo;
    private Integer nextcladeTotalSubstitutionsFrom;
    private Integer nextcladeTotalSubstitutionsTo;
    private Integer nextcladeTotalDeletionsFrom;
    private Integer nextcladeTotalDeletionsTo;
    private Integer nextcladeTotalInsertionsFrom;
    private Integer nextcladeTotalInsertionsTo;
    private Integer nextcladeTotalFrameShiftsFrom;
    private Integer nextcladeTotalFrameShiftsTo;
    private Integer nextcladeTotalAminoacidSubstitutionsFrom;
    private Integer nextcladeTotalAminoacidSubstitutionsTo;
    private Integer nextcladeTotalAminoacidDeletionsFrom;
    private Integer nextcladeTotalAminoacidDeletionsTo;
    private Integer nextcladeTotalAminoacidInsertionsFrom;
    private Integer nextcladeTotalAminoacidInsertionsTo;
    private Integer nextcladeTotalMissingFrom;
    private Integer nextcladeTotalMissingTo;
    private Integer nextcladeTotalNonAcgtnsFrom;
    private Integer nextcladeTotalNonAcgtnsTo;
    private Integer nextcladeTotalPcrPrimerChangesFrom;
    private Integer nextcladeTotalPcrPrimerChangesTo;

    private List<NucMutation> nucMutations = new ArrayList<>();
    private List<AAMutation> aaMutations = new ArrayList<>();

    private String variantQuery;

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public T setDateFrom(LocalDate dateFrom) {
        this.dateFrom = dateFrom;
        return (T) this;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public T setDateTo(LocalDate dateTo) {
        this.dateTo = dateTo;
        return (T) this;
    }

    public LocalDate getDateSubmittedFrom() {
        return dateSubmittedFrom;
    }

    public T setDateSubmittedFrom(LocalDate dateSubmittedFrom) {
        this.dateSubmittedFrom = dateSubmittedFrom;
        return (T) this;
    }

    public LocalDate getDateSubmittedTo() {
        return dateSubmittedTo;
    }

    public T setDateSubmittedTo(LocalDate dateSubmittedTo) {
        this.dateSubmittedTo = dateSubmittedTo;
        return (T) this;
    }

    public Integer getYearFrom() {
        return yearFrom;
    }

    public T setYearFrom(Integer yearFrom) {
        this.yearFrom = yearFrom;
        return (T) this;
    }

    public Integer getYearTo() {
        return yearTo;
    }

    public T setYearTo(Integer yearTo) {
        this.yearTo = yearTo;
        return (T) this;
    }

    public String getYearMonthFrom() {
        return yearMonthFrom;
    }

    public T setYearMonthFrom(String yearMonthFrom) {
        this.yearMonthFrom = yearMonthFrom;
        return (T) this;
    }

    public String getYearMonthTo() {
        return yearMonthTo;
    }

    public T setYearMonthTo(String yearMonthTo) {
        this.yearMonthTo = yearMonthTo;
        return (T) this;
    }

    public String getRegion() {
        return region;
    }

    public T setRegion(String region) {
        this.region = region;
        return (T) this;
    }

    public String getCountry() {
        return country;
    }

    public T setCountry(String country) {
        this.country = country;
        return (T) this;
    }

    public String getDivision() {
        return division;
    }

    public T setDivision(String division) {
        this.division = division;
        return (T) this;
    }

    public String getLocation() {
        return location;
    }

    public T setLocation(String location) {
        this.location = location;
        return (T) this;
    }

    public String getRegionExposure() {
        return regionExposure;
    }

    public T setRegionExposure(String regionExposure) {
        this.regionExposure = regionExposure;
        return (T) this;
    }

    public String getCountryExposure() {
        return countryExposure;
    }

    public T setCountryExposure(String countryExposure) {
        this.countryExposure = countryExposure;
        return (T) this;
    }

    public String getDivisionExposure() {
        return divisionExposure;
    }

    public T setDivisionExposure(String divisionExposure) {
        this.divisionExposure = divisionExposure;
        return (T) this;
    }

    public Integer getAgeFrom() {
        return ageFrom;
    }

    public T setAgeFrom(Integer ageFrom) {
        this.ageFrom = ageFrom;
        return (T) this;
    }

    public Integer getAgeTo() {
        return ageTo;
    }

    public T setAgeTo(Integer ageTo) {
        this.ageTo = ageTo;
        return (T) this;
    }

    public String getSex() {
        return sex;
    }

    public T setSex(String sex) {
        this.sex = sex;
        return (T) this;
    }

    public Boolean getHospitalized() {
        return hospitalized;
    }

    public T setHospitalized(Boolean hospitalized) {
        this.hospitalized = hospitalized;
        return (T) this;
    }

    public Boolean getDied() {
        return died;
    }

    public T setDied(Boolean died) {
        this.died = died;
        return (T) this;
    }

    public Boolean getFullyVaccinated() {
        return fullyVaccinated;
    }

    public T setFullyVaccinated(Boolean fullyVaccinated) {
        this.fullyVaccinated = fullyVaccinated;
        return (T) this;
    }

    public List<String> getHost() {
        return host;
    }

    public T setHost(List<String> host) {
        this.host = host;
        return (T) this;
    }

    public String getSamplingStrategy() {
        return samplingStrategy;
    }

    public T setSamplingStrategy(String samplingStrategy) {
        this.samplingStrategy = samplingStrategy;
        return (T) this;
    }

    public String getClade() {
        return clade;
    }

    public T setClade(String clade) {
        this.clade = clade;
        return (T) this;
    }

    public String getInstitution() {
        return institution;
    }

    public T setInstitution(String institution) {
        this.institution = institution;
        return (T) this;
    }

    public List<NucMutation> getNucMutations() {
        return nucMutations;
    }

    public T setNucMutations(List<NucMutation> nucMutations) {
        this.nucMutations = nucMutations;
        return (T) this;
    }

    public List<AAMutation> getAaMutations() {
        return aaMutations;
    }

    public T setAaMutations(List<AAMutation> aaMutations) {
        this.aaMutations = aaMutations;
        return (T) this;
    }

    public String getVariantQuery() {
        return variantQuery;
    }

    public T setVariantQuery(String variantQuery) {
        this.variantQuery = variantQuery;
        return (T) this;
    }

    public Float getNextcladeQcOverallScoreFrom() {
        return nextcladeQcOverallScoreFrom;
    }

    public T setNextcladeQcOverallScoreFrom(Float nextcladeQcOverallScoreFrom) {
        this.nextcladeQcOverallScoreFrom = nextcladeQcOverallScoreFrom;
        return (T) this;
    }

    public Float getNextcladeQcOverallScoreTo() {
        return nextcladeQcOverallScoreTo;
    }

    public T setNextcladeQcOverallScoreTo(Float nextcladeQcOverallScoreTo) {
        this.nextcladeQcOverallScoreTo = nextcladeQcOverallScoreTo;
        return (T) this;
    }

    public Float getNextcladeQcMissingDataScoreFrom() {
        return nextcladeQcMissingDataScoreFrom;
    }

    public T setNextcladeQcMissingDataScoreFrom(Float nextcladeQcMissingDataScoreFrom) {
        this.nextcladeQcMissingDataScoreFrom = nextcladeQcMissingDataScoreFrom;
        return (T) this;
    }

    public Float getNextcladeQcMissingDataScoreTo() {
        return nextcladeQcMissingDataScoreTo;
    }

    public T setNextcladeQcMissingDataScoreTo(Float nextcladeQcMissingDataScoreTo) {
        this.nextcladeQcMissingDataScoreTo = nextcladeQcMissingDataScoreTo;
        return (T) this;
    }

    public Float getNextcladeQcMixedSitesScoreFrom() {
        return nextcladeQcMixedSitesScoreFrom;
    }

    public T setNextcladeQcMixedSitesScoreFrom(Float nextcladeQcMixedSitesScoreFrom) {
        this.nextcladeQcMixedSitesScoreFrom = nextcladeQcMixedSitesScoreFrom;
        return (T) this;
    }

    public Float getNextcladeQcMixedSitesScoreTo() {
        return nextcladeQcMixedSitesScoreTo;
    }

    public T setNextcladeQcMixedSitesScoreTo(Float nextcladeQcMixedSitesScoreTo) {
        this.nextcladeQcMixedSitesScoreTo = nextcladeQcMixedSitesScoreTo;
        return (T) this;
    }

    public Float getNextcladeQcPrivateMutationsScoreFrom() {
        return nextcladeQcPrivateMutationsScoreFrom;
    }

    public T setNextcladeQcPrivateMutationsScoreFrom(Float nextcladeQcPrivateMutationsScoreFrom) {
        this.nextcladeQcPrivateMutationsScoreFrom = nextcladeQcPrivateMutationsScoreFrom;
        return (T) this;
    }

    public Float getNextcladeQcPrivateMutationsScoreTo() {
        return nextcladeQcPrivateMutationsScoreTo;
    }

    public T setNextcladeQcPrivateMutationsScoreTo(Float nextcladeQcPrivateMutationsScoreTo) {
        this.nextcladeQcPrivateMutationsScoreTo = nextcladeQcPrivateMutationsScoreTo;
        return (T) this;
    }

    public Float getNextcladeQcSnpClustersScoreFrom() {
        return nextcladeQcSnpClustersScoreFrom;
    }

    public T setNextcladeQcSnpClustersScoreFrom(Float nextcladeQcSnpClustersScoreFrom) {
        this.nextcladeQcSnpClustersScoreFrom = nextcladeQcSnpClustersScoreFrom;
        return (T) this;
    }

    public Float getNextcladeQcSnpClustersScoreTo() {
        return nextcladeQcSnpClustersScoreTo;
    }

    public T setNextcladeQcSnpClustersScoreTo(Float nextcladeQcSnpClustersScoreTo) {
        this.nextcladeQcSnpClustersScoreTo = nextcladeQcSnpClustersScoreTo;
        return (T) this;
    }

    public Float getNextcladeQcFrameShiftsScoreFrom() {
        return nextcladeQcFrameShiftsScoreFrom;
    }

    public T setNextcladeQcFrameShiftsScoreFrom(Float nextcladeQcFrameShiftsScoreFrom) {
        this.nextcladeQcFrameShiftsScoreFrom = nextcladeQcFrameShiftsScoreFrom;
        return (T) this;
    }

    public Float getNextcladeQcFrameShiftsScoreTo() {
        return nextcladeQcFrameShiftsScoreTo;
    }

    public T setNextcladeQcFrameShiftsScoreTo(Float nextcladeQcFrameShiftsScoreTo) {
        this.nextcladeQcFrameShiftsScoreTo = nextcladeQcFrameShiftsScoreTo;
        return (T) this;
    }

    public Float getNextcladeQcStopCodonsScoreFrom() {
        return nextcladeQcStopCodonsScoreFrom;
    }

    public T setNextcladeQcStopCodonsScoreFrom(Float nextcladeQcStopCodonsScoreFrom) {
        this.nextcladeQcStopCodonsScoreFrom = nextcladeQcStopCodonsScoreFrom;
        return (T) this;
    }

    public Float getNextcladeQcStopCodonsScoreTo() {
        return nextcladeQcStopCodonsScoreTo;
    }

    public T setNextcladeQcStopCodonsScoreTo(Float nextcladeQcStopCodonsScoreTo) {
        this.nextcladeQcStopCodonsScoreTo = nextcladeQcStopCodonsScoreTo;
        return (T) this;
    }

    public Float getNextcladeAlignmentScoreFrom() {
        return nextcladeAlignmentScoreFrom;
    }

    public T setNextcladeAlignmentScoreFrom(Float nextcladeAlignmentScoreFrom) {
        this.nextcladeAlignmentScoreFrom = nextcladeAlignmentScoreFrom;
         return (T) this;
    }

    public Float getNextcladeAlignmentScoreTo() {
        return nextcladeAlignmentScoreTo;
    }

    public T setNextcladeAlignmentScoreTo(Float nextcladeAlignmentScoreTo) {
        this.nextcladeAlignmentScoreTo = nextcladeAlignmentScoreTo;
         return (T) this;
    }

    public Integer getNextcladeAlignmentStartFrom() {
        return nextcladeAlignmentStartFrom;
    }

    public T setNextcladeAlignmentStartFrom(Integer nextcladeAlignmentStartFrom) {
        this.nextcladeAlignmentStartFrom = nextcladeAlignmentStartFrom;
         return (T) this;
    }

    public Integer getNextcladeAlignmentStartTo() {
        return nextcladeAlignmentStartTo;
    }

    public T setNextcladeAlignmentStartTo(Integer nextcladeAlignmentStartTo) {
        this.nextcladeAlignmentStartTo = nextcladeAlignmentStartTo;
         return (T) this;
    }

    public Integer getNextcladeAlignmentEndFrom() {
        return nextcladeAlignmentEndFrom;
    }

    public T setNextcladeAlignmentEndFrom(Integer nextcladeAlignmentEndFrom) {
        this.nextcladeAlignmentEndFrom = nextcladeAlignmentEndFrom;
         return (T) this;
    }

    public Integer getNextcladeAlignmentEndTo() {
        return nextcladeAlignmentEndTo;
    }

    public T setNextcladeAlignmentEndTo(Integer nextcladeAlignmentEndTo) {
        this.nextcladeAlignmentEndTo = nextcladeAlignmentEndTo;
         return (T) this;
    }

    public Integer getNextcladeTotalSubstitutionsFrom() {
        return nextcladeTotalSubstitutionsFrom;
    }

    public T setNextcladeTotalSubstitutionsFrom(Integer nextcladeTotalSubstitutionsFrom) {
        this.nextcladeTotalSubstitutionsFrom = nextcladeTotalSubstitutionsFrom;
         return (T) this;
    }

    public Integer getNextcladeTotalSubstitutionsTo() {
        return nextcladeTotalSubstitutionsTo;
    }

    public T setNextcladeTotalSubstitutionsTo(Integer nextcladeTotalSubstitutionsTo) {
        this.nextcladeTotalSubstitutionsTo = nextcladeTotalSubstitutionsTo;
         return (T) this;
    }

    public Integer getNextcladeTotalDeletionsFrom() {
        return nextcladeTotalDeletionsFrom;
    }

    public T setNextcladeTotalDeletionsFrom(Integer nextcladeTotalDeletionsFrom) {
        this.nextcladeTotalDeletionsFrom = nextcladeTotalDeletionsFrom;
         return (T) this;
    }

    public Integer getNextcladeTotalDeletionsTo() {
        return nextcladeTotalDeletionsTo;
    }

    public T setNextcladeTotalDeletionsTo(Integer nextcladeTotalDeletionsTo) {
        this.nextcladeTotalDeletionsTo = nextcladeTotalDeletionsTo;
         return (T) this;
    }

    public Integer getNextcladeTotalInsertionsFrom() {
        return nextcladeTotalInsertionsFrom;
    }

    public T setNextcladeTotalInsertionsFrom(Integer nextcladeTotalInsertionsFrom) {
        this.nextcladeTotalInsertionsFrom = nextcladeTotalInsertionsFrom;
         return (T) this;
    }

    public Integer getNextcladeTotalInsertionsTo() {
        return nextcladeTotalInsertionsTo;
    }

    public T setNextcladeTotalInsertionsTo(Integer nextcladeTotalInsertionsTo) {
        this.nextcladeTotalInsertionsTo = nextcladeTotalInsertionsTo;
         return (T) this;
    }

    public Integer getNextcladeTotalFrameShiftsFrom() {
        return nextcladeTotalFrameShiftsFrom;
    }

    public T setNextcladeTotalFrameShiftsFrom(Integer nextcladeTotalFrameShiftsFrom) {
        this.nextcladeTotalFrameShiftsFrom = nextcladeTotalFrameShiftsFrom;
         return (T) this;
    }

    public Integer getNextcladeTotalFrameShiftsTo() {
        return nextcladeTotalFrameShiftsTo;
    }

    public T setNextcladeTotalFrameShiftsTo(Integer nextcladeTotalFrameShiftsTo) {
        this.nextcladeTotalFrameShiftsTo = nextcladeTotalFrameShiftsTo;
         return (T) this;
    }

    public Integer getNextcladeTotalAminoacidSubstitutionsFrom() {
        return nextcladeTotalAminoacidSubstitutionsFrom;
    }

    public T setNextcladeTotalAminoacidSubstitutionsFrom(Integer nextcladeTotalAminoacidSubstitutionsFrom) {
        this.nextcladeTotalAminoacidSubstitutionsFrom = nextcladeTotalAminoacidSubstitutionsFrom;
         return (T) this;
    }

    public Integer getNextcladeTotalAminoacidSubstitutionsTo() {
        return nextcladeTotalAminoacidSubstitutionsTo;
    }

    public T setNextcladeTotalAminoacidSubstitutionsTo(Integer nextcladeTotalAminoacidSubstitutionsTo) {
        this.nextcladeTotalAminoacidSubstitutionsTo = nextcladeTotalAminoacidSubstitutionsTo;
         return (T) this;
    }

    public Integer getNextcladeTotalAminoacidDeletionsFrom() {
        return nextcladeTotalAminoacidDeletionsFrom;
    }

    public T setNextcladeTotalAminoacidDeletionsFrom(Integer nextcladeTotalAminoacidDeletionsFrom) {
        this.nextcladeTotalAminoacidDeletionsFrom = nextcladeTotalAminoacidDeletionsFrom;
         return (T) this;
    }

    public Integer getNextcladeTotalAminoacidDeletionsTo() {
        return nextcladeTotalAminoacidDeletionsTo;
    }

    public T setNextcladeTotalAminoacidDeletionsTo(Integer nextcladeTotalAminoacidDeletionsTo) {
        this.nextcladeTotalAminoacidDeletionsTo = nextcladeTotalAminoacidDeletionsTo;
         return (T) this;
    }

    public Integer getNextcladeTotalAminoacidInsertionsFrom() {
        return nextcladeTotalAminoacidInsertionsFrom;
    }

    public T setNextcladeTotalAminoacidInsertionsFrom(Integer nextcladeTotalAminoacidInsertionsFrom) {
        this.nextcladeTotalAminoacidInsertionsFrom = nextcladeTotalAminoacidInsertionsFrom;
         return (T) this;
    }

    public Integer getNextcladeTotalAminoacidInsertionsTo() {
        return nextcladeTotalAminoacidInsertionsTo;
    }

    public T setNextcladeTotalAminoacidInsertionsTo(Integer nextcladeTotalAminoacidInsertionsTo) {
        this.nextcladeTotalAminoacidInsertionsTo = nextcladeTotalAminoacidInsertionsTo;
         return (T) this;
    }

    public Integer getNextcladeTotalMissingFrom() {
        return nextcladeTotalMissingFrom;
    }

    public T setNextcladeTotalMissingFrom(Integer nextcladeTotalMissingFrom) {
        this.nextcladeTotalMissingFrom = nextcladeTotalMissingFrom;
         return (T) this;
    }

    public Integer getNextcladeTotalMissingTo() {
        return nextcladeTotalMissingTo;
    }

    public T setNextcladeTotalMissingTo(Integer nextcladeTotalMissingTo) {
        this.nextcladeTotalMissingTo = nextcladeTotalMissingTo;
         return (T) this;
    }

    public Integer getNextcladeTotalNonAcgtnsFrom() {
        return nextcladeTotalNonAcgtnsFrom;
    }

    public T setNextcladeTotalNonAcgtnsFrom(Integer nextcladeTotalNonAcgtnsFrom) {
        this.nextcladeTotalNonAcgtnsFrom = nextcladeTotalNonAcgtnsFrom;
         return (T) this;
    }

    public Integer getNextcladeTotalNonAcgtnsTo() {
        return nextcladeTotalNonAcgtnsTo;
    }

    public T setNextcladeTotalNonAcgtnsTo(Integer nextcladeTotalNonAcgtnsTo) {
        this.nextcladeTotalNonAcgtnsTo = nextcladeTotalNonAcgtnsTo;
         return (T) this;
    }

    public Integer getNextcladeTotalPcrPrimerChangesFrom() {
        return nextcladeTotalPcrPrimerChangesFrom;
    }

    public T setNextcladeTotalPcrPrimerChangesFrom(Integer nextcladeTotalPcrPrimerChangesFrom) {
        this.nextcladeTotalPcrPrimerChangesFrom = nextcladeTotalPcrPrimerChangesFrom;
         return (T) this;
    }

    public Integer getNextcladeTotalPcrPrimerChangesTo() {
        return nextcladeTotalPcrPrimerChangesTo;
    }

    public T setNextcladeTotalPcrPrimerChangesTo(Integer nextcladeTotalPcrPrimerChangesTo) {
        this.nextcladeTotalPcrPrimerChangesTo = nextcladeTotalPcrPrimerChangesTo;
         return (T) this;
    }
}
