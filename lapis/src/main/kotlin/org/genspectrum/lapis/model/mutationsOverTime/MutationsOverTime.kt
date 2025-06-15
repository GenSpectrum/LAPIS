package org.genspectrum.lapis.model.mutationsOverTime

import org.genspectrum.lapis.config.ReferenceGenome
import org.genspectrum.lapis.model.SiloFilterExpressionMapper
import org.genspectrum.lapis.model.aaSymbols
import org.genspectrum.lapis.model.nucleotideSymbols
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.BaseSequenceFilters
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.silo.AminoAcidSymbolEquals
import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.DateBetween
import org.genspectrum.lapis.silo.HasAminoAcidMutation
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
    var rowLabels: List<String>,
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
    private val referenceGenome: ReferenceGenome,
) {
    fun evaluateNucleotideMutations(
        mutations: List<NucleotideMutation>,
        dateRanges: List<DateRange>,
        lapisFilter: BaseSequenceFilters,
        dateField: String,
    ): MutationOverTimeResponse =
        evaluateInternal(
            mutations = mutations,
            dateRanges = dateRanges,
            lapisFilter = lapisFilter,
            dateField = dateField,
            mutationToStringFn = { mutation -> mutation.toString(referenceGenome) },
            countQueryFn = { mutation ->
                when (mutation.symbol) {
                    null -> HasNucleotideMutation(mutation.sequenceName, mutation.position)
                    else -> NucleotideSymbolEquals(mutation.sequenceName, mutation.position, mutation.symbol)
                }
            },
            coverageQueryFn = { mutation ->
                Or(
                    nucleotideSymbols.map {
                        NucleotideSymbolEquals(mutation.sequenceName, mutation.position, it.toString())
                    },
                )
            },
        )

    fun evaluateAminoAcidMutations(
        mutations: List<AminoAcidMutation>,
        dateRanges: List<DateRange>,
        lapisFilter: BaseSequenceFilters,
        dateField: String,
    ): MutationOverTimeResponse =
        evaluateInternal(
            mutations = mutations,
            dateRanges = dateRanges,
            lapisFilter = lapisFilter,
            dateField = dateField,
            mutationToStringFn = { mutation -> mutation.toString(referenceGenome) },
            countQueryFn = { mutation ->
                when (mutation.symbol) {
                    null -> HasAminoAcidMutation(mutation.gene, mutation.position)
                    else -> AminoAcidSymbolEquals(mutation.gene, mutation.position, mutation.symbol)
                }
            },
            coverageQueryFn = { mutation ->
                Or(
                    aaSymbols.map {
                        AminoAcidSymbolEquals(mutation.gene, mutation.position, it.toString())
                    },
                )
            },
        )

    private fun <T> evaluateInternal(
        mutations: List<T>,
        dateRanges: List<DateRange>,
        lapisFilter: BaseSequenceFilters,
        dateField: String,
        mutationToStringFn: (T) -> String,
        countQueryFn: (T) -> SiloFilterExpression,
        coverageQueryFn: (T) -> SiloFilterExpression,
    ): MutationOverTimeResponse {
        if (mutations.isEmpty() || dateRanges.isEmpty()) {
            return MutationOverTimeResponse(
                rowLabels = mutations.map(mutationToStringFn),
                columnLabels = dateRanges,
                data = emptyList(),
            )
        }

        val dateQuery = DateBetween(
            column = dateField,
            from = dateRanges.mapNotNull { it.dateFrom }.minOrNull(),
            to = dateRanges.mapNotNull { it.dateTo }.maxOrNull(),
        )

        val baseFilter = siloFilterExpressionMapper.map(lapisFilter)

        val data = mutations.parallelStream().map { mutation ->
            val counts = sendQuery(baseFilter, dateQuery, countQueryFn(mutation), dateField)
            val coverage = sendQuery(baseFilter, dateQuery, coverageQueryFn(mutation), dateField)
            buildResultRow(dateRanges, counts, coverage, dateField)
        }.toList()

        return MutationOverTimeResponse(
            rowLabels = mutations.map(mutationToStringFn),
            columnLabels = dateRanges,
            data = data,
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
