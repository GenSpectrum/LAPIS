/*
 * This file is generated by jOOQ.
 */
package org.jooq.lapis;


import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;
import org.jooq.lapis.tables.YMainAaSequence;
import org.jooq.lapis.tables.YMainAaSequenceColumnar;
import org.jooq.lapis.tables.YMainAaSequenceColumnarStaging;
import org.jooq.lapis.tables.YMainAaSequenceStaging;
import org.jooq.lapis.tables.YMainMetadata;
import org.jooq.lapis.tables.YMainMetadataStaging;
import org.jooq.lapis.tables.YMainSequence;
import org.jooq.lapis.tables.YMainSequenceColumnar;
import org.jooq.lapis.tables.YMainSequenceColumnarStaging;
import org.jooq.lapis.tables.YMainSequenceStaging;
import org.jooq.lapis.tables.YNextstrainGenbank;
import org.jooq.lapis.tables.YPangolinAssignment;
import org.jooq.lapis.tables.YS3c;
import org.jooq.lapis.tables.YTree;
import org.jooq.lapis.tables.records.YMainAaSequenceColumnarRecord;
import org.jooq.lapis.tables.records.YMainAaSequenceColumnarStagingRecord;
import org.jooq.lapis.tables.records.YMainAaSequenceRecord;
import org.jooq.lapis.tables.records.YMainAaSequenceStagingRecord;
import org.jooq.lapis.tables.records.YMainMetadataRecord;
import org.jooq.lapis.tables.records.YMainMetadataStagingRecord;
import org.jooq.lapis.tables.records.YMainSequenceColumnarRecord;
import org.jooq.lapis.tables.records.YMainSequenceColumnarStagingRecord;
import org.jooq.lapis.tables.records.YMainSequenceRecord;
import org.jooq.lapis.tables.records.YMainSequenceStagingRecord;
import org.jooq.lapis.tables.records.YNextstrainGenbankRecord;
import org.jooq.lapis.tables.records.YPangolinAssignmentRecord;
import org.jooq.lapis.tables.records.YS3cRecord;
import org.jooq.lapis.tables.records.YTreeRecord;


