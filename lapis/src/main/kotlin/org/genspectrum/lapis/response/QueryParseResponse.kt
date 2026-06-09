package org.genspectrum.lapis.response

import io.swagger.v3.oas.annotations.media.Schema
import org.genspectrum.lapis.silo.SiloFilterExpression

data class QueryParseResponse(
    val data: List<ParsedQueryResult>,
    val info: LapisInfo,
)

sealed interface ParsedQueryResult {
    @Schema(description = "Successful query parse result")
    data class Success(
        @field:Schema(description = "The parsed SILO filter expression")
        val filter: SiloFilterExpression,
    ) : ParsedQueryResult {
        @get:Schema(
            description = "Discriminator property, always 'success'",
            allowableValues = ["success"],
        )
        val type: String
            get() = "success"
    }

    @Schema(description = "Failed query parse result")
    data class Failure(
        @field:Schema(description = "Error message describing why parsing failed")
        val error: String,
    ) : ParsedQueryResult {
        @get:Schema(
            description = "Discriminator property, always 'failure'",
            allowableValues = ["failure"],
        )
        val type: String
            get() = "failure"
    }
}
