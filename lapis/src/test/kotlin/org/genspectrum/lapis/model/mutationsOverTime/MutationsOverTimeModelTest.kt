package org.genspectrum.lapis.model.mutationsOverTime

import com.fasterxml.jackson.databind.node.TextNode
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.genspectrum.lapis.config.ReferenceGenome
import org.genspectrum.lapis.model.SiloFilterExpressionMapper
import org.genspectrum.lapis.request.AminoAcidInsertion
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.BaseSequenceFilters
import org.genspectrum.lapis.request.NucleotideInsertion
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.SequenceFilters
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.DataVersion
import org.genspectrum.lapis.silo.DateBetween
import org.genspectrum.lapis.silo.NucleotideSymbolEquals
import org.genspectrum.lapis.silo.Or
import org.genspectrum.lapis.silo.SiloAction
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.WithDataVersion
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import java.util.stream.Stream

private data class TestLapisFilter(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aminoAcidMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
) : BaseSequenceFilters

const val DUMMY_DATE_FIELD = "date"
private val DUMMY_LAPIS_FILTER = TestLapisFilter(emptyMap(), emptyList(), emptyList(), emptyList(), emptyList())
const val DUMMY_DATA_VERSION = "1750941114"

@SpringBootTest
class MutationsOverTimeModelTest {
    @MockK
    private lateinit var siloQueryClient: SiloClient

    @Autowired
    private lateinit var siloFilterExpressionMapper: SiloFilterExpressionMapper

    @Autowired
    private lateinit var referenceGenome: ReferenceGenome

    @Autowired
    private lateinit var dataVersion: DataVersion

