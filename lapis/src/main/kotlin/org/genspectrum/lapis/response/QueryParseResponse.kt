package org.genspectrum.lapis.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.genspectrum.lapis.silo.SiloFilterExpression

data class QueryParseResponse(
    val data: List<ParsedQueryResult>,
    val info: LapisInfo,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ParsedQueryResult(
    @param:Schema(
        description = "The parsed SILO filter expression. Null if parsing failed.",
    )
    val filter: SiloFilterExpression? = null,
    @param:Schema(
        description = "Error message if parsing failed. Null if parsing succeeded.",
    )
    val error: String? = null,
)
