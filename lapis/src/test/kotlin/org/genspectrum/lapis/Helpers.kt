package org.genspectrum.lapis

import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.DatabaseFeature
import org.genspectrum.lapis.config.DatabaseMetadata
import org.genspectrum.lapis.config.DatabaseSchema
import org.genspectrum.lapis.config.OpennessLevel

fun databaseConfig(
    primaryKey: String,
    metadata: List<DatabaseMetadata>,
    databaseFeatures: List<DatabaseFeature> = emptyList(),
) = DatabaseConfig(
    schema = DatabaseSchema(
        instanceName = "test",
        opennessLevel = OpennessLevel.OPEN,
        metadata = metadata,
        primaryKey = primaryKey,
        features = databaseFeatures,
    ),
)
