package org.genspectrum.lapis.model

import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.log
import org.genspectrum.lapis.response.ParsedQueryResult
import org.genspectrum.lapis.silo.SiloAction
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.SiloException
import org.genspectrum.lapis.silo.SiloFilterExpression
import org.genspectrum.lapis.silo.SiloQuery
import org.springframework.stereotype.Component

@Component
class QueryParseModel(
    private val siloClient: SiloClient,
    private val advancedQueryFacade: AdvancedQueryFacade,
) {
    fun parseQueries(
        queries: List<String>,
        doFullValidation: Boolean = false,
    ): List<ParsedQueryResult> {
        try {
            siloClient.callInfo() // populates dataVersion.dataVersion
        } catch (_: Exception) {
            // If callInfo fails, log it and continue with null dataVersion
            log.warn { "Could not get current SILO data version" }
        }
        return queries.map { query -> parseSingleQuery(query, doFullValidation) }
    }

    private fun parseSingleQuery(
        query: String,
        doFullValidation: Boolean,
    ): ParsedQueryResult =
        try {
            val filter = try {
                advancedQueryFacade.map(query)
            } catch (e: BadRequestException) {
                return ParsedQueryResult.Failure(error = e.message)
            }

            if (doFullValidation) {
                try {
                    validateAgainstSilo(filter)
                } catch (e: SiloException) {
                    return ParsedQueryResult.Failure(error = e.message)
                }
            }

            ParsedQueryResult.Success(filter = filter)
        } catch (_: Exception) {
            ParsedQueryResult.Failure(error = "Unexpected error parsing query.")
        }

    private fun validateAgainstSilo(filter: SiloFilterExpression) {
        val query = SiloQuery(
            action = SiloAction.aggregated(
                groupByFields = emptyList(),
                limit = 1,
            ),
            filterExpression = filter,
        )

        siloClient.sendQuery(query).use {
            // we don't need the response, but we should consume (and thus close) the returned stream
        }
    }
}
