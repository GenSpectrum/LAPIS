package org.genspectrum.lapis.config

data class DatabaseConfig(val schema: DatabaseSchema)

data class DatabaseSchema(
    val instanceName: String,
    val opennessLevel: OpennessLevel,
    val metadata: List<DatabaseMetadata>,
    val primaryKey: String,
    val features: List<DatabaseFeature> = emptyList(),
)

data class DatabaseMetadata(val name: String, val type: String, val valuesAreUnique: Boolean = false)

data class DatabaseFeature(val name: String)

enum class OpennessLevel {
    /**
     * The data served by this instance is fully open and may be shared with anyone.
     */
    OPEN,

    /**
     * The data served by this instance must not be disclosed to everyone.
     *
     * Two access keys can be configured:
     *
     * One access key permits access to aggregated data. The aggregated data must be such that one cannot deduce
     * information about the data of the individual sequences.
     *
     * The other access key permits access to the full data.
     */
    PROTECTED,
}
