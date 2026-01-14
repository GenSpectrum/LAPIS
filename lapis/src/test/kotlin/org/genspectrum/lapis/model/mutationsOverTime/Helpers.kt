package org.genspectrum.lapis.model.mutationsOverTime

import io.mockk.every
import org.genspectrum.lapis.model.aaSymbols
import org.genspectrum.lapis.model.deletionSymbols
import org.genspectrum.lapis.model.nucleotideSymbols
import org.genspectrum.lapis.request.AminoAcidInsertion
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.BaseSequenceFilters
import org.genspectrum.lapis.request.NucleotideInsertion
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.OrderBySpec
import org.genspectrum.lapis.request.SequenceFilters
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.InfoData
import org.genspectrum.lapis.silo.AminoAcidSymbolEquals
import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.DataVersion
import org.genspectrum.lapis.silo.DateBetween
import org.genspectrum.lapis.silo.NucleotideSymbolEquals
import org.genspectrum.lapis.silo.Or
import org.genspectrum.lapis.silo.SiloAction
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.SiloFilterExpression
import org.genspectrum.lapis.silo.True
import org.genspectrum.lapis.silo.WithDataVersion
import java.time.LocalDate
import java.util.stream.Stream

data class TestLapisFilter(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aminoAcidMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
) : BaseSequenceFilters

val DUMMY_LAPIS_FILTER = TestLapisFilter(emptyMap(), emptyList(), emptyList(), emptyList(), emptyList())
const val DUMMY_DATA_VERSION = "1750941114"

val DUMMY_DATE_RANGE1 = DateRange(LocalDate.parse("2021-01-01"), LocalDate.parse("2021-12-31"))
val DUMMY_DATE_RANGE2 = DateRange(LocalDate.parse("2022-01-01"), LocalDate.parse("2022-12-31"))
val DUMMY_DATE_BETWEEN_ALL =
    DateBetween(DUMMY_DATE_FIELD, LocalDate.parse("2021-01-01"), LocalDate.parse("2022-12-31"))

const val DUMMY_DATE_FIELD = "date"
val AGGREGATED_SILO_ACTION = SiloAction.aggregated(
    listOf(DUMMY_DATE_FIELD),
    OrderBySpec.EMPTY,
    null,
    null,
)

fun mockSiloCallInfo(
    siloClient: SiloClient,
    dataVersion: DataVersion,
) {
    every {
        siloClient.callInfo()
    } answers {
        dataVersion.dataVersion = DUMMY_DATA_VERSION
        InfoData(DUMMY_DATA_VERSION, null)
    }
}

fun mockSiloCountQuery(
    siloClient: SiloClient,
    mutationFilter: SiloFilterExpression,
    dateBetweenFilter: DateBetween,
    queryResult: Stream<AggregationData>,
) {
    every {
        siloClient.sendQueryAndGetDataVersion<AggregationData>(
            query = match { query ->
                query.action == AGGREGATED_SILO_ACTION &&
                    query.filterExpression is And &&
                    query.filterExpression.children.count() == 3 &&
                    query.filterExpression.children.contains(mutationFilter) &&
                    query.filterExpression.children.contains(dateBetweenFilter)
            },
            setRequestDataVersion = false,
        )
    } answers {
        WithDataVersion(DUMMY_DATA_VERSION, queryResult)
    }
}

fun mockSiloNucleotideCoverageQuery(
    siloClient: SiloClient,
    sequenceName: String?,
    position: Int,
    dateBetween: DateBetween,
    queryResult: Stream<AggregationData>,
) = mockSiloCoverageQuery(
    siloClient,
    dateBetween,
    queryResult,
    {
        it is Or &&
            (it).children.all { child ->
                child is NucleotideSymbolEquals &&
                    child.sequenceName == sequenceName &&
                    child.position == position &&
                    child.symbol in (nucleotideSymbols + deletionSymbols).map { symbol -> symbol.toString() }
            }
    },
)

fun mockSiloAminoAcidCoverageQuery(
    siloClient: SiloClient,
    sequenceName: String?,
    position: Int,
    dateBetween: DateBetween,
    queryResult: Stream<AggregationData>,
) = mockSiloCoverageQuery(
    siloClient,
    dateBetween,
    queryResult,
    {
        it is Or &&
            (it).children.all { child ->
                child is AminoAcidSymbolEquals &&
                    child.sequenceName == sequenceName &&
                    child.position == position &&
                    child.symbol in (aaSymbols + deletionSymbols).map { symbol -> symbol.toString() }
            }
    },
)

fun mockSiloCoverageQuery(
    siloClient: SiloClient,
    dateBetween: DateBetween,
    queryResult: Stream<AggregationData>,
    coverageFilterExpressionFn: (SiloFilterExpression) -> Boolean,
) {
    every {
        siloClient.sendQueryAndGetDataVersion<AggregationData>(
            query = match { query ->
                query.action == AGGREGATED_SILO_ACTION &&
                    query.filterExpression is And &&
                    query.filterExpression.children.count() == 3 &&
                    query.filterExpression.children.any(coverageFilterExpressionFn) &&
                    query.filterExpression.children.contains(dateBetween)
            },
            setRequestDataVersion = false,
        )
    } answers {
        WithDataVersion(DUMMY_DATA_VERSION, queryResult)
    }
}

fun mockSiloTotalCountQuery(
    siloClient: SiloClient,
    dateBetweenFilter: DateBetween,
    queryResult: Stream<AggregationData>,
) {
    every {
        siloClient.sendQueryAndGetDataVersion<AggregationData>(
            match { query ->
                query.action == AGGREGATED_SILO_ACTION &&
                    query.filterExpression is And &&
                    query.filterExpression.children.count() == 2 &&
                    query.filterExpression.children.contains(True) &&
                    query.filterExpression.children.contains(dateBetweenFilter)
            },
            false,
        )
    } answers {
        WithDataVersion(DUMMY_DATA_VERSION, queryResult)
    }
}
