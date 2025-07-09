package org.genspectrum.lapis.model.mutationsOverTime

import io.swagger.v3.oas.annotations.media.Schema
import org.genspectrum.lapis.config.ReferenceGenome
import org.genspectrum.lapis.model.SiloFilterExpressionMapper
import org.genspectrum.lapis.model.deletionSymbols
import org.genspectrum.lapis.model.nucleotideSymbols
import org.genspectrum.lapis.request.BaseSequenceFilters
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.DataVersion
import org.genspectrum.lapis.silo.DateBetween
import org.genspectrum.lapis.silo.HasNucleotideMutation
import org.genspectrum.lapis.silo.NucleotideSymbolEquals
import org.genspectrum.lapis.silo.Or
import org.genspectrum.lapis.silo.SiloAction
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.SiloFilterExpression
import org.genspectrum.lapis.silo.SiloQuery
import org.genspectrum.lapis.silo.WithDataVersion
import org.springframework.stereotype.Component
import java.time.LocalDate

@Schema(
    description = "The result in tabular format with mutations as rows (outer array) and date ranges as " +
        "columns (inner array).",
)
data class MutationsOverTimeResult(
    @param:Schema(
        description = "The list of requested mutations",
    )
    var mutations: List<String>,
    @param:Schema(
        description = "The list of requested date ranges",
    )
    var dateRanges: List<DateRange>,
    @param:Schema(
        description = "A 2D array of mutation counts and coverage. " +
            "It should be understood as data[index of mutation][index of date range].",
    )
    var data: List<List<MutationsOverTimeCell>>,
)

data class MutationsOverTimeCell(
    @param:Schema(description = "Number of sequences with the mutation in the date range")
    var count: Int,
    @param:Schema(
        description = "Number of sequences with coverage (i.e., having a non-ambiguous symbol) at the position in" +
            "the date range. Confirmed deletions (i.e., \"-\") are included.",
    )
    var coverage: Int,
)

@Component
class MutationsOverTimeModel(
    private val siloClient: SiloClient,
    private val siloFilterExpressionMapper: SiloFilterExpressionMapper,
    private val referenceGenome: ReferenceGenome,
    private val dataVersion: DataVersion,
) {
    fun evaluate(
        mutations: List<NucleotideMutation>,
        dateRanges: List<DateRange>,
        lapisFilter: BaseSequenceFilters,
        dateField: String,
        remainingRetries: Int = 1,
    ): MutationsOverTimeResult {
        if (mutations.isEmpty() || dateRanges.isEmpty()) {
            return MutationsOverTimeResult(
                mutations = mutations.map { it.toString(referenceGenome) },
                dateRanges = dateRanges,
                data = emptyList(),
            )
        }

        val dateQuery =
            DateBetween(
                column = dateField,
                from = dateRanges.mapNotNull { it.dateFrom }.minOrNull(),
                to = dateRanges.mapNotNull { it.dateTo }.maxOrNull(),
            )

        val siloExpressionFromLapisFilter = siloFilterExpressionMapper.map(lapisFilter)

        val dataWithDataVersions = mutations.parallelStream().map { mutation ->
            val countsWithDataVersion = queryCounts(
                mutation = mutation,
                baseSiloFilterExpression = siloExpressionFromLapisFilter,
                dateQuery = dateQuery,
                dateField = dateField,
            )
            val coverageWithDataVersion = queryCoverage(
                mutation = mutation,
                baseSiloFilterExpression = siloExpressionFromLapisFilter,
                dateQuery = dateQuery,
                dateField = dateField,
            )

            listOf(countsWithDataVersion.dataVersion, coverageWithDataVersion.dataVersion) to
                buildResultRow(
                    dateRanges,
                    countsWithDataVersion.queryResult,
                    coverageWithDataVersion.queryResult,
                    dateField,
                )
        }.toList()

        val dataVersions = dataWithDataVersions.flatMap { it.first }
        if (dataVersions.distinct().size != 1) {
            if (remainingRetries > 0) {
                return evaluate(mutations, dateRanges, lapisFilter, dateField, remainingRetries - 1)
            }
            throw RuntimeException(
                "The data has been updated multiple times during the execution of the request. This is unexpected. " +
                    "Please try again or inform the administrator of the LAPIS instance or the LAPIS developers.",
            )
        }
        dataVersion.dataVersion = dataVersions.first()

        return MutationsOverTimeResult(
            mutations = mutations.map { it.toString(referenceGenome) },
            dateRanges = dateRanges,
            data = dataWithDataVersions.map { it.second },
        )
    }

    private fun queryCounts(
        mutation: NucleotideMutation,
        baseSiloFilterExpression: SiloFilterExpression,
        dateQuery: DateBetween,
        dateField: String,
    ): WithDataVersion<List<AggregationData>> {
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
    ): WithDataVersion<List<AggregationData>> {
        val coverageQuery = Or(
            (nucleotideSymbols + deletionSymbols).map {
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
    ): WithDataVersion<List<AggregationData>> =
        siloClient.sendQueryAndGetDataVersion(
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
            setRequestDataVersion = false,
        ).map { it.toList() }

    private fun buildResultRow(
        dateRanges: List<DateRange>,
        counts: List<AggregationData>,
        coverage: List<AggregationData>,
        dateField: String,
    ): List<MutationsOverTimeCell> {
        val result = Array(dateRanges.size) { MutationsOverTimeCell(0, 0) }

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
