package org.genspectrum.lapis.model

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.genspectrum.lapis.response.AggregatedResponse
import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.NucleotideSymbolEquals
import org.genspectrum.lapis.silo.PangoLineageEquals
import org.genspectrum.lapis.silo.SiloAction
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.SiloFilterExpression
import org.genspectrum.lapis.silo.SiloQuery
import org.genspectrum.lapis.silo.StringEquals
import org.genspectrum.lapis.silo.True
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

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
