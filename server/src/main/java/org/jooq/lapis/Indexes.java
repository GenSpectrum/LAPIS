/*
 * This file is generated by jOOQ.
 */
package org.jooq.lapis;


import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;
import org.jooq.lapis.tables.YMainMetadata;
import org.jooq.lapis.tables.YMainMetadataStaging;


/**
 * A class modelling indexes of tables in the default schema.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Indexes {

    // -------------------------------------------------------------------------
    // INDEX definitions
    // -------------------------------------------------------------------------

    public static final Index Y_MAIN_METADATA_AGE_IDX = Internal.createIndex(DSL.name("y_main_metadata_age_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.AGE }, false);
    public static final Index Y_MAIN_METADATA_COUNTRY_EXPOSURE_IDX = Internal.createIndex(DSL.name("y_main_metadata_country_exposure_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.COUNTRY_EXPOSURE }, false);
    public static final Index Y_MAIN_METADATA_COUNTRY_IDX = Internal.createIndex(DSL.name("y_main_metadata_country_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.COUNTRY }, false);
    public static final Index Y_MAIN_METADATA_DATE_IDX = Internal.createIndex(DSL.name("y_main_metadata_date_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.DATE }, false);
    public static final Index Y_MAIN_METADATA_DATE_SUBMITTED_IDX = Internal.createIndex(DSL.name("y_main_metadata_date_submitted_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.DATE_SUBMITTED }, false);
    public static final Index Y_MAIN_METADATA_DIED_IDX = Internal.createIndex(DSL.name("y_main_metadata_died_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.DIED }, false);
    public static final Index Y_MAIN_METADATA_DIVISION_EXPOSURE_IDX = Internal.createIndex(DSL.name("y_main_metadata_division_exposure_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.DIVISION_EXPOSURE }, false);
    public static final Index Y_MAIN_METADATA_DIVISION_IDX = Internal.createIndex(DSL.name("y_main_metadata_division_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.DIVISION }, false);
    public static final Index Y_MAIN_METADATA_FULLY_VACCINATED_IDX = Internal.createIndex(DSL.name("y_main_metadata_fully_vaccinated_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.FULLY_VACCINATED }, false);
    public static final Index Y_MAIN_METADATA_GENBANK_ACCESSION_IDX = Internal.createIndex(DSL.name("y_main_metadata_genbank_accession_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.GENBANK_ACCESSION }, false);
    public static final Index Y_MAIN_METADATA_GISAID_CLADE_IDX = Internal.createIndex(DSL.name("y_main_metadata_gisaid_clade_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.GISAID_CLADE }, false);
    public static final Index Y_MAIN_METADATA_GISAID_EPI_ISL_IDX = Internal.createIndex(DSL.name("y_main_metadata_gisaid_epi_isl_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.GISAID_EPI_ISL }, false);
    public static final Index Y_MAIN_METADATA_HOSPITALIZED_IDX = Internal.createIndex(DSL.name("y_main_metadata_hospitalized_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.HOSPITALIZED }, false);
    public static final Index Y_MAIN_METADATA_HOST_IDX = Internal.createIndex(DSL.name("y_main_metadata_host_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.HOST }, false);
    public static final Index Y_MAIN_METADATA_NEXTCLADE_PANGO_LINEAGE_IDX = Internal.createIndex(DSL.name("y_main_metadata_nextclade_pango_lineage_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.NEXTCLADE_PANGO_LINEAGE }, false);
    public static final Index Y_MAIN_METADATA_NEXTSTRAIN_CLADE_IDX = Internal.createIndex(DSL.name("y_main_metadata_nextstrain_clade_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.NEXTSTRAIN_CLADE }, false);
    public static final Index Y_MAIN_METADATA_ORIGINATING_LAB_IDX = Internal.createIndex(DSL.name("y_main_metadata_originating_lab_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.ORIGINATING_LAB }, false);
    public static final Index Y_MAIN_METADATA_PANGO_LINEAGE_IDX = Internal.createIndex(DSL.name("y_main_metadata_pango_lineage_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.PANGO_LINEAGE }, false);
    public static final Index Y_MAIN_METADATA_REGION_EXPOSURE_IDX = Internal.createIndex(DSL.name("y_main_metadata_region_exposure_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.REGION_EXPOSURE }, false);
    public static final Index Y_MAIN_METADATA_REGION_IDX = Internal.createIndex(DSL.name("y_main_metadata_region_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.REGION }, false);
    public static final Index Y_MAIN_METADATA_SAMPLING_STRATEGY_IDX = Internal.createIndex(DSL.name("y_main_metadata_sampling_strategy_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.SAMPLING_STRATEGY }, false);
    public static final Index Y_MAIN_METADATA_SEX_IDX = Internal.createIndex(DSL.name("y_main_metadata_sex_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.SEX }, false);
    public static final Index Y_MAIN_METADATA_SRA_ACCESSION_IDX = Internal.createIndex(DSL.name("y_main_metadata_sra_accession_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.SRA_ACCESSION }, false);
    public static final Index Y_MAIN_METADATA_STAGING_AGE_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_age_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.AGE }, false);
    public static final Index Y_MAIN_METADATA_STAGING_COUNTRY_EXPOSURE_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_country_exposure_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.COUNTRY_EXPOSURE }, false);
    public static final Index Y_MAIN_METADATA_STAGING_COUNTRY_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_country_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.COUNTRY }, false);
    public static final Index Y_MAIN_METADATA_STAGING_DATE_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_date_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.DATE }, false);
    public static final Index Y_MAIN_METADATA_STAGING_DATE_SUBMITTED_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_date_submitted_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.DATE_SUBMITTED }, false);
    public static final Index Y_MAIN_METADATA_STAGING_DIED_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_died_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.DIED }, false);
    public static final Index Y_MAIN_METADATA_STAGING_DIVISION_EXPOSURE_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_division_exposure_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.DIVISION_EXPOSURE }, false);
    public static final Index Y_MAIN_METADATA_STAGING_DIVISION_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_division_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.DIVISION }, false);
    public static final Index Y_MAIN_METADATA_STAGING_FULLY_VACCINATED_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_fully_vaccinated_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.FULLY_VACCINATED }, false);
    public static final Index Y_MAIN_METADATA_STAGING_GENBANK_ACCESSION_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_genbank_accession_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.GENBANK_ACCESSION }, false);
    public static final Index Y_MAIN_METADATA_STAGING_GISAID_CLADE_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_gisaid_clade_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.GISAID_CLADE }, false);
    public static final Index Y_MAIN_METADATA_STAGING_GISAID_EPI_ISL_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_gisaid_epi_isl_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.GISAID_EPI_ISL }, false);
    public static final Index Y_MAIN_METADATA_STAGING_HOSPITALIZED_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_hospitalized_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.HOSPITALIZED }, false);
    public static final Index Y_MAIN_METADATA_STAGING_HOST_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_host_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.HOST }, false);
    public static final Index Y_MAIN_METADATA_STAGING_NEXTCLADE_PANGO_LINEAGE_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_nextclade_pango_lineage_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.NEXTCLADE_PANGO_LINEAGE }, false);
    public static final Index Y_MAIN_METADATA_STAGING_NEXTSTRAIN_CLADE_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_nextstrain_clade_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.NEXTSTRAIN_CLADE }, false);
    public static final Index Y_MAIN_METADATA_STAGING_ORIGINATING_LAB_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_originating_lab_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.ORIGINATING_LAB }, false);
    public static final Index Y_MAIN_METADATA_STAGING_PANGO_LINEAGE_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_pango_lineage_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.PANGO_LINEAGE }, false);
    public static final Index Y_MAIN_METADATA_STAGING_REGION_EXPOSURE_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_region_exposure_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.REGION_EXPOSURE }, false);
    public static final Index Y_MAIN_METADATA_STAGING_REGION_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_region_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.REGION }, false);
    public static final Index Y_MAIN_METADATA_STAGING_SAMPLING_STRATEGY_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_sampling_strategy_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.SAMPLING_STRATEGY }, false);
    public static final Index Y_MAIN_METADATA_STAGING_SEX_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_sex_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.SEX }, false);
    public static final Index Y_MAIN_METADATA_STAGING_SRA_ACCESSION_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_sra_accession_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.SRA_ACCESSION }, false);
    public static final Index Y_MAIN_METADATA_STAGING_STRAIN_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_strain_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.STRAIN }, false);
    public static final Index Y_MAIN_METADATA_STAGING_SUBMITTING_LAB_IDX = Internal.createIndex(DSL.name("y_main_metadata_staging_submitting_lab_idx"), YMainMetadataStaging.Y_MAIN_METADATA_STAGING, new OrderField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.SUBMITTING_LAB }, false);
    public static final Index Y_MAIN_METADATA_STRAIN_IDX = Internal.createIndex(DSL.name("y_main_metadata_strain_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.STRAIN }, false);
    public static final Index Y_MAIN_METADATA_SUBMITTING_LAB_IDX = Internal.createIndex(DSL.name("y_main_metadata_submitting_lab_idx"), YMainMetadata.Y_MAIN_METADATA, new OrderField[] { YMainMetadata.Y_MAIN_METADATA.SUBMITTING_LAB }, false);
}
