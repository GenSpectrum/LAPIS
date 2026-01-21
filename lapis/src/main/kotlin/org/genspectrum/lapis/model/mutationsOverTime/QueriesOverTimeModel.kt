package org.genspectrum.lapis.model.mutationsOverTime

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.annotation.PreDestroy
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.ReferenceGenome
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.model.AdvancedQueryFacade
import org.genspectrum.lapis.model.SiloFilterExpressionMapper
import org.genspectrum.lapis.model.aaSymbols
import org.genspectrum.lapis.model.deletionSymbols
import org.genspectrum.lapis.model.nucleotideSymbols
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.BaseSequenceFilters
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.OrderBySpec
import org.genspectrum.lapis.request.QueryOverTimeItem
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.silo.AminoAcidSymbolEquals
import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.DataVersion
import org.genspectrum.lapis.silo.DateBetween
import org.genspectrum.lapis.silo.HasAminoAcidMutation
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
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val QUERY_TIMEOUT_SECONDS = 60L

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
    @param:Schema(
        description = "The list of total sample counts per date range",
    )
    var totalCountsByDateRange: List<Number>,
    @param:Schema(
        description = "Aggregated statistics per mutation, summed across all date ranges. " +
            "One entry per mutation in the same order as the mutations array.",
    )
    var overallStatisticsByMutation: List<OverallStatistics>,
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

@Schema(
    description = "The result in tabular format with queries as rows (outer array) and date ranges as " +
        "columns (inner array).",
)
data class QueriesOverTimeResult(
    @param:Schema(
        description = "The 'displayLabel's of the supplied queries",
    )
    var queries: List<String>,
    @param:Schema(
        description = "The list of requested date ranges",
    )
    var dateRanges: List<DateRange>,
    @param:Schema(
        description = "A 2D array of query counts and coverage. " +
            "It should be understood as data[index of query][index of date range].",
    )
    var data: List<List<QueryOverTimeCell>>,
    @param:Schema(
        description = "The list of total sample counts per date range",
    )
    var totalCountsByDateRange: List<Number>,
    @param:Schema(
        description = "Aggregated statistics per query, summed across all date ranges. " +
            "One entry per query in the same order as the queries array.",
    )
    var overallStatisticsByQuery: List<OverallStatistics>,
) {
    fun toMutationsOverTimeResult() =
        MutationsOverTimeResult(
            mutations = queries,
            dateRanges = dateRanges,
            data = data.map { row ->
                row.map { cell ->
                    MutationsOverTimeCell(
                        count = cell.count,
                        coverage = cell.coverage,
                    )
                }
            },
            totalCountsByDateRange = totalCountsByDateRange,
            overallStatisticsByMutation = overallStatisticsByQuery,
        )
}

data class QueryOverTimeCell(
    @param:Schema(description = "Number of sequences that match the 'countQuery' in the date range")
    var count: Int,
    @param:Schema(
        description = "Number of sequences that match the 'coverageQuery' in the date range. " +
            "The query should be picked such that this number is the count of sequences that have a non-ambiguous " +
            "symbol at the positions of interest.",
    )
    var coverage: Int,
)

data class OverallStatistics(
    @param:Schema(description = "Total count across all date ranges")
    var count: Int,
    @param:Schema(description = "Total coverage across all date ranges")
    var coverage: Int,
    @param:Schema(description = "Proportion (count / coverage). Omitted if coverage is 0.")
    var proportion: Double?,
)

