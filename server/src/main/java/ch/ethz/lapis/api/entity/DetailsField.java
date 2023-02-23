package ch.ethz.lapis.api.entity;

import ch.ethz.lapis.api.entity.res.SampleDetail;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.lapis.tables.YMainMetadata;
import org.jooq.lapis.tables.records.YMainMetadataRecord;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.jooq.lapis.tables.YMainMetadata.*;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DetailsField<FieldType> {
    public static final Map<SampleDetail.Fields, DetailsField<?>> FIELD_NAME_TO_DATABASE_COLUMN = new HashMap<>() {{
        put(
            SampleDetail.Fields.genbankAccession,
            new DetailsField<>(Y_MAIN_METADATA.GENBANK_ACCESSION, SampleDetail::setGenbankAccession)
        );
        put(
            SampleDetail.Fields.sraAccession,
            new DetailsField<>(Y_MAIN_METADATA.SRA_ACCESSION, SampleDetail::setSraAccession)
        );
        put(
            SampleDetail.Fields.gisaidEpiIsl,
            new DetailsField<>(Y_MAIN_METADATA.GISAID_EPI_ISL, SampleDetail::setGisaidEpiIsl)
        );
        put(
            SampleDetail.Fields.strain,
            new DetailsField<>(Y_MAIN_METADATA.STRAIN, SampleDetail::setStrain)
        );
        put(
            SampleDetail.Fields.date,
            new DetailsField<>(Y_MAIN_METADATA.DATE, SampleDetail::setDate)
        );
        put(
            SampleDetail.Fields.year,
            new DetailsField<>(Y_MAIN_METADATA.YEAR, SampleDetail::setYear)
        );
        put(
            SampleDetail.Fields.month,
            new DetailsField<>(Y_MAIN_METADATA.MONTH, SampleDetail::setMonth)
        );
        put(
            SampleDetail.Fields.dateSubmitted,
            new DetailsField<>(Y_MAIN_METADATA.DATE_SUBMITTED, SampleDetail::setDateSubmitted)
        );
        put(
            SampleDetail.Fields.region,
            new DetailsField<>(Y_MAIN_METADATA.REGION, SampleDetail::setRegion)
        );
        put(
            SampleDetail.Fields.country,
            new DetailsField<>(Y_MAIN_METADATA.COUNTRY, SampleDetail::setCountry)
        );
        put(
            SampleDetail.Fields.division,
            new DetailsField<>(Y_MAIN_METADATA.DIVISION, SampleDetail::setDivision)
        );
        put(
            SampleDetail.Fields.location,
            new DetailsField<>(Y_MAIN_METADATA.LOCATION, SampleDetail::setLocation)
        );
        put(
            SampleDetail.Fields.regionExposure,
            new DetailsField<>(Y_MAIN_METADATA.REGION_EXPOSURE, SampleDetail::setRegionExposure)
        );
        put(
            SampleDetail.Fields.countryExposure,
            new DetailsField<>(Y_MAIN_METADATA.COUNTRY_EXPOSURE, SampleDetail::setCountryExposure)
        );
        put(
            SampleDetail.Fields.divisionExposure,
            new DetailsField<>(Y_MAIN_METADATA.DIVISION_EXPOSURE, SampleDetail::setDivisionExposure)
        );
        put(
            SampleDetail.Fields.age,
            new DetailsField<>(Y_MAIN_METADATA.AGE, SampleDetail::setAge)
        );
        put(
            SampleDetail.Fields.sex,
            new DetailsField<>(Y_MAIN_METADATA.SEX, SampleDetail::setSex)
        );
        put(
            SampleDetail.Fields.hospitalized,
            new DetailsField<>(Y_MAIN_METADATA.HOSPITALIZED, SampleDetail::setHospitalized)
        );
        put(
            SampleDetail.Fields.died,
            new DetailsField<>(Y_MAIN_METADATA.DIED, SampleDetail::setDied)
        );
        put(
            SampleDetail.Fields.fullyVaccinated,
            new DetailsField<>(Y_MAIN_METADATA.FULLY_VACCINATED, SampleDetail::setFullyVaccinated)
        );
        put(
            SampleDetail.Fields.host,
            new DetailsField<>(Y_MAIN_METADATA.HOST, SampleDetail::setHost)
        );
        put(
            SampleDetail.Fields.samplingStrategy,
            new DetailsField<>(Y_MAIN_METADATA.SAMPLING_STRATEGY, SampleDetail::setSamplingStrategy)
        );
        put(
            SampleDetail.Fields.pangoLineage,
            new DetailsField<>(Y_MAIN_METADATA.PANGO_LINEAGE, SampleDetail::setPangoLineage)
        );
        put(
            SampleDetail.Fields.nextcladePangoLineage,
            new DetailsField<>(Y_MAIN_METADATA.NEXTCLADE_PANGO_LINEAGE, SampleDetail::setNextcladePangoLineage)
        );
        put(
            SampleDetail.Fields.nextstrainClade,
            new DetailsField<>(Y_MAIN_METADATA.NEXTSTRAIN_CLADE, SampleDetail::setNextstrainClade)
        );
        put(
            SampleDetail.Fields.gisaidCloade,
            new DetailsField<>(Y_MAIN_METADATA.GISAID_CLADE, SampleDetail::setGisaidCloade)
        );
        put(
            SampleDetail.Fields.submittingLab,
            new DetailsField<>(Y_MAIN_METADATA.SUBMITTING_LAB, SampleDetail::setSubmittingLab)
        );
        put(
            SampleDetail.Fields.originatingLab,
            new DetailsField<>(Y_MAIN_METADATA.ORIGINATING_LAB, SampleDetail::setOriginatingLab)
        );
        put(
            SampleDetail.Fields.database,
            new DetailsField<>(Y_MAIN_METADATA.DATABASE, SampleDetail::setDatabase)
        );
    }};

    public final TableField<YMainMetadataRecord, FieldType> databaseColumnName;
    private final BiFunction<SampleDetail, FieldType, SampleDetail> sampleDetailSetter;

    public void setDataFromRecord(SampleDetail detail, Record record) {
        sampleDetailSetter.apply(detail, record.get(databaseColumnName));
    }

    public static Collection<DetailsField<?>> getDetails(List<SampleDetail.Fields> filter) {
        if (filter.isEmpty()) {
            return FIELD_NAME_TO_DATABASE_COLUMN.values();
        }

        Stream<DetailsField<?>> detailsFieldStream = FIELD_NAME_TO_DATABASE_COLUMN.entrySet().stream()
            .filter(detailsFieldsDetailsFieldEntry -> filter.contains(detailsFieldsDetailsFieldEntry.getKey()))
            .map(Map.Entry::getValue);
        return detailsFieldStream.toList();

    }
}