/**
 * A class modelling foreign key relationships and constraints of tables in
 * the default schema.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<YMainAaSequenceRecord> Y_MAIN_AA_SEQUENCE_PKEY = Internal.createUniqueKey(YMainAaSequence.Y_MAIN_AA_SEQUENCE, DSL.name("y_main_aa_sequence_pkey"), new TableField[] { YMainAaSequence.Y_MAIN_AA_SEQUENCE.ID, YMainAaSequence.Y_MAIN_AA_SEQUENCE.GENE }, true);
    public static final UniqueKey<YMainAaSequenceColumnarRecord> Y_MAIN_AA_SEQUENCE_COLUMNAR_PKEY = Internal.createUniqueKey(YMainAaSequenceColumnar.Y_MAIN_AA_SEQUENCE_COLUMNAR, DSL.name("y_main_aa_sequence_columnar_pkey"), new TableField[] { YMainAaSequenceColumnar.Y_MAIN_AA_SEQUENCE_COLUMNAR.GENE, YMainAaSequenceColumnar.Y_MAIN_AA_SEQUENCE_COLUMNAR.POSITION }, true);
    public static final UniqueKey<YMainAaSequenceColumnarStagingRecord> Y_MAIN_AA_SEQUENCE_COLUMNAR_STAGING_PKEY = Internal.createUniqueKey(YMainAaSequenceColumnarStaging.Y_MAIN_AA_SEQUENCE_COLUMNAR_STAGING, DSL.name("y_main_aa_sequence_columnar_staging_pkey"), new TableField[] { YMainAaSequenceColumnarStaging.Y_MAIN_AA_SEQUENCE_COLUMNAR_STAGING.GENE, YMainAaSequenceColumnarStaging.Y_MAIN_AA_SEQUENCE_COLUMNAR_STAGING.POSITION }, true);
    public static final UniqueKey<YMainAaSequenceStagingRecord> Y_MAIN_AA_SEQUENCE_STAGING_PKEY = Internal.createUniqueKey(YMainAaSequenceStaging.Y_MAIN_AA_SEQUENCE_STAGING, DSL.name("y_main_aa_sequence_staging_pkey"), new TableField[] { YMainAaSequenceStaging.Y_MAIN_AA_SEQUENCE_STAGING.ID, YMainAaSequenceStaging.Y_MAIN_AA_SEQUENCE_STAGING.GENE }, true);
    public static final UniqueKey<YMainMetadataRecord> Y_MAIN_METADATA_PKEY = Internal.createUniqueKey(YMainMetadata.Y_MAIN_METADATA, DSL.name("y_main_metadata_pkey"), new TableField[] { YMainMetadata.Y_MAIN_METADATA.ID }, true);
    public static final UniqueKey<YMainMetadataStagingRecord> Y_MAIN_METADATA_STAGING_PKEY = Internal.createUniqueKey(YMainMetadataStaging.Y_MAIN_METADATA_STAGING, DSL.name("y_main_metadata_staging_pkey"), new TableField[] { YMainMetadataStaging.Y_MAIN_METADATA_STAGING.ID }, true);
    public static final UniqueKey<YMainSequenceRecord> Y_MAIN_SEQUENCE_PKEY = Internal.createUniqueKey(YMainSequence.Y_MAIN_SEQUENCE, DSL.name("y_main_sequence_pkey"), new TableField[] { YMainSequence.Y_MAIN_SEQUENCE.ID }, true);
    public static final UniqueKey<YMainSequenceColumnarRecord> Y_MAIN_SEQUENCE_COLUMNAR_PKEY = Internal.createUniqueKey(YMainSequenceColumnar.Y_MAIN_SEQUENCE_COLUMNAR, DSL.name("y_main_sequence_columnar_pkey"), new TableField[] { YMainSequenceColumnar.Y_MAIN_SEQUENCE_COLUMNAR.POSITION }, true);
    public static final UniqueKey<YMainSequenceColumnarStagingRecord> Y_MAIN_SEQUENCE_COLUMNAR_STAGING_PKEY = Internal.createUniqueKey(YMainSequenceColumnarStaging.Y_MAIN_SEQUENCE_COLUMNAR_STAGING, DSL.name("y_main_sequence_columnar_staging_pkey"), new TableField[] { YMainSequenceColumnarStaging.Y_MAIN_SEQUENCE_COLUMNAR_STAGING.POSITION }, true);
    public static final UniqueKey<YMainSequenceStagingRecord> Y_MAIN_SEQUENCE_STAGING_PKEY = Internal.createUniqueKey(YMainSequenceStaging.Y_MAIN_SEQUENCE_STAGING, DSL.name("y_main_sequence_staging_pkey"), new TableField[] { YMainSequenceStaging.Y_MAIN_SEQUENCE_STAGING.ID }, true);
    public static final UniqueKey<YNextstrainGenbankRecord> Y_NEXTSTRAIN_GENBANK_PKEY = Internal.createUniqueKey(YNextstrainGenbank.Y_NEXTSTRAIN_GENBANK, DSL.name("y_nextstrain_genbank_pkey"), new TableField[] { YNextstrainGenbank.Y_NEXTSTRAIN_GENBANK.STRAIN }, true);
    public static final UniqueKey<YPangolinAssignmentRecord> Y_PANGOLIN_ASSIGNMENT_PKEY = Internal.createUniqueKey(YPangolinAssignment.Y_PANGOLIN_ASSIGNMENT, DSL.name("y_pangolin_assignment_pkey"), new TableField[] { YPangolinAssignment.Y_PANGOLIN_ASSIGNMENT.GISAID_EPI_ISL }, true);
    public static final UniqueKey<YS3cRecord> Y_S3C_GISAID_EPI_ISL_KEY = Internal.createUniqueKey(YS3c.Y_S3C, DSL.name("y_s3c_gisaid_epi_isl_key"), new TableField[] { YS3c.Y_S3C.GISAID_EPI_ISL }, true);
    public static final UniqueKey<YS3cRecord> Y_S3C_SRA_ACCESSION_KEY = Internal.createUniqueKey(YS3c.Y_S3C, DSL.name("y_s3c_sra_accession_key"), new TableField[] { YS3c.Y_S3C.SRA_ACCESSION }, true);
    public static final UniqueKey<YTreeRecord> Y_TREE_PKEY = Internal.createUniqueKey(YTree.Y_TREE, DSL.name("y_tree_pkey"), new TableField[] { YTree.Y_TREE.TIMESTAMP }, true);
}
