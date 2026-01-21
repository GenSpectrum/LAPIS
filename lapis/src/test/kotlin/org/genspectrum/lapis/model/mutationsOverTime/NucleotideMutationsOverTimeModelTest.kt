package org.genspectrum.lapis.model.mutationsOverTime

import com.fasterxml.jackson.databind.node.TextNode
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.ReferenceGenome
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.model.AdvancedQueryFacade
import org.genspectrum.lapis.model.SiloFilterExpressionMapper
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.silo.DataVersion
import org.genspectrum.lapis.silo.NucleotideSymbolEquals
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.WithDataVersion
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.stream.Stream

private val DUMMY_MUTATION1 = NucleotideMutation(null, 1, "T")
private val DUMMY_MUTATION2 = NucleotideMutation(null, 2, "G")
private val DUMMY_MUTATION_EQUALS1 = NucleotideSymbolEquals(null, 1, "T")
private val DUMMY_MUTATION_EQUALS2 = NucleotideSymbolEquals(null, 2, "G")

@SpringBootTest
class NucleotideMutationsOverTimeModelTest {
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

    private lateinit var underTest: QueriesOverTimeModel

    @Autowired
    private lateinit var config: DatabaseConfig

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        underTest = QueriesOverTimeModel(
            siloClient = siloQueryClient,
            siloFilterExpressionMapper = siloFilterExpressionMapper,
            referenceGenome = referenceGenome,
            dataVersion = dataVersion,
            advancedQueryFacade = advancedQueryFacade,
            config = config,
        )
    }

    @Test
    fun `given an empty list of mutations, then it returns an empty list`() {
        mockSiloCallInfo(siloQueryClient, dataVersion)
        val mutations = emptyList<NucleotideMutation>()
        val dateRanges = listOf(DUMMY_DATE_RANGE1, DUMMY_DATE_RANGE2)
        val result = underTest.evaluateNucleotideMutations(
            mutations = mutations,
            dateRanges = dateRanges,
            lapisFilter = DUMMY_LAPIS_FILTER,
            dateField = DUMMY_DATE_FIELD,
        )

        assertThat(result.mutations, equalTo(emptyList()))
        assertThat(result.data, equalTo(emptyList()))
        assertThat(result.dateRanges, equalTo(dateRanges))
        assertThat(result.totalCountsByDateRange, equalTo(emptyList()))
        assertThat(result.overallStatisticsByMutation, equalTo(emptyList()))
        assertThat(dataVersion.dataVersion, notNullValue())
    }

    @Test
    fun `given an empty list of date ranges, then it returns an empty list`() {
        mockSiloCallInfo(siloQueryClient, dataVersion)
        val mutations = listOf(DUMMY_MUTATION1, DUMMY_MUTATION2)
        val dateRanges = emptyList<DateRange>()
        val result = underTest.evaluateNucleotideMutations(
            mutations = mutations,
            dateRanges = dateRanges,
            lapisFilter = DUMMY_LAPIS_FILTER,
            dateField = DUMMY_DATE_FIELD,
        )

        assertThat(result.mutations, equalTo(mutations.map { it.toString(referenceGenome) }))
        assertThat(result.data, equalTo(emptyList()))
        assertThat(result.dateRanges, equalTo(emptyList()))
        assertThat(result.totalCountsByDateRange, equalTo(emptyList()))
        assertThat(result.overallStatisticsByMutation, equalTo(emptyList()))
        assertThat(dataVersion.dataVersion, notNullValue())
    }

    private fun commonSetup() {
        mockSiloCallInfo(siloQueryClient, dataVersion)
        mockSiloCountQuery(
            siloQueryClient,
            DUMMY_MUTATION_EQUALS1,
            DUMMY_DATE_BETWEEN_ALL,
            Stream.of(
                AggregationData(1, fields = mapOf("date" to TextNode("2021-06-01"))),
                AggregationData(2, fields = mapOf("date" to TextNode("2022-06-01"))),
            ),
        )
        mockSiloCountQuery(
            siloQueryClient,
            DUMMY_MUTATION_EQUALS2,
            DUMMY_DATE_BETWEEN_ALL,
            Stream.of(
                AggregationData(3, fields = mapOf("date" to TextNode("2021-06-01"))),
                AggregationData(2, fields = mapOf("date" to TextNode("2022-06-01"))),
                AggregationData(2, fields = mapOf("date" to TextNode("2022-07-01"))),
            ),
        )
        mockSiloNucleotideCoverageQuery(
            siloQueryClient,
            null,
            1,
            DUMMY_DATE_BETWEEN_ALL,
            Stream.of(
                AggregationData(5, fields = mapOf("date" to TextNode("2021-06-01"))),
                AggregationData(6, fields = mapOf("date" to TextNode("2022-06-01"))),
            ),
        )
        mockSiloNucleotideCoverageQuery(
            siloQueryClient,
            null,
            2,
            DUMMY_DATE_BETWEEN_ALL,
            Stream.of(
                AggregationData(0, fields = mapOf("date" to TextNode("2021-06-01"))),
                AggregationData(1, fields = mapOf("date" to TextNode("2022-06-01"))),
                AggregationData(1, fields = mapOf("date" to TextNode("2022-07-01"))),
            ),
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
    fun `given a list of mutations and date ranges, then it returns the count and coverage data`() {
        commonSetup()

        val mutations = listOf(DUMMY_MUTATION1, DUMMY_MUTATION2)
        val dateRanges = listOf(DUMMY_DATE_RANGE1, DUMMY_DATE_RANGE2)

        val result = underTest.evaluateNucleotideMutations(
            mutations = mutations,
            lapisFilter = DUMMY_LAPIS_FILTER,
            dateField = DUMMY_DATE_FIELD,
            dateRanges = dateRanges,
        )

        assertThat(result.mutations, equalTo(mutations.map { it.toString(referenceGenome) }))
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
        assertThat(
            result.overallStatisticsByMutation,
            equalTo(
                listOf(
                    OverallStatistics(count = 3, coverage = 11, proportion = 3.0 / 11.0),
                    OverallStatistics(count = 7, coverage = 2, proportion = 7.0 / 2.0),
                ),
            ),
        )
        assertThat(dataVersion.dataVersion, notNullValue())
    }

    @Test
    fun `given a list of mutations and date ranges in reverse order, then the order is preserved`() {
        commonSetup()

        val mutationsReversed = listOf(DUMMY_MUTATION2, DUMMY_MUTATION1)
        val dateRangesReversed = listOf(DUMMY_DATE_RANGE2, DUMMY_DATE_RANGE1)

        val result = underTest.evaluateNucleotideMutations(
            mutations = mutationsReversed,
            lapisFilter = DUMMY_LAPIS_FILTER,
            dateField = DUMMY_DATE_FIELD,
            dateRanges = dateRangesReversed,
        )

        assertThat(result.mutations, equalTo(mutationsReversed.map { it.toString(referenceGenome) }))
        assertThat(result.dateRanges, equalTo(dateRangesReversed))
    }

    @Test
    fun `given a list of mutations and date ranges and no data for a mutation, then it returns zero`() {
        mockSiloCountQuery(siloQueryClient, DUMMY_MUTATION_EQUALS1, DUMMY_DATE_BETWEEN_ALL, Stream.empty())
        mockSiloNucleotideCoverageQuery(siloQueryClient, null, 1, DUMMY_DATE_BETWEEN_ALL, Stream.empty())
        mockSiloTotalCountQuery(siloQueryClient, DUMMY_DATE_BETWEEN_ALL, Stream.empty())

        val mutations = listOf(DUMMY_MUTATION1)
        val dateRanges = listOf(DUMMY_DATE_RANGE1, DUMMY_DATE_RANGE2)

        val result = underTest.evaluateNucleotideMutations(
            mutations = mutations,
            lapisFilter = DUMMY_LAPIS_FILTER,
            dateField = DUMMY_DATE_FIELD,
            dateRanges = dateRanges,
        )

        assertThat(result.mutations, equalTo(mutations.map { it.toString(referenceGenome) }))
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
        assertThat(
            result.overallStatisticsByMutation,
            equalTo(
                listOf(
                    OverallStatistics(count = 0, coverage = 0, proportion = null),
                ),
            ),
        )
    }

    @Test
    fun `given one data version change, then it succeeds`() {
        var callCount = 0
        every {
            siloQueryClient.sendQueryAndGetDataVersion<AggregationData>(any(), false)
        } answers {
            val version = if (callCount++ == 0) "1" else "2"
            WithDataVersion(
                version,
                Stream.of(
                    AggregationData(1, fields = mapOf(DUMMY_DATE_FIELD to TextNode("2021-06-01"))),
                ),
            )
        }

        val mutations = listOf(DUMMY_MUTATION1)
        val dateRanges = listOf(DUMMY_DATE_RANGE1)

        val result = underTest.evaluateNucleotideMutations(
            mutations = mutations,
            lapisFilter = DUMMY_LAPIS_FILTER,
            dateField = DUMMY_DATE_FIELD,
            dateRanges = dateRanges,
        )

        assertThat(result.data[0][0].count, equalTo(1))
    }

    @Test
    fun `given more than once data version change, then it throws`() {
        var callCount = 0
        every {
            siloQueryClient.sendQueryAndGetDataVersion<AggregationData>(any(), false)
        } answers {
            val version = (callCount++).toString()
            WithDataVersion(
                version,
                Stream.of(
                    AggregationData(1, fields = mapOf(DUMMY_DATE_FIELD to TextNode("2021-06-01"))),
                ),
            )
        }

        val mutations = listOf(DUMMY_MUTATION1)
        val dateRanges = listOf(DUMMY_DATE_RANGE1)

        val exception = assertThrows<RuntimeException> {
            underTest.evaluateNucleotideMutations(
                mutations = mutations,
                lapisFilter = DUMMY_LAPIS_FILTER,
                dateField = DUMMY_DATE_FIELD,
                dateRanges = dateRanges,
            )
        }
        assertThat(exception.message, containsString("data has been updated multiple times"))
    }

    @Test
    fun `given a maybe() query, then it throws`() {
        val mutations = listOf(NucleotideMutation(null, 1, "T", maybe = true))
        val dateRanges = listOf(DUMMY_DATE_RANGE1)

        val exception = assertThrows<BadRequestException> {
            underTest.evaluateNucleotideMutations(
                mutations = mutations,
                lapisFilter = DUMMY_LAPIS_FILTER,
                dateField = DUMMY_DATE_FIELD,
                dateRanges = dateRanges,
            )
        }
        assertThat(exception.message, `is`("Invalid mutation in includeMutations – maybe() is not allowed: maybe(A1T)"))
    }
}
