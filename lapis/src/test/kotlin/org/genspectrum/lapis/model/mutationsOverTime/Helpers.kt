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
import org.genspectrum.lapis.rhydb.AminoAcidSymbolEquals
import org.genspectrum.lapis.rhydb.And
import org.genspectrum.lapis.rhydb.DataVersion
import org.genspectrum.lapis.rhydb.DateBetween
import org.genspectrum.lapis.rhydb.NucleotideSymbolEquals
import org.genspectrum.lapis.rhydb.Or
import org.genspectrum.lapis.rhydb.RhyDbAction
import org.genspectrum.lapis.rhydb.RhyDbClient
import org.genspectrum.lapis.rhydb.RhyDbFilterExpression
import org.genspectrum.lapis.rhydb.True
import org.genspectrum.lapis.rhydb.WithDataVersion
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
val AGGREGATED_RHYDB_ACTION = RhyDbAction.aggregated(
    groupByFields = listOf(DUMMY_DATE_FIELD),
    orderByFields = OrderBySpec.EMPTY,
    limit = null,
    offset = null,
)

fun mockRhyDbCallInfo(
    rhyDbClient: RhyDbClient,
    dataVersion: DataVersion,
) {
    every {
        rhyDbClient.callInfo()
    } answers {
        dataVersion.dataVersion = DUMMY_DATA_VERSION
        InfoData(DUMMY_DATA_VERSION, null)
    }
}

fun mockRhyDbCountQuery(
    rhyDbClient: RhyDbClient,
    mutationFilter: RhyDbFilterExpression,
    dateBetweenFilter: DateBetween,
    queryResult: Stream<AggregationData>,
) {
    every {
        rhyDbClient.sendQueryAndGetDataVersion<AggregationData>(
            query = match { query ->
                query.action == AGGREGATED_RHYDB_ACTION &&
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

fun mockRhyDbNucleotideCoverageQuery(
    rhyDbClient: RhyDbClient,
    sequenceName: String?,
    position: Int,
    dateBetween: DateBetween,
    queryResult: Stream<AggregationData>,
) = mockRhyDbCoverageQuery(
    rhyDbClient,
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

fun mockRhyDbAminoAcidCoverageQuery(
    rhyDbClient: RhyDbClient,
    sequenceName: String?,
    position: Int,
    dateBetween: DateBetween,
    queryResult: Stream<AggregationData>,
) = mockRhyDbCoverageQuery(
    rhyDbClient,
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

fun mockRhyDbCoverageQuery(
    rhyDbClient: RhyDbClient,
    dateBetween: DateBetween,
    queryResult: Stream<AggregationData>,
    coverageFilterExpressionFn: (RhyDbFilterExpression) -> Boolean,
) {
    every {
        rhyDbClient.sendQueryAndGetDataVersion<AggregationData>(
            query = match { query ->
                query.action == AGGREGATED_RHYDB_ACTION &&
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

fun mockRhyDbTotalCountQuery(
    rhyDbClient: RhyDbClient,
    dateBetweenFilter: DateBetween,
    queryResult: Stream<AggregationData>,
) {
    every {
        rhyDbClient.sendQueryAndGetDataVersion<AggregationData>(
            match { query ->
                query.action == AGGREGATED_RHYDB_ACTION &&
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
