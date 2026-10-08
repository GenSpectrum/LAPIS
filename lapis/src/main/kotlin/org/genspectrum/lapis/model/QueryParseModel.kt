package org.genspectrum.lapis.model

import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.log
import org.genspectrum.lapis.response.ParsedQueryResult
import org.genspectrum.lapis.rhydb.RhyDbAction
import org.genspectrum.lapis.rhydb.RhyDbClient
import org.genspectrum.lapis.rhydb.RhyDbException
import org.genspectrum.lapis.rhydb.RhyDbFilterExpression
import org.genspectrum.lapis.rhydb.RhyDbQuery
import org.springframework.stereotype.Component

@Component
class QueryParseModel(
    private val rhyDbClient: RhyDbClient,
    private val advancedQueryFacade: AdvancedQueryFacade,
) {
    fun parseQueries(
        queries: List<String>,
        doFullValidation: Boolean = false,
    ): List<ParsedQueryResult> {
        try {
            rhyDbClient.callInfo() // populates dataVersion.dataVersion
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
                    validateAgainstRhyDb(filter)
                } catch (e: RhyDbException) {
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

    private fun validateAgainstRhyDb(filter: RhyDbFilterExpression) {
        val query = RhyDbQuery(
            action = RhyDbAction.aggregated(
                groupByFields = emptyList(),
            ),
            filterExpression = filter,
        )

        rhyDbClient.sendQuery(query).use {
            // we don't need the response, but we should consume (and thus close) the returned stream
        }
    }
}
