package org.genspectrum.lapis.model

import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.log
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
        } catch (_: Exception) {
            // If callInfo fails, log it and continue with null dataVersion
            log.warn { "Could not get current SILO data version" }
        }
        return queries.map { query ->
            try {
                val filter = advancedQueryFacade.map(query)
                ParsedQueryResult.Success(filter = filter)
            } catch (e: BadRequestException) {
                ParsedQueryResult.Failure(error = e.message ?: "Unknown error")
            } catch (_: Exception) {
                ParsedQueryResult.Failure(
                    error = "Unexpected error parsing query.",
                )
            }
        }
    }
}