@Component
class QueriesOverTimeModel(
    private val siloClient: SiloClient,
    private val siloFilterExpressionMapper: SiloFilterExpressionMapper,
    private val referenceGenome: ReferenceGenome,
    private val dataVersion: DataVersion,
    private val advancedQueryFacade: AdvancedQueryFacade,
    config: DatabaseConfig,
) {
    /**
     * Thread pool used for parallel queries to SILO.
     */
    private val threadPool = Executors.newFixedThreadPool(config.siloClientThreadCount)

    fun evaluateQueriesOverTime(
        queries: List<QueryOverTimeItem>,
        dateRanges: List<DateRange>,
        lapisFilter: BaseSequenceFilters,
        dateField: String,
        remainingRetries: Int = 1,
    ): QueriesOverTimeResult =
        evaluateInternal(
            queryItems = queries,
            dateRanges = dateRanges,
            lapisFilter = lapisFilter,
            dateField = dateField,
            remainingRetries = remainingRetries,
            mutationToStringFn = { query -> query.displayLabel ?: query.countQuery },
            countQueryFn = { query -> advancedQueryFacade.map(query.countQuery) },
            coverageQueryFn = { query -> advancedQueryFacade.map(query.coverageQuery) },
        )

    fun evaluateAminoAcidMutations(
        mutations: List<AminoAcidMutation>,
        dateRanges: List<DateRange>,
        lapisFilter: BaseSequenceFilters,
        dateField: String,
        remainingRetries: Int = 1,
    ): MutationsOverTimeResult {
        for (mutation in mutations) {
            if (mutation.maybe) {
                throw BadRequestException(
                    "Invalid mutation in includeMutations – maybe() is not allowed: " +
                        mutation.toString(referenceGenome),
                )
            }
        }
        return evaluateInternal(
            queryItems = mutations,
            dateRanges = dateRanges,
            lapisFilter = lapisFilter,
            dateField = dateField,
            remainingRetries = remainingRetries,
            mutationToStringFn = { mutation -> mutation.toString(referenceGenome) },
            countQueryFn = { mutation ->
                when (mutation.symbol) {
                    null -> HasAminoAcidMutation(mutation.gene, mutation.position)

                    else -> AminoAcidSymbolEquals(
                        mutation.gene,
                        mutation.position,
                        mutation.symbol,
                    )
                }
            },
            coverageQueryFn = { mutation ->
                Or(
                    (aaSymbols + deletionSymbols).map {
                        AminoAcidSymbolEquals(
                            sequenceName = mutation.gene,
                            position = mutation.position,
                            symbol = it.toString(),
                        )
                    },
                )
            },
        ).toMutationsOverTimeResult()
    }

    fun evaluateNucleotideMutations(
        mutations: List<NucleotideMutation>,
        dateRanges: List<DateRange>,
        lapisFilter: BaseSequenceFilters,
        dateField: String,
        remainingRetries: Int = 1,
    ): MutationsOverTimeResult {
        for (mutation in mutations) {
            if (mutation.maybe) {
                throw BadRequestException(
                    "Invalid mutation in includeMutations – maybe() is not allowed: " +
                        mutation.toString(referenceGenome),
                )
            }
        }
        return evaluateInternal(
            queryItems = mutations,
            dateRanges = dateRanges,
            lapisFilter = lapisFilter,
            dateField = dateField,
            remainingRetries = remainingRetries,
            mutationToStringFn = { mutation -> mutation.toString(referenceGenome) },
            countQueryFn = { mutation ->
                when (mutation.symbol) {
                    null -> HasNucleotideMutation(mutation.sequenceName, mutation.position)

                    else -> NucleotideSymbolEquals(
                        mutation.sequenceName,
                        mutation.position,
                        mutation.symbol,
                    )
                }
            },
            coverageQueryFn = { mutation ->
                Or(
                    (nucleotideSymbols + deletionSymbols).map {
                        NucleotideSymbolEquals(
                            sequenceName = mutation.sequenceName,
                            position = mutation.position,
                            symbol = it.toString(),
                        )
                    },
                )
            },
        ).toMutationsOverTimeResult()
    }

    fun <T> evaluateInternal(
        queryItems: List<T>,
        dateRanges: List<DateRange>,
        lapisFilter: BaseSequenceFilters,
        dateField: String,
        remainingRetries: Int = 1,
        mutationToStringFn: (queryItem: T) -> String,
        countQueryFn: (queryItem: T) -> SiloFilterExpression,
        coverageQueryFn: (queryItem: T) -> SiloFilterExpression,
    ): QueriesOverTimeResult {
        if (queryItems.isEmpty() || dateRanges.isEmpty()) {
            siloClient.callInfo() // populates dataVersion.dataVersion
            return QueriesOverTimeResult(
                queries = queryItems.map(mutationToStringFn),
                dateRanges = dateRanges,
                data = emptyList(),
                totalCountsByDateRange = emptyList(),
                overallStatisticsByQuery = emptyList(),
            )
        }

        val dateQuery = DateBetween(
            column = dateField,
            from = dateRanges.mapNotNull { it.dateFrom }.minOrNull(),
            to = dateRanges.mapNotNull { it.dateTo }.maxOrNull(),
        )

        val baseFilter = siloFilterExpressionMapper.map(lapisFilter)

        val dailyTotalsWithDataVersion = sendQuery(baseFilter, dateQuery, null, dateField)
        val dailyTotalsDataVersion = dailyTotalsWithDataVersion.dataVersion
        val totalCountsByDateRange = aggregateDailyCountsIntoDateRanges(
            dailyTotalsWithDataVersion.queryResult,
            dateField,
            dateRanges,
        )

        val tasks = queryItems.map { mutation ->
            Callable {
                val counts = sendQuery(baseFilter, dateQuery, countQueryFn(mutation), dateField)
                val coverage = sendQuery(baseFilter, dateQuery, coverageQueryFn(mutation), dateField)
                listOf(counts.dataVersion, coverage.dataVersion) to
                    aggregateDailyMutationDataIntoDateRanges(
                        counts.queryResult,
                        coverage.queryResult,
                        dateField,
                        dateRanges,
                    )
            }
        }

        val dataWithDataVersions = this.threadPool.invokeAll(
            tasks,
            QUERY_TIMEOUT_SECONDS,
            TimeUnit.SECONDS,
        ).map {
            it.get()
        }

        val dataVersions = dataWithDataVersions.flatMap { it.first } + dailyTotalsDataVersion
        if (dataVersions.distinct().size != 1) {
            if (remainingRetries > 0) {
                return evaluateInternal(
                    queryItems = queryItems,
                    dateRanges = dateRanges,
                    lapisFilter = lapisFilter,
                    dateField = dateField,
                    remainingRetries = remainingRetries - 1,
                    mutationToStringFn = mutationToStringFn,
                    countQueryFn = countQueryFn,
                    coverageQueryFn = coverageQueryFn,
                )
            }
            throw RuntimeException(
                "The data has been updated multiple times during the execution of the request. This is unexpected. " +
                    "Please try again or inform the administrator of the LAPIS instance or the LAPIS developers.",
            )
        }
        dataVersion.dataVersion = dataVersions.first()

        val overallStats = computeOverallStatistics(dataWithDataVersions.map { it.second })

        return QueriesOverTimeResult(
            queries = queryItems.map(mutationToStringFn),
            dateRanges = dateRanges,
            data = dataWithDataVersions.map { it.second },
            totalCountsByDateRange = totalCountsByDateRange,
            overallStatisticsByQuery = overallStats,
        )
    }

    private fun sendQuery(
        baseSiloFilterExpression: SiloFilterExpression,
        dateQuery: SiloFilterExpression,
        mutationQuery: SiloFilterExpression?,
        dateField: String,
    ): WithDataVersion<List<AggregationData>> =
        siloClient.sendQueryAndGetDataVersion(
            SiloQuery(
                SiloAction.aggregated(
                    listOf(dateField),
                    OrderBySpec.EMPTY,
                    null,
                    null,
                ),
                And(
                    children = listOfNotNull(
                        baseSiloFilterExpression,
                        mutationQuery,
                        dateQuery,
                    ),
                ),
            ),
            setRequestDataVersion = false,
        ).map { it.toList() }

    /**
     * Builds a result row for one particular mutation.
     * The date ranges are the 'columns' of the row, there is one cell per date range.
     * `counts` and `coverage` are data from SILO, for every day in the overall range of dates
     * defined by the list of date ranges.
     */
    private fun aggregateDailyMutationDataIntoDateRanges(
        counts: List<AggregationData>,
        coverage: List<AggregationData>,
        dateField: String,
        dateRanges: List<DateRange>,
    ): List<QueryOverTimeCell> {
        val result = Array(dateRanges.size) { QueryOverTimeCell(0, 0) }

        counts.forEach { dateCount ->
            val index = findDateRangeIndex(dateCount, dateField, dateRanges)
            index?.let { result[it] = result[it].copy(count = result[it].count + dateCount.count) }
        }

        coverage.forEach { cov ->
            val index = findDateRangeIndex(cov, dateField, dateRanges)
            index?.let { result[it] = result[it].copy(coverage = result[it].coverage + cov.count) }
        }

        return result.toList()
    }

    private fun aggregateDailyCountsIntoDateRanges(
        counts: List<AggregationData>,
        dateField: String,
        dateRanges: List<DateRange>,
    ): List<Number> {
        val result = Array(dateRanges.size) { 0 }

        counts.forEach { dateCount ->
            val index = findDateRangeIndex(dateCount, dateField, dateRanges)
            index?.let { result[it] = result[it] + dateCount.count }
        }

        return result.toList()
    }

    /**
     * Given a datum that has a date inside the dateField, find the index of the dateRange
     * in the list of dateRanges that this datum belongs to.
     */
    private fun findDateRangeIndex(
        datum: AggregationData,
        dateField: String,
        dateRanges: List<DateRange>,
    ): Int? {
        val dateString = datum.fields[dateField]?.asText() ?: return null

        val date = LocalDate.parse(dateString)

        return dateRanges.indexOfFirst {
            it.containsDate(date)
        }.takeIf { it >= 0 }
    }

    /**
     * Aggregates statistics across all date ranges for each query/mutation.
     * For each row in the data (representing a mutation/query), sums up the counts and coverage
     * across all columns (representing date ranges).
     */
    private fun computeOverallStatistics(data: List<List<QueryOverTimeCell>>): List<OverallStatistics> {
        return data.map { row ->
            val totalCount = row.sumOf { it.count }
            val totalCoverage = row.sumOf { it.coverage }
            OverallStatistics(
                count = totalCount,
                coverage = totalCoverage,
                proportion = if (totalCoverage > 0) totalCount.toDouble() / totalCoverage else null,
            )
        }
    }

    @PreDestroy
    fun shutdownThreadPool() {
        threadPool.shutdown()
        if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
            threadPool.shutdownNow()
        }
    }
}
