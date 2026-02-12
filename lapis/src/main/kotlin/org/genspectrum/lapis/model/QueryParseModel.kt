package org.genspectrum.lapis.model

import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.response.ParsedQueryResult
import org.genspectrum.lapis.silo.SiloClient
import org.springframework.stereotype.Component

@Component
class QueryParseModel(
    private val siloClient: SiloClient,
    private val advancedQueryFacade: AdvancedQueryFacade,
) {
    fun parseQueries(queries: List<String>): List<ParsedQueryResult> {
        try {
            siloClient.callInfo() // populates dataVersion.dataVersion
        } catch (e: Exception) {
            // If callInfo fails, continue with null dataVersion
        }
        return queries.map { query ->
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
}
