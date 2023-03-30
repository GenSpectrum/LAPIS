package org.genspectrum.lapis.silo

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class SiloFilterTest {
    @Test
    fun `Query is correctly serialized to JSON`() {
        val underTest = SiloQuery(SiloAction.aggregated(), StringEquals("theColumn", "theValue"))

        val result = jacksonObjectMapper().writeValueAsString(underTest)

        val expected = """
            {
                "action": {
                    "type": "Aggregated"
                },
                "filterExpression": {
                    "type": "StringEquals",
                    "column": "theColumn",
                    "value": "theValue"
                }
            }
        """
        assertThat(jacksonObjectMapper().readTree(result), equalTo(jacksonObjectMapper().readTree(expected)))
    }

    @ParameterizedTest(name = "Test SiloFilterExpression {1}")
    @MethodSource("getTestSiloFilterExpression")
    fun `SiloFilterExpressions is correctly serialized to JSON`(underTest: SiloFilterExpression, expected: String) {
        val result = jacksonObjectMapper().writeValueAsString(underTest)

        assertThat(jacksonObjectMapper().readTree(result), equalTo(jacksonObjectMapper().readTree(expected)))
    }

    companion object {
        @JvmStatic
        fun getTestSiloFilterExpression() = listOf(
            Arguments.of(
                True,
                """
                {
                    "type": "True"
                }
                """,
            ),
            Arguments.of(
                StringEquals("theColumn", "theValue"),
                """
                {
                    "type": "StringEquals",
                    "column": "theColumn",
                    "value": "theValue"
                }
                """,
            ),
            Arguments.of(
                PangoLineageEquals("ABC", includeSublineages = false),
                """
                {
                    "type": "PangoLineage",
                    "value": "ABC",
                    "includeSublineages": false
                }
                """,
            ),
            Arguments.of(
                PangoLineageEquals("ABC", includeSublineages = true),
                """
                {
                    "type": "PangoLineage",
                    "value": "ABC",
                    "includeSublineages": true
                }
                """,
            ),
            Arguments.of(
                And(listOf(StringEquals("theColumn", "theValue"), StringEquals("theOtherColumn", "theOtherValue"))),
                """
                {
                    "type": "And",
                    "children": [
                        {
                        "type": "StringEquals",
                        "column": "theColumn",
                        "value": "theValue"
                        },
                        {
                        "type": "StringEquals",
                        "column": "theOtherColumn",
                        "value": "theOtherValue"
                        }
                    ]
                }
                """,
            ),
            Arguments.of(
                NucleotideSymbolEquals(1234, "A"),
                """
                {
                    "type": "NucleotideEquals",
                    "position": 1234,
                    "symbol": "A"
                }
                """,
            ),
        )
    }
}
