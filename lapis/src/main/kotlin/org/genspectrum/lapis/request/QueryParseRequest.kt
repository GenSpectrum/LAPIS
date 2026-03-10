package org.genspectrum.lapis.request

import io.swagger.v3.oas.annotations.media.Schema

data class QueryParseRequest(
    @param:Schema(
        description = "A list of advanced query strings to parse into SILO filter expressions.",
        example = """["country = 'USA'", "age >= 30 & age <= 50"]""",
    )
    val queries: List<String>,
    @param:Schema(
        description = "When false (default), LAPIS validates only the query syntax and metadata field names. " +
            "When true, LAPIS sends each query to SILO for full semantic validation, " +
            "which catches invalid mutation positions, unknown genes, and other database-specific errors. " +
            "Full validation is more thorough but adds overhead.",
        defaultValue = "false",
    )
    val doFullValidation: Boolean = false,
)
