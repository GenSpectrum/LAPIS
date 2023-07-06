package org.genspectrum.lapis

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.config.OpennessLevel
import org.genspectrum.lapis.config.SequenceFilterFieldName
import org.genspectrum.lapis.config.SequenceFilterFields
import org.genspectrum.lapis.controller.AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION
import org.genspectrum.lapis.controller.AGGREGATED_REQUEST_SCHEMA
import org.genspectrum.lapis.controller.AGGREGATED_RESPONSE_SCHEMA
import org.genspectrum.lapis.controller.DETAILS_FIELDS_DESCRIPTION
import org.genspectrum.lapis.controller.DETAILS_REQUEST_SCHEMA
import org.genspectrum.lapis.controller.DETAILS_RESPONSE_SCHEMA
import org.genspectrum.lapis.controller.FIELDS_PROPERTY
import org.genspectrum.lapis.controller.MIN_PROPORTION_PROPERTY
import org.genspectrum.lapis.controller.REQUEST_SCHEMA_WITH_MIN_PROPORTION
import org.genspectrum.lapis.controller.SEQUENCE_FILTERS_SCHEMA
import org.genspectrum.lapis.response.COUNT_PROPERTY

fun buildOpenApiSchema(sequenceFilterFields: SequenceFilterFields, databaseConfig: DatabaseConfig): OpenAPI {
    val requestProperties = when (databaseConfig.schema.opennessLevel) {
        OpennessLevel.PROTECTED -> sequenceFilterFieldSchemas(sequenceFilterFields) + ("accessKey" to accessKeySchema())
        else -> sequenceFilterFieldSchemas(sequenceFilterFields)
    }

    return OpenAPI()
        .components(
            Components().addSchemas(
                SEQUENCE_FILTERS_SCHEMA,
                Schema<String>()
                    .type("object")
                    .description("valid filters for sequence data")
                    .properties(requestProperties),
            ).addSchemas(
                REQUEST_SCHEMA_WITH_MIN_PROPORTION,
                Schema<String>()
                    .type("object")
                    .description("valid filters for sequence data")
                    .properties(requestProperties + Pair(MIN_PROPORTION_PROPERTY, Schema<String>().type("number"))),
            ).addSchemas(
                AGGREGATED_REQUEST_SCHEMA,
                requestSchemaWithFields(requestProperties, AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION),
            ).addSchemas(
                DETAILS_REQUEST_SCHEMA,
                requestSchemaWithFields(requestProperties, DETAILS_FIELDS_DESCRIPTION),
            ).addSchemas(
                AGGREGATED_RESPONSE_SCHEMA,
                Schema<String>()
                    .type("object")
                    .description(
                        "Aggregated sequence data. " +
                            "If fields are specified, then these fields are also keys in the result. " +
                            "The key 'count' is always present.",
                    )
                    .required(listOf(COUNT_PROPERTY))
                    .properties(getAggregatedResponseProperties(metadataFieldSchemas(databaseConfig))),
            ).addSchemas(
                DETAILS_RESPONSE_SCHEMA,
                Schema<String>()
                    .type("object")
                    .description("The response contains the metadata of every sequence matching the sequence filters.")
                    .properties(metadataFieldSchemas(databaseConfig)),
            ),
        )
}

private fun metadataFieldSchemas(databaseConfig: DatabaseConfig) =
    databaseConfig.schema.metadata.associate { it.name to Schema<String>().type(mapToOpenApiType(it.type)) }

private fun mapToOpenApiType(type: MetadataType): String = when (type) {
    MetadataType.STRING -> "string"
    MetadataType.PANGO_LINEAGE -> "string"
    MetadataType.DATE -> "string"
    MetadataType.INT -> "integer"
    MetadataType.FLOAT -> "number"
}

private fun sequenceFilterFieldSchemas(sequenceFilterFields: SequenceFilterFields) = sequenceFilterFields.fields
    .map { (fieldName, fieldType) -> fieldName to Schema<String>().type(fieldType.openApiType) }
    .toMap()

private fun requestSchemaWithFields(
    requestProperties: Map<SequenceFilterFieldName, Schema<Any>>,
    fieldsDescription: String,
): Schema<*> =
    Schema<String>()
        .type("object")
        .description("valid filters for sequence data")
        .properties(requestProperties + Pair(FIELDS_PROPERTY, fieldsSchema().description(fieldsDescription)))

private fun getAggregatedResponseProperties(filterProperties: Map<SequenceFilterFieldName, Schema<Any>>) =
    filterProperties.mapValues { (_, schema) ->
        schema.description(
            "This field is present if and only if it was specified in \"fields\" in the request. " +
                "The response is stratified by this field.",
        )
    } + mapOf(
        COUNT_PROPERTY to Schema<String>().type("number").description("The number of sequences matching the filters."),
    )

private fun accessKeySchema() = Schema<String>()
    .type("string")
    .description(
        "An access key that grants access to the protected data that this instance serves. " +
            "There are two types or access keys: One only grants access to aggregated data, " +
            "the other also grants access to detailed data.",
    )

// This is a function so that the resulting schema can be reused in multiple places. The setters mutate the instance.
private fun fieldsSchema() = Schema<String>()
    .type("array")
    .items(Schema<String>().type("string"))
