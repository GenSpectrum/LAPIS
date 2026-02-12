package org.genspectrum.lapis.response

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema
import org.genspectrum.lapis.silo.SiloFilterExpression

data class QueryParseResponse(
    val data: List<ParsedQueryResult>,
    val info: LapisInfo,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ParsedQueryResult.Success::class, name = "success"),
    JsonSubTypes.Type(value = ParsedQueryResult.Failure::class, name = "failure"),
)
sealed interface ParsedQueryResult {

    @Schema(description = "Successful query parse result")
    data class Success(
        @field:Schema(description = "The parsed SILO filter expression")
        val filter: SiloFilterExpression,
    ) : ParsedQueryResult

    @Schema(description = "Failed query parse result")
    data class Failure(
        @field:Schema(description = "Error message describing why parsing failed")
        val error: String,
    ) : ParsedQueryResult
}
