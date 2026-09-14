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
import java.time.Duration

// only sets a response header, and failure is ignored below - don't stall a request on it
private val DATA_VERSION_TIMEOUT = Duration.ofSeconds(1)

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
            siloClient.callInfo(DATA_VERSION_TIMEOUT) // populates dataVersion.dataVersion
        } catch (e: Exception) {
            // continue with a null data version: the queries can still be parsed without it
            log.warn { "Could not get current SILO data version: $e" }
        }
        return queries.map { query -> parseSingleQuery(query, doFullValidation) }
    }

    private fun parseSingleQuery(
        query: String,
        doFullValidation: Boolean,
    ): ParsedQueryResult =
        try {
            log.info { "Parsing query: $query" }

            val filter = try {
                advancedQueryFacade.map(query)
            } catch (e: BadRequestException) {
                return ParsedQueryResult.Failure(error = e.message)
            }

            if (doFullValidation) {
                try {
                    validateAgainstSilo(filter)
                } catch (e: SiloException) {
                    if (e.statusCode in 400..<500) {
                        return ParsedQueryResult.Failure(error = e.message)
                    }
                    throw e
                }
            }

            ParsedQueryResult.Success(filter = filter)
        } catch (e: Exception) {
            log.error(e) { "Unexpected error parsing query: $query" }
            ParsedQueryResult.Failure(error = "Unexpected error parsing query.")
        }

    private fun validateAgainstSilo(filter: SiloFilterExpression) {
        val query = SiloQuery(
            action = SiloAction.aggregated(
                groupByFields = emptyList(),
            ),
            filterExpression = filter,
        )

        siloClient.sendQuery(query).use {
            // we don't need the response, but we should consume (and thus close) the returned stream
        }
    }
}
