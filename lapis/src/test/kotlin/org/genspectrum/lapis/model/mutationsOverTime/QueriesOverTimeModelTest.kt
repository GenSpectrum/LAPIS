package org.genspectrum.lapis.model.mutationsOverTime

import com.fasterxml.jackson.databind.node.TextNode
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.ReferenceGenome
import org.genspectrum.lapis.model.AdvancedQueryFacade
import org.genspectrum.lapis.model.SiloFilterExpressionMapper
import org.genspectrum.lapis.request.QueryOverTimeItem
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.DataVersion
import org.genspectrum.lapis.silo.Not
import org.genspectrum.lapis.silo.NucleotideSymbolEquals
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.StringEquals
import org.genspectrum.lapis.silo.WithDataVersion
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.stream.Stream

private val DUMMY_QUERY_ITEM_1 = QueryOverTimeItem(
    displayLabel = "my label",
    countQuery = "main:1T & country=Switzerland",
    coverageQuery = "!main:1N",
)
private val DUMMY_QUERY_ITEM_2 = QueryOverTimeItem(
    countQuery = "other_segment:2G & country=Germany",
    coverageQuery = "!other_segment:2N",
)
private val DUMMY_FILTER_1 = And(
    StringEquals("country", "Switzerland"),
    NucleotideSymbolEquals("main", 1, "T"),
)
private val DUMMY_FILTER_COVERAGE_1 = Not(NucleotideSymbolEquals("main", 1, "N"))
private val DUMMY_FILTER_2 = And(
    StringEquals("country", "Germany"),
    NucleotideSymbolEquals("other_segment", 2, "G"),
)
private val DUMMY_FILTER_COVERAGE_2 = Not(NucleotideSymbolEquals("other_segment", 2, "N"))

@SpringBootTest
class QueriesOverTimeModelTest {
    @MockK
    private lateinit var siloQueryClient: SiloClient

    @Autowired
    private lateinit var siloFilterExpressionMapper: SiloFilterExpressionMapper

    @Autowired
    private lateinit var referenceGenome: ReferenceGenome

    @Autowired
    private lateinit var dataVersion: DataVersion

    @Autowired
    private lateinit var advancedQueryFacade: AdvancedQueryFacade

    private lateinit var underTest: MutationsOverTimeModel

