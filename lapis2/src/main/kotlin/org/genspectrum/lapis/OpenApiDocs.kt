package org.genspectrum.lapis

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.OpennessLevel
import org.genspectrum.lapis.config.SequenceFilterFields
import org.genspectrum.lapis.controller.MIN_PROPORTION_PROPERTY
import org.genspectrum.lapis.controller.REQUEST_SCHEMA
import org.genspectrum.lapis.controller.REQUEST_SCHEMA_WITH_FIELDS
import org.genspectrum.lapis.controller.REQUEST_SCHEMA_WITH_MIN_PROPORTION
import org.genspectrum.lapis.controller.RESPONSE_SCHEMA_AGGREGATED

fun buildOpenApiSchema(sequenceFilterFields: SequenceFilterFields, databaseConfig: DatabaseConfig): OpenAPI {
    val filterProperties = sequenceFilterFields.fields
        .map { (fieldName, fieldType) -> fieldName to Schema<String>().type(fieldType.openApiType) }
        .toMap()

    val requestProperties = when (databaseConfig.schema.opennessLevel) {
        OpennessLevel.PROTECTED -> filterProperties + ("accessKey" to accessKeySchema)
        else -> filterProperties
    }

    val responseProperties = filterProperties + mapOf(
        "count" to Schema<String>().type("number"),
    )

    return OpenAPI()
        .components(
            Components().addSchemas(
                REQUEST_SCHEMA,
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
                REQUEST_SCHEMA_WITH_FIELDS,
                Schema<String>()
                    .type("object")
                    .description("valid filters for sequence data")
                    .properties(
                        requestProperties + Pair(
                            "fields",
                            fieldsSchema,
                        ),
                    ),
            ).addSchemas(
                RESPONSE_SCHEMA_AGGREGATED,
                Schema<String>()
                    .type("object")
                    .description(
                        "Aggregated sequence data. " +
                            "If fields are specified, then these fields are also keys in the result. " +
                            "The key 'count' is always present.",
                    )
                    .required(listOf("count"))
                    .properties(responseProperties),
            ),
        )
}

private val accessKeySchema = Schema<String>()
    .type("string")
    .description(
        "An access key that grants access to the protected data that this instance serves. " +
            "There are two types or access keys: One only grants access to aggregated data, " +
            "the other also grants access to detailed data.",
    )

private val fieldsSchema = Schema<String>()
    .type("array")
    .items(Schema<String>().type("string"))
    .description(
        "The fields to stratify the result by. " +
            "The response will contain the fields specified here with their respective values.",
    )
