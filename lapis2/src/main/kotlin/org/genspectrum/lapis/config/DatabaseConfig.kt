package org.genspectrum.lapis.config

data class DatabaseConfig(val schema: DatabaseSchema)

data class DatabaseSchema(val instanceName: String, val metadata: List<DatabaseMetadata>, val primaryKey: String)

data class DatabaseMetadata(val name: String, val type: String)
