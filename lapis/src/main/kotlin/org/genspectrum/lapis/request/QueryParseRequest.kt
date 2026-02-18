package org.genspectrum.lapis.request

import io.swagger.v3.oas.annotations.media.Schema

data class QueryParseRequest(
    @param:Schema(
        description = "A list of advanced query strings to parse into SILO filter expressions.",
        example = """["country = 'USA'", "age >= 30 & age <= 50"]""",
    )
    val queries: List<String>,
)