    private lateinit var underTest: MutationsOverTimeModel

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        underTest = MutationsOverTimeModel(siloQueryClient, siloFilterExpressionMapper, referenceGenome, dataVersion)
    }

    @Test
    fun `given an empty list of mutations, then it returns an empty list`() {
        val mutations = emptyList<NucleotideMutation>()
        val dateRanges = listOf(
            DateRange(dateFrom = LocalDate.parse("2021-01-01"), dateTo = LocalDate.parse("2021-12-31")),
            DateRange(dateFrom = LocalDate.parse("2022-01-01"), dateTo = LocalDate.parse("2022-12-31")),
        )
        val result = underTest.evaluate(
            mutations = mutations,
            dateRanges = dateRanges,
            lapisFilter = DUMMY_LAPIS_FILTER,
            dateField = DUMMY_DATE_FIELD,
        )

        assertThat(result.rowLabels, equalTo(emptyList()))
        assertThat(result.data, equalTo(emptyList()))
        assertThat(result.columnLabels, equalTo(dateRanges))
    }

    @Test
    fun `given an empty list of date ranges, then it returns an empty list`() {
        val mutations = listOf(
            NucleotideMutation(sequenceName = null, position = 1, symbol = "T"),
            NucleotideMutation(sequenceName = null, position = 2, symbol = "G"),
        )
        val dateRanges = emptyList<DateRange>()
        val result = underTest.evaluate(
            mutations = mutations,
            dateRanges = dateRanges,
            lapisFilter = DUMMY_LAPIS_FILTER,
            dateField = DUMMY_DATE_FIELD,
        )

        assertThat(result.rowLabels, equalTo(mutations.map { it.toString(referenceGenome) }))
        assertThat(result.data, equalTo(emptyList()))
        assertThat(result.columnLabels, equalTo(emptyList()))
    }

    @Test
    fun `given a list of mutations and date ranges, then it returns the count and coverage data`() {
        every {
            siloQueryClient.sendQueryAndGetDataVersion<AggregationData>(
                match { query ->
                    query.action == SiloAction.aggregated(
                        listOf(DUMMY_DATE_FIELD),
                        emptyList(),
                        null,
                        null,
                    ) &&
                        query.filterExpression is And &&
                        query.filterExpression.children.any {
                            it is NucleotideSymbolEquals &&
                                it.sequenceName == null &&
                                it.position == 1 &&
                                it.symbol == "T"
                        } &&
                        query.filterExpression.children.any {
                            it is DateBetween &&
                                it.column == DUMMY_DATE_FIELD &&
                                it.from == LocalDate.parse("2021-01-01") &&
                                it.to == LocalDate.parse("2022-12-31")
                        }
                },
                false,
            )
        } answers {
            WithDataVersion(
                DUMMY_DATA_VERSION,
                Stream.of(
                    AggregationData(1, fields = mapOf("date" to TextNode("2021-06-01"))),
                    AggregationData(2, fields = mapOf("date" to TextNode("2022-06-01"))),
                ),
            )
        }

        every {
            siloQueryClient.sendQueryAndGetDataVersion<AggregationData>(
                match { query ->
                    query.action == SiloAction.aggregated(
                        listOf(DUMMY_DATE_FIELD),
                        emptyList(),
                        null,
                        null,
                    ) &&
                        query.filterExpression is And &&
                        query.filterExpression.children.any {
                            it is NucleotideSymbolEquals &&
                                it.sequenceName == null &&
                                it.position == 2 &&
                                it.symbol == "G"
                        } &&
                        query.filterExpression.children.any {
                            it is DateBetween &&
                                it.column == DUMMY_DATE_FIELD &&
                                it.from == LocalDate.parse("2021-01-01") &&
                                it.to == LocalDate.parse("2022-12-31")
                        }
                },
                false,
            )
        } answers {
            WithDataVersion(
                DUMMY_DATA_VERSION,
                Stream.of(
                    AggregationData(3, fields = mapOf("date" to TextNode("2021-06-01"))),
                    AggregationData(2, fields = mapOf("date" to TextNode("2022-06-01"))),
                    AggregationData(2, fields = mapOf("date" to TextNode("2022-07-01"))),
                ),
            )
        }

        every {
            siloQueryClient.sendQueryAndGetDataVersion<AggregationData>(
                match { query ->
                    query.action == SiloAction.aggregated(
                        listOf(DUMMY_DATE_FIELD),
                        emptyList(),
                        null,
                        null,
                    ) &&
                        query.filterExpression is And &&
                        query.filterExpression.children.any {
                            it is Or &&
                                (it).children.all { child ->
                                    child is NucleotideSymbolEquals &&
                                        child.sequenceName == null &&
                                        child.position == 1 &&
                                        child.symbol in listOf("A", "C", "T", "G")
                                }
                        } &&
                        query.filterExpression.children.any {
                            it is DateBetween &&
                                it.column == DUMMY_DATE_FIELD &&
                                it.from == LocalDate.parse("2021-01-01") &&
                                it.to == LocalDate.parse("2022-12-31")
                        }
                },
                false,
            )
        } answers {
            WithDataVersion(
                DUMMY_DATA_VERSION,
                Stream.of(
                    AggregationData(5, fields = mapOf("date" to TextNode("2021-06-01"))),
                    AggregationData(6, fields = mapOf("date" to TextNode("2022-06-01"))),
                ),
            )
        }

        every {
            siloQueryClient.sendQueryAndGetDataVersion<AggregationData>(
                match { query ->
                    query.action == SiloAction.aggregated(
                        listOf(DUMMY_DATE_FIELD),
                        emptyList(),
                        null,
                        null,
                    ) &&
                        query.filterExpression is And &&
                        query.filterExpression.children.any {
                            it is Or &&
                                (it).children.all { child ->
                                    child is NucleotideSymbolEquals &&
                                        child.sequenceName == null &&
                                        child.position == 2 &&
                                        child.symbol in listOf("A", "C", "T", "G")
                                }
                        } &&
                        query.filterExpression.children.any {
                            it is DateBetween &&
                                it.column == DUMMY_DATE_FIELD &&
                                it.from == LocalDate.parse("2021-01-01") &&
                                it.to == LocalDate.parse("2022-12-31")
                        }
                },
                false,
            )
        } answers {
            WithDataVersion(
                DUMMY_DATA_VERSION,
                Stream.of(
                    AggregationData(0, fields = mapOf("date" to TextNode("2021-06-01"))),
                    AggregationData(1, fields = mapOf("date" to TextNode("2022-06-01"))),
                    AggregationData(1, fields = mapOf("date" to TextNode("2022-07-01"))),
                ),
            )
        }

        val mutations = listOf(
            NucleotideMutation(sequenceName = null, position = 1, symbol = "T"),
            NucleotideMutation(sequenceName = null, position = 2, symbol = "G"),
        )
        val dateRanges = listOf(
            DateRange(dateFrom = LocalDate.parse("2021-01-01"), dateTo = LocalDate.parse("2021-12-31")),
            DateRange(dateFrom = LocalDate.parse("2022-01-01"), dateTo = LocalDate.parse("2022-12-31")),
        )

        val result = underTest.evaluate(
            mutations = mutations,
            lapisFilter = DUMMY_LAPIS_FILTER,
            dateField = DUMMY_DATE_FIELD,
            dateRanges = dateRanges,
        )

        assertThat(result.rowLabels, equalTo(mutations.map { it.toString(referenceGenome) }))
        assertThat(result.columnLabels, equalTo(dateRanges))
        assertThat(
            result.data,
            equalTo(
                listOf(
                    listOf(
                        MutationsOverTimeCell(count = 1, coverage = 5),
                        MutationsOverTimeCell(count = 2, coverage = 6),
                    ),
                    listOf(
                        MutationsOverTimeCell(count = 3, coverage = 0),
                        MutationsOverTimeCell(count = 4, coverage = 2),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `given a list of mutations and date ranges and no data for a mutation, then it returns zero`() {
        every {
            siloQueryClient.sendQueryAndGetDataVersion<AggregationData>(
                match { query ->
                    query.action == SiloAction.aggregated(
                        listOf(DUMMY_DATE_FIELD),
                        emptyList(),
                        null,
                        null,
                    ) &&
                        query.filterExpression is And &&
                        query.filterExpression.children.any {
                            it is NucleotideSymbolEquals &&
                                it.sequenceName == null &&
                                it.position == 1 &&
                                it.symbol == "T"
                        } &&
                        query.filterExpression.children.any {
                            it is DateBetween &&
                                it.column == DUMMY_DATE_FIELD &&
                                it.from == LocalDate.parse("2021-01-01") &&
                                it.to == LocalDate.parse("2022-12-31")
                        }
                },
                false,
            )
        } answers {
            WithDataVersion(
                DUMMY_DATA_VERSION,
                Stream.empty(),
            )
        }

        every {
            siloQueryClient.sendQueryAndGetDataVersion<AggregationData>(
                match { query ->
                    query.action == SiloAction.aggregated(
                        listOf(DUMMY_DATE_FIELD),
                        emptyList(),
                        null,
                        null,
                    ) &&
                        query.filterExpression is And &&
                        query.filterExpression.children.any {
                            it is Or &&
                                (it).children.all { child ->
                                    child is NucleotideSymbolEquals &&
                                        child.sequenceName == null &&
                                        child.position == 1 &&
                                        child.symbol in listOf("A", "C", "T", "G")
                                }
                        } &&
                        query.filterExpression.children.any {
                            it is DateBetween &&
                                it.column == DUMMY_DATE_FIELD &&
                                it.from == LocalDate.parse("2021-01-01") &&
                                it.to == LocalDate.parse("2022-12-31")
                        }
                },
                false,
            )
        } answers {
            WithDataVersion(
                DUMMY_DATA_VERSION,
                Stream.empty(),
            )
        }

        val mutations = listOf(
            NucleotideMutation(sequenceName = null, position = 1, symbol = "T"),
        )
        val dateRanges = listOf(
            DateRange(dateFrom = LocalDate.parse("2021-01-01"), dateTo = LocalDate.parse("2021-12-31")),
            DateRange(dateFrom = LocalDate.parse("2022-01-01"), dateTo = LocalDate.parse("2022-12-31")),
        )

        val result = underTest.evaluate(
            mutations = mutations,
            lapisFilter = DUMMY_LAPIS_FILTER,
            dateField = DUMMY_DATE_FIELD,
            dateRanges = dateRanges,
        )

        assertThat(result.rowLabels, equalTo(mutations.map { it.toString(referenceGenome) }))
        assertThat(result.columnLabels, equalTo(dateRanges))
        assertThat(
            result.data,
            equalTo(
                listOf(
                    listOf(
                        MutationsOverTimeCell(count = 0, coverage = 0),
                        MutationsOverTimeCell(count = 0, coverage = 0),
                    ),
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

        val mutations = listOf(NucleotideMutation(null, 1, "T"))
        val dateRanges = listOf(DateRange(LocalDate.parse("2021-01-01"), LocalDate.parse("2021-12-31")))

        val result = underTest.evaluate(
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

        val mutations = listOf(NucleotideMutation(null, 1, "T"))
        val dateRanges = listOf(DateRange(LocalDate.parse("2021-01-01"), LocalDate.parse("2021-12-31")))

        val exception = assertThrows<RuntimeException> {
            underTest.evaluate(
                mutations = mutations,
                lapisFilter = DUMMY_LAPIS_FILTER,
                dateField = DUMMY_DATE_FIELD,
                dateRanges = dateRanges,
            )
        }
        assertThat(exception.message, containsString("data has been updated multiple times"))
    }
}
