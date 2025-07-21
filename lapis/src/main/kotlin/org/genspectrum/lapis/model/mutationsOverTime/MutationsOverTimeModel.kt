package org.genspectrum.lapis.model.mutationsOverTime

import io.swagger.v3.oas.annotations.media.Schema
import org.genspectrum.lapis.config.ReferenceGenome
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.model.SiloFilterExpressionMapper
import org.genspectrum.lapis.model.aaSymbols
import org.genspectrum.lapis.model.deletionSymbols
import org.genspectrum.lapis.model.nucleotideSymbols
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.BaseSequenceFilters
import org.genspectrum.lapis.request.NucleotideMutation
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
            return MutationsOverTimeResult(
                mutations = mutations.map(mutationToStringFn),
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

        val baseFilter = siloFilterExpressionMapper.map(lapisFilter)

        val dataWithDataVersions = mutations.parallelStream().map { mutation ->
            val countsWithDataVersion = sendQuery(baseFilter, dateQuery, countQueryFn(mutation), dateField)
            val coverageWithDataVersion = sendQuery(baseFilter, dateQuery, coverageQueryFn(mutation), dateField)

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