    @Autowired
    private lateinit var config: DatabaseConfig

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        underTest = MutationsOverTimeModel(
            siloClient = siloQueryClient,
            siloFilterExpressionMapper = siloFilterExpressionMapper,
            referenceGenome = referenceGenome,
            dataVersion = dataVersion,
            advancedQueryFacade = advancedQueryFacade,
            config = config,
        )
    }

    @Test
    fun `GIVEN an empty list of queries THEN it returns an empty list`() {
        mockSiloCallInfo(siloQueryClient, dataVersion)
        val dateRanges = listOf(DUMMY_DATE_RANGE1, DUMMY_DATE_RANGE2)
        val result = underTest.evaluateQueriesOverTime(
            queries = emptyList(),
            dateRanges = dateRanges,
            lapisFilter = DUMMY_LAPIS_FILTER,
            dateField = DUMMY_DATE_FIELD,
        )

        assertThat(result.mutations, equalTo(emptyList()))
        assertThat(result.data, equalTo(emptyList()))
        assertThat(result.dateRanges, equalTo(dateRanges))
        assertThat(result.totalCountsByDateRange, equalTo(emptyList()))
        assertThat(dataVersion.dataVersion, notNullValue())
    }

    @Test
    fun `GIVEN an empty list of date ranges THEN it returns an empty list`() {
        mockSiloCallInfo(siloQueryClient, dataVersion)
        val result = underTest.evaluateQueriesOverTime(
            queries = listOf(QueryOverTimeItem("label", "123", "!123")),
            dateRanges = emptyList(),
            lapisFilter = DUMMY_LAPIS_FILTER,
            dateField = DUMMY_DATE_FIELD,
        )

        assertThat(result.mutations, equalTo(listOf("label")))
        assertThat(result.data, equalTo(emptyList()))
        assertThat(result.dateRanges, equalTo(emptyList()))
        assertThat(result.totalCountsByDateRange, equalTo(emptyList()))
        assertThat(dataVersion.dataVersion, notNullValue())
    }

    private fun commonSetup() {
        mockSiloCallInfo(siloQueryClient, dataVersion)
        mockSiloCountQuery(
            siloClient = siloQueryClient,
            mutationFilter = DUMMY_FILTER_1,
            dateBetweenFilter = DUMMY_DATE_BETWEEN_ALL,
            queryResult = Stream.of(
                AggregationData(1, fields = mapOf("date" to TextNode("2021-06-01"))),
                AggregationData(2, fields = mapOf("date" to TextNode("2022-06-01"))),
            ),
        )
        mockSiloCountQuery(
            siloClient = siloQueryClient,
            mutationFilter = DUMMY_FILTER_2,
            dateBetweenFilter = DUMMY_DATE_BETWEEN_ALL,
            queryResult = Stream.of(
                AggregationData(3, fields = mapOf("date" to TextNode("2021-06-01"))),
                AggregationData(2, fields = mapOf("date" to TextNode("2022-06-01"))),
                AggregationData(2, fields = mapOf("date" to TextNode("2022-07-01"))),
            ),
        )
        mockSiloCoverageQuery(
            siloClient = siloQueryClient,
            dateBetween = DUMMY_DATE_BETWEEN_ALL,
            queryResult = Stream.of(
                AggregationData(5, fields = mapOf("date" to TextNode("2021-06-01"))),
                AggregationData(6, fields = mapOf("date" to TextNode("2022-06-01"))),
            ),
            coverageFilterExpressionFn = { it == DUMMY_FILTER_COVERAGE_1 },
        )
        mockSiloCoverageQuery(
            siloClient = siloQueryClient,
            dateBetween = DUMMY_DATE_BETWEEN_ALL,
            queryResult = Stream.of(
                AggregationData(0, fields = mapOf("date" to TextNode("2021-06-01"))),
                AggregationData(1, fields = mapOf("date" to TextNode("2022-06-01"))),
                AggregationData(1, fields = mapOf("date" to TextNode("2022-07-01"))),
            ),
            coverageFilterExpressionFn = { it == DUMMY_FILTER_COVERAGE_2 },
        )
        mockSiloTotalCountQuery(
            siloQueryClient,
            DUMMY_DATE_BETWEEN_ALL,
            queryResult = Stream.of(
                AggregationData(10, fields = mapOf("date" to TextNode("2021-06-01"))),
                AggregationData(11, fields = mapOf("date" to TextNode("2022-06-01"))),
                AggregationData(12, fields = mapOf("date" to TextNode("2022-07-01"))),
            ),
        )
    }

    @Test
    fun `GIVEN a list of queries and date ranges THEN it returns the count and coverage data`() {
        commonSetup()

        val queries = listOf(DUMMY_QUERY_ITEM_1, DUMMY_QUERY_ITEM_2)
        val dateRanges = listOf(DUMMY_DATE_RANGE1, DUMMY_DATE_RANGE2)

        val result = underTest.evaluateQueriesOverTime(
            queries = queries,
            lapisFilter = DUMMY_LAPIS_FILTER,
            dateField = DUMMY_DATE_FIELD,
            dateRanges = dateRanges,
        )

        assertThat(result.mutations, equalTo(listOf("my label", "other_segment:2G & country=Germany")))
        assertThat(result.dateRanges, equalTo(dateRanges))
        assertThat(
            result.data,
            equalTo(
                listOf(
                    listOf(MutationsOverTimeCell(1, 5), MutationsOverTimeCell(2, 6)),
                    listOf(MutationsOverTimeCell(3, 0), MutationsOverTimeCell(4, 2)),
                ),
            ),
        )
        assertThat(
            result.totalCountsByDateRange,
            equalTo(listOf(10, 23)),
        )
        assertThat(dataVersion.dataVersion, notNullValue())
    }

    @Test
    fun `GIVEN a list of queries and date ranges in reverse order THEN the order is preserved`() {
        commonSetup()

        val queriesReversed = listOf(DUMMY_QUERY_ITEM_2, DUMMY_QUERY_ITEM_1)
        val dateRangesReversed = listOf(DUMMY_DATE_RANGE2, DUMMY_DATE_RANGE1)

        val result = underTest.evaluateQueriesOverTime(
            queries = queriesReversed,
            lapisFilter = DUMMY_LAPIS_FILTER,
            dateField = DUMMY_DATE_FIELD,
            dateRanges = dateRangesReversed,
        )

        assertThat(result.mutations, equalTo(listOf("other_segment:2G & country=Germany", "my label")))
        assertThat(result.dateRanges, equalTo(dateRangesReversed))
    }

    @Test
    fun `GIVEN a list of queries and date ranges and no data for a mutation THEN it returns zero`() {
        mockSiloCountQuery(siloQueryClient, DUMMY_FILTER_1, DUMMY_DATE_BETWEEN_ALL, Stream.empty())
        mockSiloCoverageQuery(
            siloClient = siloQueryClient,
            dateBetween = DUMMY_DATE_BETWEEN_ALL,
            queryResult = Stream.empty(),
            coverageFilterExpressionFn = { it == DUMMY_FILTER_COVERAGE_1 },
        )
        mockSiloTotalCountQuery(siloQueryClient, DUMMY_DATE_BETWEEN_ALL, Stream.empty())

        val queries = listOf(DUMMY_QUERY_ITEM_1)
        val dateRanges = listOf(DUMMY_DATE_RANGE1, DUMMY_DATE_RANGE2)

        val result = underTest.evaluateQueriesOverTime(
            queries = queries,
            lapisFilter = DUMMY_LAPIS_FILTER,
            dateField = DUMMY_DATE_FIELD,
            dateRanges = dateRanges,
        )

        assertThat(result.mutations, equalTo(listOf("my label")))
        assertThat(result.dateRanges, equalTo(dateRanges))
        assertThat(
            result.data,
            equalTo(
                listOf(
                    listOf(MutationsOverTimeCell(0, 0), MutationsOverTimeCell(0, 0)),
                ),
            ),
        )
        assertThat(
            result.totalCountsByDateRange,
            equalTo(listOf(0, 0)),
        )
    }

    @Test
    fun `GIVEN one data version change THEN it succeeds`() {
        var callCount = 0
        every {
            siloQueryClient.sendQueryAndGetDataVersion<AggregationData>(any(), false, any())
        } answers {
            val version = if (callCount++ == 0) "1" else "2"
            WithDataVersion(
                version,
                Stream.of(
                    AggregationData(1, fields = mapOf(DUMMY_DATE_FIELD to TextNode("2021-06-01"))),
                ),
            )
        }

        val queries = listOf(DUMMY_QUERY_ITEM_1)
        val dateRanges = listOf(DUMMY_DATE_RANGE1)

        val result = underTest.evaluateQueriesOverTime(
            queries = queries,
            lapisFilter = DUMMY_LAPIS_FILTER,
            dateField = DUMMY_DATE_FIELD,
            dateRanges = dateRanges,
        )

        assertThat(result.data[0][0].count, equalTo(1))
    }

    @Test
    fun `GIVEN more than once data version change THEN it throws`() {
        var callCount = 0
        every {
            siloQueryClient.sendQueryAndGetDataVersion<AggregationData>(any(), false, any())
        } answers {
            val version = (callCount++).toString()
            WithDataVersion(
                version,
                Stream.of(
                    AggregationData(1, fields = mapOf(DUMMY_DATE_FIELD to TextNode("2021-06-01"))),
                ),
            )
        }

        val queries = listOf(DUMMY_QUERY_ITEM_1)
        val dateRanges = listOf(DUMMY_DATE_RANGE1)

        val exception = assertThrows<RuntimeException> {
            underTest.evaluateQueriesOverTime(
                queries = queries,
                lapisFilter = DUMMY_LAPIS_FILTER,
                dateField = DUMMY_DATE_FIELD,
                dateRanges = dateRanges,
            )
        }
        assertThat(exception.message, containsString("data has been updated multiple times"))
    }
}
