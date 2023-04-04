package org.genspectrum.lapis.model

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.genspectrum.lapis.response.AggregatedResponse
import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.DateBetween
import org.genspectrum.lapis.silo.NucleotideSymbolEquals
import org.genspectrum.lapis.silo.PangoLineageEquals
import org.genspectrum.lapis.silo.SiloAction
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.SiloFilterExpression
import org.genspectrum.lapis.silo.SiloQuery
import org.genspectrum.lapis.silo.StringEquals
import org.genspectrum.lapis.silo.True
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

class AggregatedModelTest {
    @MockK
    lateinit var siloClientMock: SiloClient
    private lateinit var underTest: AggregatedModel

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        every { siloClientMock.sendQuery(any<SiloQuery<AggregatedResponse>>()) } returns AggregatedResponse(0)
        underTest = AggregatedModel(siloClientMock)
    }

    @Test
    fun `given empty filter parameters then handleRequest should call the SiloClient with MatchAll SiloQuery`() {
        val filterParameter = emptyMap<String, String>()

        underTest.handleRequest(filterParameter)

        verify {
            siloClientMock.sendQuery(
                SiloQuery(SiloAction.aggregated(), True),
            )
        }
    }

    @ParameterizedTest(name = "FilterParameter: {0}, SiloQuery: {1}")
    @MethodSource("getFilterParametersWithExpectedSiloQuery")
    fun `given filter parameters then handleRequest should call the SiloClient with the corresponding SiloQuery`(
        filterParameter: Map<String, String>,
        expectedResult: SiloFilterExpression,
    ) {
        underTest.handleRequest(filterParameter)

        verify {
            siloClientMock.sendQuery(
                SiloQuery(
                    SiloAction.aggregated(),
                    expectedResult,
                ),
            )
        }
    }

    @Test
    fun `given invalid dateTo then handleRequest should throw an exception`() {
        val filterParameter = mapOf("dateTo" to "this is not a date")

        val exception = assertThrows<IllegalArgumentException> { underTest.handleRequest(filterParameter) }
        assertThat(exception.message, containsString("dateTo 'this is not a date' is not a valid date"))
    }

    @Test
    fun `given invalid dateFrom then handleRequest should throw an exception`() {
        val filterParameter = mapOf("dateFrom" to "this is not a date either")

        val exception = assertThrows<IllegalArgumentException> { underTest.handleRequest(filterParameter) }
        assertThat(exception.message, containsString("dateFrom 'this is not a date either' is not a valid date"))
    }

    @Test
    fun `given invalid Pango lineage ending with a dot then handleRequest should throw an exception`() {
        val filterParameter = mapOf("pangoLineage" to "A.1.2.")

        assertThrows<IllegalArgumentException> {
            underTest.handleRequest(filterParameter)
        }
    }

    @ParameterizedTest(name = "nucleotideMutations: {0}")
    @MethodSource("getNucleotideMutationWithWrongSyntax")
    fun `given nucleotideMutations with wrong syntax then handleRequest should throw an exception`(
        invalidMutation: String,
    ) {
        val filterParameter = mapOf("nucleotideMutations" to invalidMutation)

        assertThrows<IllegalArgumentException> {
            underTest.handleRequest(filterParameter)
        }
    }

    @ParameterizedTest(name = "nucleotideMutations: {0}")
    @MethodSource("getNucleotideMutationWithValidSyntax")
    fun `given valid mutations then handleRequest should call the SiloClient with the corresponding SiloQuery`(
        validMutation: String,
        expectedResult: SiloFilterExpression,
    ) {
        val filterParameter = mapOf("nucleotideMutations" to validMutation)
        underTest.handleRequest(filterParameter)

        verify {
            siloClientMock.sendQuery(
                SiloQuery(
                    SiloAction.aggregated(),
                    expectedResult,
                ),
            )
        }
    }

    companion object {
        @JvmStatic
        fun getFilterParametersWithExpectedSiloQuery() = listOf(
            Arguments.of(
                mapOf(
                    "some_metadata" to "ABC",
                    "other_metadata" to "def",
                ),
                And(
                    listOf(
                        StringEquals("some_metadata", "ABC"),
                        StringEquals("other_metadata", "def"),
                    ),
                ),
            ),
            Arguments.of(
                mapOf("pangoLineage" to "A.1.2.3"),
                And(listOf(PangoLineageEquals("A.1.2.3", includeSublineages = false))),
            ),
            Arguments.of(
                mapOf("pangoLineage" to "A.1.2.3*"),
                And(listOf(PangoLineageEquals("A.1.2.3", includeSublineages = true))),
            ),
            Arguments.of(
                mapOf("pangoLineage" to "A.1.2.3.*"),
                And(listOf(PangoLineageEquals("A.1.2.3", includeSublineages = true))),
            ),
            Arguments.of(
                mapOf(
                    "pangoLineage" to "A.1.2.3",
                    "some_metadata" to "ABC",
                    "other_metadata" to "DEF",
                ),
                And(
                    listOf(
                        PangoLineageEquals("A.1.2.3", includeSublineages = false),
                        StringEquals("some_metadata", "ABC"),
                        StringEquals("other_metadata", "DEF"),
                    ),
                ),
            ),
            Arguments.of(
                mapOf(
                    "nucleotideMutations" to "G123A",
                ),
                And(
                    listOf(
                        And(listOf(NucleotideSymbolEquals(123, "A"))),
                    ),
                ),
            ),
            Arguments.of(
                mapOf(
                    "nucleotideMutations" to "G123A,567T",
                ),
                And(
                    listOf(
                        And(
                            listOf(
                                NucleotideSymbolEquals(123, "A"),
                                NucleotideSymbolEquals(567, "T"),
                            ),
                        ),
                    ),
                ),
            ),
            Arguments.of(
                mapOf(
                    "dateTo" to "2021-06-03",
                ),
                And(listOf(DateBetween(from = null, to = LocalDate.of(2021, 6, 3)))),
            ),
            Arguments.of(
                mapOf(
                    "dateFrom" to "2021-03-28",
                ),
                And(listOf(DateBetween(from = LocalDate.of(2021, 3, 28), to = null))),
            ),
            Arguments.of(
                mapOf(
                    "dateFrom" to "2021-03-28",
                    "dateTo" to "2021-06-03",
                ),
                And(listOf(DateBetween(from = LocalDate.of(2021, 3, 28), to = LocalDate.of(2021, 6, 3)))),
            ),
            Arguments.of(
                mapOf(
                    "dateTo" to "2021-06-03",
                    "some_metadata" to "ABC",
                ),
                And(
                    listOf(
                        StringEquals("some_metadata", "ABC"),
                        DateBetween(from = null, to = LocalDate.of(2021, 6, 3)),
                    ),
                ),
            ),
        )

        @JvmStatic
        fun getNucleotideMutationWithWrongSyntax() = listOf(
            Arguments.of("AA123"),
            Arguments.of("123AA"),
            Arguments.of(""),
            Arguments.of("123X"),
            Arguments.of("AA123A"),
            Arguments.of("A123"),
            Arguments.of("123"),
        )

        @JvmStatic
        fun getNucleotideMutationWithValidSyntax() = listOf(
            Arguments.of(
                "G123A",
                And(
                    listOf(
                        And(listOf(NucleotideSymbolEquals(123, "A"))),
                    ),
                ),
            ),
            Arguments.of(
                "123A",
                And(
                    listOf(
                        And(listOf(NucleotideSymbolEquals(123, "A"))),
                    ),
                ),
            ),
            Arguments.of(
                "123.",
                And(
                    listOf(
                        And(listOf(NucleotideSymbolEquals(123, "."))),
                    ),
                ),
            ),
            Arguments.of(
                "123-",
                And(
                    listOf(
                        And(listOf(NucleotideSymbolEquals(123, "-"))),
                    ),
                ),
            ),
        )
    }
}
