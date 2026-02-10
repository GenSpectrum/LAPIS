package org.genspectrum.lapis.model

import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.response.ParsedQueryResult
import org.springframework.stereotype.Component

@Component
class QueryParseModel(
    private val advancedQueryFacade: AdvancedQueryFacade,
) {
    fun parseQueries(queries: List<String>): List<ParsedQueryResult> =
        queries.map { query ->
            try {
                val filter = advancedQueryFacade.map(query)
                ParsedQueryResult(filter = filter, error = null)
            } catch (e: BadRequestException) {
                ParsedQueryResult(filter = null, error = e.message)
            } catch (e: Exception) {
                ParsedQueryResult(
                    filter = null,
                    error = "Unexpected error parsing query: ${e.message}",
                )
            }
        }
}
