package org.genspectrum.lapis.model.mutationsOverTime

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.annotation.PreDestroy
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.ReferenceGenome
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.model.SiloFilterExpressionMapper
import org.genspectrum.lapis.model.aaSymbols
import org.genspectrum.lapis.model.deletionSymbols
import org.genspectrum.lapis.model.nucleotideSymbols
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.BaseSequenceFilters
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.OrderBySpec
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

private const val MUTATION_QUERY_TIMEOUT_SECONDS = 60L

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
    private val config: DatabaseConfig,
) {
    /**
     * Thread pool used for parallel queries to SILO.
     */
    private val threadPool = Executors.newFixedThreadPool(config.siloClientThreadCount)

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
            mutations = mutations,
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
        )
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
            mutations = mutations,
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
        )
    }

    fun <T> evaluateInternal(
        mutations: List<T>,
        dateRanges: List<DateRange>,
        lapisFilter: BaseSequenceFilters,
        dateField: String,
        remainingRetries: Int = 1,
        mutationToStringFn: (mutation: T) -> String,
        countQueryFn: (mutation: T) -> SiloFilterExpression,
        coverageQueryFn: (mutation: T) -> SiloFilterExpression,
    ): MutationsOverTimeResult {
        if (mutations.isEmpty() || dateRanges.isEmpty()) {
            siloClient.callInfo() // populates dataVersion.dataVersion
            return MutationsOverTimeResult(
                mutations = mutations.map(mutationToStringFn),
                dateRanges = dateRanges,
                data = emptyList(),
                totalCountsByDateRange = emptyList(),
            )
        }

        val dateQuery =
            DateBetween(
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

        val tasks = mutations.map { mutation ->
            Callable {
                val counts = sendQuery(baseFilter, dateQuery, countQueryFn(mutation), dateField, false)
                val coverage = sendQuery(baseFilter, dateQuery, coverageQueryFn(mutation), dateField, false)
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
            MUTATION_QUERY_TIMEOUT_SECONDS,
            TimeUnit.SECONDS,
        ).map {
            it.get()
        }

        val dataVersions = dataWithDataVersions.flatMap { it.first } + dailyTotalsDataVersion
        if (dataVersions.distinct().size != 1) {
            if (remainingRetries > 0) {
                return evaluateInternal(
                    mutations,
                    dateRanges,
                    lapisFilter,
                    dateField,
                    remainingRetries - 1,
                    mutationToStringFn,
                    countQueryFn,
                    coverageQueryFn,
                )
            }
            throw RuntimeException(
                "The data has been updated multiple times during the execution of the request. This is unexpected. " +
                    "Please try again or inform the administrator of the LAPIS instance or the LAPIS developers.",
            )
        }
        dataVersion.dataVersion = dataVersions.first()

        return MutationsOverTimeResult(
            mutations = mutations.map(mutationToStringFn),
            dateRanges = dateRanges,
            data = dataWithDataVersions.map { it.second },
            totalCountsByDateRange = totalCountsByDateRange,
        )
    }

    private fun sendQuery(
        baseSiloFilterExpression: SiloFilterExpression,
        dateQuery: SiloFilterExpression,
        mutationQuery: SiloFilterExpression?,
        dateField: String,
        checkProtection: Boolean = true,
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
            checkProtection,
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
    ): List<MutationsOverTimeCell> {
        val result = Array(dateRanges.size) { MutationsOverTimeCell(0, 0) }

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
        val dateString = datum.fields[dateField]?.asText()
        if (dateString == null) {
            return null
        }

        val date = LocalDate.parse(dateString)

        return dateRanges.indexOfFirst {
            it.containsDate(date)
        }.takeIf { it >= 0 }
    }

    @PreDestroy
    fun shutdownThreadPool() {
        threadPool.shutdown()
        if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
            threadPool.shutdownNow()
        }
    }
}
