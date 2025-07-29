package org.genspectrum.lapis

import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.DatabaseFeature
import org.genspectrum.lapis.config.DatabaseMetadata
import org.genspectrum.lapis.config.DatabaseSchema
import org.genspectrum.lapis.config.GENERALIZED_ADVANCED_QUERY_FEATURE
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.config.OpennessLevel
import org.genspectrum.lapis.config.SARS_COV2_VARIANT_QUERY_FEATURE

val dummyDatabaseConfig = DatabaseConfig(
    DatabaseSchema(
        "test config",
        OpennessLevel.OPEN,
        listOf(
            DatabaseMetadata("treeKey", MetadataType.STRING, isPhyloTreeField = true),
            DatabaseMetadata(DATE_FIELD, MetadataType.DATE),
            DatabaseMetadata(PANGO_LINEAGE_FIELD, MetadataType.STRING, generateLineageIndex = true),
            DatabaseMetadata("some_metadata", MetadataType.STRING),
            DatabaseMetadata("other_metadata", MetadataType.STRING),
            DatabaseMetadata("floatField", MetadataType.FLOAT),
            DatabaseMetadata("intField", MetadataType.INT),
            DatabaseMetadata("test_boolean_column", MetadataType.BOOLEAN),
        ),
        "some_metadata",
        listOf(
            DatabaseFeature(name = SARS_COV2_VARIANT_QUERY_FEATURE),
            DatabaseFeature(name = GENERALIZED_ADVANCED_QUERY_FEATURE),
        ),
    ),
)
