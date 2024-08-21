package org.genspectrum.lapis.config

import java.util.Locale

typealias SequenceFilterFieldName = String
typealias LowercaseName = String

const val SARS_COV2_VARIANT_QUERY_FEATURE = "sarsCoV2VariantQuery"

val FEATURES_FOR_SEQUENCE_FILTERS = listOf(
    SARS_COV2_VARIANT_QUERY_FEATURE,
)

data class SequenceFilterFields(val fields: Map<LowercaseName, SequenceFilterField>) {
    companion object {
        fun fromDatabaseConfig(databaseConfig: DatabaseConfig): SequenceFilterFields {
            val metadataFields = databaseConfig.schema.metadata
                .flatMap(::mapToSequenceFilterField)
                .associateBy { it.name.lowercase(Locale.US) }

            val featuresFields = if (databaseConfig.schema.features.isEmpty()) {
                emptyMap()
            } else {
                databaseConfig.schema.features
                    .filter { it.name in FEATURES_FOR_SEQUENCE_FILTERS }
                    .associate(::mapToSequenceFilterFieldsFromFeatures)
            }

            return SequenceFilterFields(fields = metadataFields + featuresFields)
        }
    }
}

data class SequenceFilterField(
    val name: SequenceFilterFieldName,
    val type: SequenceFilterFieldType,
)

private fun mapToSequenceFilterField(databaseMetadata: DatabaseMetadata) =
    when (databaseMetadata.type) {
        MetadataType.STRING -> listOf(
            SequenceFilterField(
                name = databaseMetadata.name,
                type = SequenceFilterFieldType.String,
            ),
        ).let {
            when (databaseMetadata.lapisAllowsRegexSearch) {
                true -> it + SequenceFilterField(
                    name = "${databaseMetadata.name}\$regex",
                    type = SequenceFilterFieldType.StringSearch(databaseMetadata.name),
                )

                false -> it
            }
        }

        MetadataType.PANGO_LINEAGE -> listOf(
            SequenceFilterField(
                name = databaseMetadata.name,
                type = SequenceFilterFieldType.PangoLineage,
            ),
        )

        MetadataType.DATE -> listOf(
            SequenceFilterField(name = databaseMetadata.name, type = SequenceFilterFieldType.Date),
            SequenceFilterField(
                name = "${databaseMetadata.name}From",
                type = SequenceFilterFieldType.DateFrom(databaseMetadata.name),
            ),
            SequenceFilterField(
                name = "${databaseMetadata.name}To",
                type = SequenceFilterFieldType.DateTo(databaseMetadata.name),
            ),
        )

        MetadataType.INT -> listOf(
            SequenceFilterField(name = databaseMetadata.name, type = SequenceFilterFieldType.Int),
            SequenceFilterField(
                name = "${databaseMetadata.name}From",
                type = SequenceFilterFieldType.IntFrom(databaseMetadata.name),
            ),
            SequenceFilterField(
                name = "${databaseMetadata.name}To",
                type = SequenceFilterFieldType.IntTo(databaseMetadata.name),
            ),
        )

        MetadataType.FLOAT -> listOf(
            SequenceFilterField(name = databaseMetadata.name, type = SequenceFilterFieldType.Float),
            SequenceFilterField(
                name = "${databaseMetadata.name}From",
                type = SequenceFilterFieldType.FloatFrom(databaseMetadata.name),
            ),
            SequenceFilterField(
                name = "${databaseMetadata.name}To",
                type = SequenceFilterFieldType.FloatTo(databaseMetadata.name),
            ),
        )

        MetadataType.BOOLEAN -> listOf(
            SequenceFilterField(
                name = databaseMetadata.name,
                type = SequenceFilterFieldType.Boolean,
            ),
        )
    }

private const val VARIANT_QUERY_FIELD = "variantQuery"

private fun mapToSequenceFilterFieldsFromFeatures(databaseFeature: DatabaseFeature) =
    when (databaseFeature.name) {
        SARS_COV2_VARIANT_QUERY_FEATURE -> VARIANT_QUERY_FIELD.lowercase(Locale.US) to SequenceFilterField(
            name = VARIANT_QUERY_FIELD,
            type = SequenceFilterFieldType.VariantQuery,
        )

        else -> throw IllegalArgumentException(
            "Unknown feature '${databaseFeature.name}'",
        )
    }

sealed class SequenceFilterFieldType(val openApiType: kotlin.String) {
    data object String : SequenceFilterFieldType("string")

    data object PangoLineage : SequenceFilterFieldType("string")

    data object Date : SequenceFilterFieldType("string")

    data object Boolean : SequenceFilterFieldType("boolean")

    data object VariantQuery : SequenceFilterFieldType("string")

    data class DateFrom(val associatedField: SequenceFilterFieldName) : SequenceFilterFieldType("string")

    data class DateTo(val associatedField: SequenceFilterFieldName) : SequenceFilterFieldType("string")

    data object Int : SequenceFilterFieldType("integer")

    data class IntFrom(val associatedField: SequenceFilterFieldName) : SequenceFilterFieldType("integer")

    data class IntTo(val associatedField: SequenceFilterFieldName) : SequenceFilterFieldType("integer")

    data object Float : SequenceFilterFieldType("number")

    data class FloatFrom(val associatedField: SequenceFilterFieldName) : SequenceFilterFieldType("number")

    data class FloatTo(val associatedField: SequenceFilterFieldName) : SequenceFilterFieldType("number")

    data class StringSearch(val associatedField: SequenceFilterFieldName) : SequenceFilterFieldType("string")
}
