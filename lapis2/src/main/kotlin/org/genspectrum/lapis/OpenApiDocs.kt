package org.genspectrum.lapis

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.controller.MIN_PROPORTION_PROPERTY
import org.genspectrum.lapis.controller.REQUEST_SCHEMA
import org.genspectrum.lapis.controller.REQUEST_SCHEMA_WITH_MIN_PROPORTION

fun buildOpenApiSchema(databaseConfig: DatabaseConfig): OpenAPI {
    val properties = databaseConfig.schema.metadata.map { it.name }.associateWith { Schema<String>().type("string") }

    return OpenAPI()
        .components(
            Components().addSchemas(
                REQUEST_SCHEMA,
                Schema<String>()
                    .type("object")
                    .description("valid filters for sequence data")
                    .properties(properties),
            ).addSchemas(
                REQUEST_SCHEMA_WITH_MIN_PROPORTION,
                Schema<String>()
                    .type("object")
                    .description("valid filters for sequence data")
                    .properties(properties + Pair(MIN_PROPORTION_PROPERTY, Schema<String>().type("number"))),
            ),
        )
}
