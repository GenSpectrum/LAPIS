package org.genspectrum.lapis.model.mutationsOverTime

import org.genspectrum.lapis.model.SiloFilterExpressionMapper
import org.genspectrum.lapis.model.nucleotideSymbols
import org.genspectrum.lapis.request.BaseSequenceFilters
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.DateBetween
import org.genspectrum.lapis.silo.HasNucleotideMutation
import org.genspectrum.lapis.silo.NucleotideSymbolEquals
import org.genspectrum.lapis.silo.Or
import org.genspectrum.lapis.silo.SiloAction
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.SiloFilterExpression
import org.genspectrum.lapis.silo.SiloQuery
import org.springframework.stereotype.Component
import java.time.LocalDate

data class MutationOverTimeResponse(
    var rowLabels: List<NucleotideMutation>,
    var columnLabels: List<DateRange>,
    var data: List<List<MutationOverTimeCell>>,
)

data class MutationOverTimeCell(
    var count: Int,
    var coverage: Int,
)

@Component
class MutationsOverTime(
    private val siloClient: SiloClient,
    private val siloFilterExpressionMapper: SiloFilterExpressionMapper,
) {
    fun evaluate(
        mutations: List<NucleotideMutation>,
        dateRanges: List<DateRange>,
        lapisFilter: BaseSequenceFilters,
        dateField: String,
    ): MutationOverTimeResponse {
        if (mutations.isEmpty() || dateRanges.isEmpty()) {
            return MutationOverTimeResponse(
                rowLabels = mutations,
                columnLabels = dateRanges,
                data = emptyList(),
            )
        }

        val dateQuery =
            DateBetween(column = dateField, from = dateRanges[0].dateFrom, to = dateRanges.last().dateTo)

        val siloExpressionFromLapisFilter = siloFilterExpressionMapper.map(lapisFilter)

        val data = mutations.parallelStream().map { mutation ->
            val counts = queryCounts(
                mutation = mutation,
                baseSiloFilterExpression = siloExpressionFromLapisFilter,
                dateQuery = dateQuery,
                dateField = dateField,
            )
            val coverage = queryCoverage(
                mutation = mutation,
                baseSiloFilterExpression = siloExpressionFromLapisFilter,
                dateQuery = dateQuery,
                dateField = dateField,
            )

            buildResultRow(dateRanges, counts, coverage, dateField)
        }.toList()

        return MutationOverTimeResponse(
            rowLabels = mutations,
            columnLabels = dateRanges,
            data = data,
        )
    }

    private fun queryCounts(
        mutation: NucleotideMutation,
        baseSiloFilterExpression: SiloFilterExpression,
        dateQuery: DateBetween,
        dateField: String,
    ): List<AggregationData> {
        val mutationQuery = when (mutation.symbol) {
            null -> HasNucleotideMutation(mutation.sequenceName, mutation.position)
            else -> NucleotideSymbolEquals(
                mutation.sequenceName,
                mutation.position,
                mutation.symbol,
            )
        }

        return sendQuery(
            dateQuery = dateQuery,
            baseSiloFilterExpression = baseSiloFilterExpression,
            dateField = dateField,
            mutationQuery = mutationQuery,
        )
    }

    private fun queryCoverage(
        mutation: NucleotideMutation,
        baseSiloFilterExpression: SiloFilterExpression,
        dateQuery: DateBetween,
        dateField: String,
    ): List<AggregationData> {
        val coverageQuery = Or(
            nucleotideSymbols.map {
                NucleotideSymbolEquals(
                    sequenceName = mutation.sequenceName,
                    position = mutation.position,
                    symbol = it.toString(),
                )
            },
        )

        return sendQuery(
            dateQuery = dateQuery,
            baseSiloFilterExpression = baseSiloFilterExpression,
            dateField = dateField,
            mutationQuery = coverageQuery,
        )
    }

    private fun sendQuery(
        baseSiloFilterExpression: SiloFilterExpression,
        dateQuery: SiloFilterExpression,
        mutationQuery: SiloFilterExpression,
        dateField: String,
    ): List<AggregationData> =
        siloClient.sendQuery(
            SiloQuery(
                SiloAction.aggregated(
                    listOf(dateField),
                    emptyList(),
                    null,
                    null,
                ),
                And(
                    children = listOf(
                        baseSiloFilterExpression,
                        mutationQuery,
                        dateQuery,
                    ),
                ),
            ),
        ).toList()

    private fun buildResultRow(
        dateRanges: List<DateRange>,
        counts: List<AggregationData>,
        coverage: List<AggregationData>,
        dateField: String,
    ): List<MutationOverTimeCell> {
        val result = Array(dateRanges.size) { MutationOverTimeCell(0, 0) }

        counts.forEach { dateCount ->
            val dateString = dateCount.fields[dateField]?.asText()
            val index = findDateRangeIndex(dateRanges, dateString)
            index?.let { result[it] = result[it].copy(count = result[it].count + dateCount.count) }
        }

        coverage.forEach { cov ->
            val dateStr = cov.fields[dateField]?.asText()
            val index = findDateRangeIndex(dateRanges, dateStr)
            index?.let { result[it] = result[it].copy(coverage = result[it].coverage + cov.count) }
        }

        return result.toList()
    }

    private fun findDateRangeIndex(
        dateRanges: List<DateRange>,
        dateString: String?,
    ): Int? {
        if (dateString == null) {
            return null
        }

        val date = LocalDate.parse(dateString)

        return dateRanges.indexOfFirst {
            it.containsDate(date)
        }.takeIf { it >= 0 }
    }
}
