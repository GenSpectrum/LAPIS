package org.genspectrum.lapis.config

data class DatabaseConfig(val schema: DatabaseSchema)

data class DatabaseSchema(
    val instanceName: String,
    val opennessLevel: OpennessLevel,
    val metadata: List<DatabaseMetadata>,
    val primaryKey: String,
    val features: List<DatabaseFeature> = emptyList(),
)

data class DatabaseMetadata(val name: String, val type: String, val unique: Boolean = false)

data class DatabaseFeature(val name: String)

enum class OpennessLevel {
    OPEN,
    GISAID,
}
