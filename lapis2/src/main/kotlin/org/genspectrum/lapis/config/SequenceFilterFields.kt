package org.genspectrum.lapis.config

typealias SequenceFilterFieldName = String

data class SequenceFilterFields(val fields: Map<SequenceFilterFieldName, SequenceFilterFieldType>) {
    companion object {
        private val nucleotideMutationsField = Pair("nucleotideMutations", SequenceFilterFieldType.MutationsList)

        fun fromDatabaseConfig(databaseConfig: DatabaseConfig): SequenceFilterFields {
            val metadataFields = databaseConfig.schema.metadata
                .map(::mapToSequenceFilterFields)
                .flatten()
                .toMap()
            val staticFields = listOf(nucleotideMutationsField)

            val featuresFields = if (databaseConfig.schema.features.isEmpty()) {
                emptyMap<SequenceFilterFieldName, SequenceFilterFieldType>()
            } else {
                databaseConfig.schema.features.associate(::mapToSequenceFilterFieldsFromFeatures)
            }

            return SequenceFilterFields(fields = metadataFields + staticFields + featuresFields)
        }
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
    "int" -> listOf(
        databaseMetadata.name to SequenceFilterFieldType.Int,
        "${databaseMetadata.name}From" to SequenceFilterFieldType.IntFrom(databaseMetadata.name),
        "${databaseMetadata.name}To" to SequenceFilterFieldType.IntTo(databaseMetadata.name),
    )
    "float" -> listOf(
        databaseMetadata.name to SequenceFilterFieldType.Float,
        "${databaseMetadata.name}From" to SequenceFilterFieldType.FloatFrom(databaseMetadata.name),
        "${databaseMetadata.name}To" to SequenceFilterFieldType.FloatTo(databaseMetadata.name),
    )

    else -> throw IllegalArgumentException(
        "Unknown field type '${databaseMetadata.type}' for field '${databaseMetadata.name}'",
    )
}

private fun mapToSequenceFilterFieldsFromFeatures(databaseFeature: DatabaseFeature) = when (databaseFeature.name) {
    "sarsCoV2VariantQuery" -> "variantQuery" to SequenceFilterFieldType.VariantQuery
    else -> throw IllegalArgumentException(
        "Unknown feature '${databaseFeature.name}'",
    )
}

sealed class SequenceFilterFieldType(val openApiType: kotlin.String) {
    object String : SequenceFilterFieldType("string")
    object PangoLineage : SequenceFilterFieldType("string")
    object Date : SequenceFilterFieldType("string")
    object MutationsList : SequenceFilterFieldType("string")
    object VariantQuery : SequenceFilterFieldType("string")
    data class DateFrom(val associatedField: SequenceFilterFieldName) : SequenceFilterFieldType("string")
    data class DateTo(val associatedField: SequenceFilterFieldName) : SequenceFilterFieldType("string")
    object Int : SequenceFilterFieldType("integer")
    data class IntFrom(val associatedField: SequenceFilterFieldName) : SequenceFilterFieldType("integer")
    data class IntTo(val associatedField: SequenceFilterFieldName) : SequenceFilterFieldType("integer")
    object Float : SequenceFilterFieldType("number")
    data class FloatFrom(val associatedField: SequenceFilterFieldName) : SequenceFilterFieldType("number")
    data class FloatTo(val associatedField: SequenceFilterFieldName) : SequenceFilterFieldType("number")
}
