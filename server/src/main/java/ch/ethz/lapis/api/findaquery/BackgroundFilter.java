package ch.ethz.lapis.api.findaquery;

import ch.ethz.lapis.api.entity.req.SampleDetailRequest;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;


/**
 * This class contains a subset of the fields of SampleFilter.
 */
public class BackgroundFilter {

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
    private Float nextcladeCoverageFrom;
    private Float nextcladeCoverageTo;

    public SampleDetailRequest toSampleDetailRequest() {
        return new SampleDetailRequest()
            .setDateFrom(dateFrom)
            .setDateTo(dateTo)
            .setDateSubmittedFrom(dateSubmittedFrom)
            .setDateTo(dateSubmittedTo)
            .setYearFrom(yearFrom)
            .setYearTo(yearTo)
            .setYearMonthFrom(yearMonthFrom)
            .setYearMonthTo(yearMonthTo)
            .setRegion(region)
            .setCountry(country)
            .setDivision(division)
            .setNextcladeQcOverallScoreFrom(nextcladeQcOverallScoreFrom)
            .setNextcladeQcOverallScoreTo(nextcladeQcOverallScoreTo)
            .setNextcladeQcMissingDataScoreFrom(nextcladeQcMissingDataScoreFrom)
            .setNextcladeQcMissingDataScoreTo(nextcladeQcMissingDataScoreTo)
            .setNextcladeQcMixedSitesScoreFrom(nextcladeQcMixedSitesScoreFrom)
            .setNextcladeQcMixedSitesScoreTo(nextcladeQcMixedSitesScoreTo)
            .setNextcladeQcPrivateMutationsScoreFrom(nextcladeQcPrivateMutationsScoreFrom)
            .setNextcladeQcPrivateMutationsScoreTo(nextcladeQcPrivateMutationsScoreTo)
            .setNextcladeQcSnpClustersScoreFrom(nextcladeQcSnpClustersScoreFrom)
            .setNextcladeQcSnpClustersScoreTo(nextcladeQcSnpClustersScoreTo)
            .setNextcladeQcFrameShiftsScoreFrom(nextcladeQcFrameShiftsScoreFrom)
            .setNextcladeQcFrameShiftsScoreTo(nextcladeQcFrameShiftsScoreTo)
            .setNextcladeQcStopCodonsScoreFrom(nextcladeQcStopCodonsScoreFrom)
            .setNextcladeQcStopCodonsScoreTo(nextcladeQcStopCodonsScoreTo)
            .setNextcladeCoverageFrom(nextcladeCoverageFrom)
            .setNextcladeCoverageTo(nextcladeCoverageTo);

    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public BackgroundFilter setDateFrom(LocalDate dateFrom) {
        this.dateFrom = dateFrom;
        return this;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public BackgroundFilter setDateTo(LocalDate dateTo) {
        this.dateTo = dateTo;
        return this;
    }

    public LocalDate getDateSubmittedFrom() {
        return dateSubmittedFrom;
    }

    public BackgroundFilter setDateSubmittedFrom(LocalDate dateSubmittedFrom) {
        this.dateSubmittedFrom = dateSubmittedFrom;
        return this;
    }

    public LocalDate getDateSubmittedTo() {
        return dateSubmittedTo;
    }

    public BackgroundFilter setDateSubmittedTo(LocalDate dateSubmittedTo) {
        this.dateSubmittedTo = dateSubmittedTo;
        return this;
    }

    public Integer getYearFrom() {
        return yearFrom;
    }

    public BackgroundFilter setYearFrom(Integer yearFrom) {
        this.yearFrom = yearFrom;
        return this;
    }

    public Integer getYearTo() {
        return yearTo;
    }

    public BackgroundFilter setYearTo(Integer yearTo) {
        this.yearTo = yearTo;
        return this;
    }

    public String getYearMonthFrom() {
        return yearMonthFrom;
    }

    public BackgroundFilter setYearMonthFrom(String yearMonthFrom) {
        this.yearMonthFrom = yearMonthFrom;
        return this;
    }

    public String getYearMonthTo() {
        return yearMonthTo;
    }

    public BackgroundFilter setYearMonthTo(String yearMonthTo) {
        this.yearMonthTo = yearMonthTo;
        return this;
    }

    public String getRegion() {
        return region;
    }

    public BackgroundFilter setRegion(String region) {
        this.region = region;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public BackgroundFilter setCountry(String country) {
        this.country = country;
        return this;
    }

    public String getDivision() {
        return division;
    }

    public BackgroundFilter setDivision(String division) {
        this.division = division;
        return this;
    }

    public Float getNextcladeQcOverallScoreFrom() {
        return nextcladeQcOverallScoreFrom;
    }

    public BackgroundFilter setNextcladeQcOverallScoreFrom(
        Float nextcladeQcOverallScoreFrom) {
        this.nextcladeQcOverallScoreFrom = nextcladeQcOverallScoreFrom;
        return this;
    }

    public Float getNextcladeQcOverallScoreTo() {
        return nextcladeQcOverallScoreTo;
    }

    public BackgroundFilter setNextcladeQcOverallScoreTo(Float nextcladeQcOverallScoreTo) {
        this.nextcladeQcOverallScoreTo = nextcladeQcOverallScoreTo;
        return this;
    }

    public Float getNextcladeQcMissingDataScoreFrom() {
        return nextcladeQcMissingDataScoreFrom;
    }

    public BackgroundFilter setNextcladeQcMissingDataScoreFrom(
        Float nextcladeQcMissingDataScoreFrom) {
        this.nextcladeQcMissingDataScoreFrom = nextcladeQcMissingDataScoreFrom;
        return this;
    }

    public Float getNextcladeQcMissingDataScoreTo() {
        return nextcladeQcMissingDataScoreTo;
    }

    public BackgroundFilter setNextcladeQcMissingDataScoreTo(
        Float nextcladeQcMissingDataScoreTo) {
        this.nextcladeQcMissingDataScoreTo = nextcladeQcMissingDataScoreTo;
        return this;
    }

    public Float getNextcladeQcMixedSitesScoreFrom() {
        return nextcladeQcMixedSitesScoreFrom;
    }

    public BackgroundFilter setNextcladeQcMixedSitesScoreFrom(
        Float nextcladeQcMixedSitesScoreFrom) {
        this.nextcladeQcMixedSitesScoreFrom = nextcladeQcMixedSitesScoreFrom;
        return this;
    }

    public Float getNextcladeQcMixedSitesScoreTo() {
        return nextcladeQcMixedSitesScoreTo;
    }

    public BackgroundFilter setNextcladeQcMixedSitesScoreTo(
        Float nextcladeQcMixedSitesScoreTo) {
        this.nextcladeQcMixedSitesScoreTo = nextcladeQcMixedSitesScoreTo;
        return this;
    }

    public Float getNextcladeQcPrivateMutationsScoreFrom() {
        return nextcladeQcPrivateMutationsScoreFrom;
    }

    public BackgroundFilter setNextcladeQcPrivateMutationsScoreFrom(
        Float nextcladeQcPrivateMutationsScoreFrom) {
        this.nextcladeQcPrivateMutationsScoreFrom = nextcladeQcPrivateMutationsScoreFrom;
        return this;
    }

    public Float getNextcladeQcPrivateMutationsScoreTo() {
        return nextcladeQcPrivateMutationsScoreTo;
    }

    public BackgroundFilter setNextcladeQcPrivateMutationsScoreTo(
        Float nextcladeQcPrivateMutationsScoreTo) {
        this.nextcladeQcPrivateMutationsScoreTo = nextcladeQcPrivateMutationsScoreTo;
        return this;
    }

    public Float getNextcladeQcSnpClustersScoreFrom() {
        return nextcladeQcSnpClustersScoreFrom;
    }

    public BackgroundFilter setNextcladeQcSnpClustersScoreFrom(
        Float nextcladeQcSnpClustersScoreFrom) {
        this.nextcladeQcSnpClustersScoreFrom = nextcladeQcSnpClustersScoreFrom;
        return this;
    }

    public Float getNextcladeQcSnpClustersScoreTo() {
        return nextcladeQcSnpClustersScoreTo;
    }

    public BackgroundFilter setNextcladeQcSnpClustersScoreTo(
        Float nextcladeQcSnpClustersScoreTo) {
        this.nextcladeQcSnpClustersScoreTo = nextcladeQcSnpClustersScoreTo;
        return this;
    }

    public Float getNextcladeQcFrameShiftsScoreFrom() {
        return nextcladeQcFrameShiftsScoreFrom;
    }

    public BackgroundFilter setNextcladeQcFrameShiftsScoreFrom(
        Float nextcladeQcFrameShiftsScoreFrom) {
        this.nextcladeQcFrameShiftsScoreFrom = nextcladeQcFrameShiftsScoreFrom;
        return this;
    }

    public Float getNextcladeQcFrameShiftsScoreTo() {
        return nextcladeQcFrameShiftsScoreTo;
    }

    public BackgroundFilter setNextcladeQcFrameShiftsScoreTo(
        Float nextcladeQcFrameShiftsScoreTo) {
        this.nextcladeQcFrameShiftsScoreTo = nextcladeQcFrameShiftsScoreTo;
        return this;
    }

    public Float getNextcladeQcStopCodonsScoreFrom() {
        return nextcladeQcStopCodonsScoreFrom;
    }

    public BackgroundFilter setNextcladeQcStopCodonsScoreFrom(
        Float nextcladeQcStopCodonsScoreFrom) {
        this.nextcladeQcStopCodonsScoreFrom = nextcladeQcStopCodonsScoreFrom;
        return this;
    }

    public Float getNextcladeQcStopCodonsScoreTo() {
        return nextcladeQcStopCodonsScoreTo;
    }

    public BackgroundFilter setNextcladeQcStopCodonsScoreTo(
        Float nextcladeQcStopCodonsScoreTo) {
        this.nextcladeQcStopCodonsScoreTo = nextcladeQcStopCodonsScoreTo;
        return this;
    }

    public Float getNextcladeCoverageFrom() {
        return nextcladeCoverageFrom;
    }

    public BackgroundFilter setNextcladeCoverageFrom(Float nextcladeCoverageFrom) {
        this.nextcladeCoverageFrom = nextcladeCoverageFrom;
        return this;
    }

    public Float getNextcladeCoverageTo() {
        return nextcladeCoverageTo;
    }

    public BackgroundFilter setNextcladeCoverageTo(Float nextcladeCoverageTo) {
        this.nextcladeCoverageTo = nextcladeCoverageTo;
        return this;
    }
}
