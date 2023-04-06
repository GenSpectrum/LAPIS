package org.genspectrum.lapis.config

data class SequenceFilterFields(val fields: List<SequenceFilterField>) {
    companion object {
        fun fromDatabaseConfig(databaseConfig: DatabaseConfig) = SequenceFilterFields(
            fields = databaseConfig.schema.metadata.map(::mapToSequenceFilterFields).flatten(),
        )
    }
}

private fun mapToSequenceFilterFields(databaseMetadata: DatabaseMetadata) = when (databaseMetadata.type) {
    "string" -> listOf(SequenceFilterField(databaseMetadata.name, SequenceFilterField.Type.String))
    "pango_lineage" -> listOf(SequenceFilterField(databaseMetadata.name, SequenceFilterField.Type.PangoLineage))
    "date" -> listOf(
        SequenceFilterField(databaseMetadata.name, SequenceFilterField.Type.Date),
        SequenceFilterField("${databaseMetadata.name}From", SequenceFilterField.Type.DateFrom(databaseMetadata.name)),
        SequenceFilterField("${databaseMetadata.name}To", SequenceFilterField.Type.DateTo(databaseMetadata.name)),
    )
    else -> throw IllegalArgumentException(
        "Unknown field type '${databaseMetadata.type}' for field '${databaseMetadata.name}'",
    )
}

data class SequenceFilterField(val name: String, val type: Type) {
    sealed class Type(val openApiType: kotlin.String) {
        object String : Type("string")
        object PangoLineage : Type("string")
        object Date : Type("string")
        data class DateFrom(val associatedField: kotlin.String) : Type("string")
        data class DateTo(val associatedField: kotlin.String) : Type("string")
    }
}
