package org.genspectrum.lapis.config

typealias FieldName = String

data class SequenceFilterFields(val fields: Map<FieldName, SequenceFilterFieldType>) {
    companion object {
        private val nucleotideMutationsField = Pair("nucleotideMutations", SequenceFilterFieldType.MutationsList)

        fun fromDatabaseConfig(databaseConfig: DatabaseConfig) = SequenceFilterFields(
            fields = databaseConfig.schema.metadata
                .map(::mapToSequenceFilterFields)
                .flatten()
                .toMap() + nucleotideMutationsField,
        )
    }
}

private fun mapToSequenceFilterFields(databaseMetadata: DatabaseMetadata) = when (databaseMetadata.type) {
    "string" -> listOf(databaseMetadata.name to SequenceFilterFieldType.String)
    "pango_lineage" -> listOf(databaseMetadata.name to SequenceFilterFieldType.PangoLineage)
    "date" -> listOf(
        databaseMetadata.name to SequenceFilterFieldType.Date,
        "${databaseMetadata.name}From" to SequenceFilterFieldType.DateFrom(databaseMetadata.name),
        "${databaseMetadata.name}To" to SequenceFilterFieldType.DateTo(databaseMetadata.name),
    )

    else -> throw IllegalArgumentException(
        "Unknown field type '${databaseMetadata.type}' for field '${databaseMetadata.name}'",
    )
}

sealed class SequenceFilterFieldType(val openApiType: kotlin.String) {
    object String : SequenceFilterFieldType("string")
    object PangoLineage : SequenceFilterFieldType("string")
    object Date : SequenceFilterFieldType("string")
    object MutationsList : SequenceFilterFieldType("string")
    data class DateFrom(val associatedField: kotlin.String) : SequenceFilterFieldType("string")
    data class DateTo(val associatedField: kotlin.String) : SequenceFilterFieldType("string")
}
