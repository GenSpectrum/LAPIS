package org.genspectrum.lapis.silo

import com.fasterxml.jackson.databind.ObjectMapper
import org.genspectrum.lapis.request.Order
import org.genspectrum.lapis.request.OrderByField
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class SiloQueryTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `Query is correctly serialized to JSON`() {
        val underTest = SiloQuery(SiloAction.aggregated(), StringEquals("theColumn", "theValue"))

        val result = objectMapper.writeValueAsString(underTest)

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
        assertThat(objectMapper.readTree(result), equalTo(objectMapper.readTree(expected)))
    }

    @ParameterizedTest(name = "Test SiloAction {1}")
    @MethodSource("getTestSiloActions")
    fun `SiloAction is correctly serialized to JSON`(underTest: SiloAction<*>, expected: String) {
        val result = objectMapper.writeValueAsString(underTest)

        assertThat(objectMapper.readTree(result), equalTo(objectMapper.readTree(expected)))
    }

    @ParameterizedTest(name = "Test SiloFilterExpression {1}")
    @MethodSource("getTestSiloFilterExpression")
    fun `SiloFilterExpressions is correctly serialized to JSON`(underTest: SiloFilterExpression, expected: String) {
        val result = objectMapper.writeValueAsString(underTest)

        assertThat(objectMapper.readTree(result), equalTo(objectMapper.readTree(expected)))
    }

    companion object {
        @JvmStatic
        fun getTestSiloActions() = listOf(
            Arguments.of(
                SiloAction.aggregated(),
                """
                {
                    "type": "Aggregated"
                }
                """,
            ),
            Arguments.of(
                SiloAction.aggregated(
                    listOf("field1", "field2"),
                    listOf(OrderByField("field3", Order.ASCENDING), OrderByField("field4", Order.DESCENDING)),
                    100,
                    50,
                ),
                """
                {
                    "type": "Aggregated",
                    "groupByFields": ["field1", "field2"],
                    "orderByFields": [
                        {"field": "field3", "order": "ascending"},
                        {"field": "field4", "order": "descending"}
                    ],
                    "limit": 100,
                    "offset": 50
                }
                """,
            ),
            Arguments.of(
                SiloAction.mutations(),
                """
                {
                    "type": "Mutations"
                }
                """,
            ),
            Arguments.of(
                SiloAction.mutations(
                    0.5,
                    listOf(OrderByField("field3", Order.ASCENDING), OrderByField("field4", Order.DESCENDING)),
                    100,
                    50,
                ),
                """
                {
                    "type": "Mutations",
                    "minProportion": 0.5,
                    "orderByFields": [
                        {"field": "field3", "order": "ascending"},
                        {"field": "field4", "order": "descending"}
                    ],
                    "limit": 100,
                    "offset": 50
                }
                """,
            ),
            Arguments.of(
                SiloAction.aminoAcidMutations(),
                """
                {
                    "type": "AminoAcidMutations"
                }
                """,
            ),
            Arguments.of(
                SiloAction.aminoAcidMutations(
                    0.5,
                    listOf(OrderByField("field3", Order.ASCENDING), OrderByField("field4", Order.DESCENDING)),
                    100,
                    50,
                ),
                """
                {
                    "type": "AminoAcidMutations",
                    "minProportion": 0.5,
                    "orderByFields": [
                        {"field": "field3", "order": "ascending"},
                        {"field": "field4", "order": "descending"}
                    ],
                    "limit": 100,
                    "offset": 50
                }
                """,
            ),
            Arguments.of(
                SiloAction.details(),
                """
                {
                    "type": "Details"
                }
                """,
            ),
            Arguments.of(
                SiloAction.details(
                    listOf("age", "pango_lineage"),
                    listOf(OrderByField("field3", Order.ASCENDING), OrderByField("field4", Order.DESCENDING)),
                    100,
                    50,
                ),
                """
                {
                    "type": "Details",
                    "fields": ["age", "pango_lineage"],
                    "orderByFields": [
                        {"field": "field3", "order": "ascending"},
                        {"field": "field4", "order": "descending"}
                    ],
                    "limit": 100,
                    "offset": 50
                }
                """,
            ),
            Arguments.of(
                SiloAction.nucleotideInsertions(),
                """
                {
                    "type": "Insertions"
                }
                """,
            ),
            Arguments.of(
                SiloAction.nucleotideInsertions(
                    listOf(OrderByField("field3", Order.ASCENDING), OrderByField("field4", Order.DESCENDING)),
                    100,
                    50,
                ),
                """
                {
                    "type": "Insertions",
                    "orderByFields": [
                        {"field": "field3", "order": "ascending"},
                        {"field": "field4", "order": "descending"}
                    ],
                    "limit": 100,
                    "offset": 50
                }
                """,
            ),
        )

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
                PangoLineageEquals("fieldName", "ABC", includeSublineages = false),
                """
                {
                    "type": "PangoLineage",
                    "column": "fieldName",
                    "value": "ABC",
                    "includeSublineages": false
                }
                """,
            ),
            Arguments.of(
                PangoLineageEquals("fieldName", "ABC", includeSublineages = true),
                """
                {
                    "type": "PangoLineage",
                    "column": "fieldName",
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
                NucleotideSymbolEquals(null, 1234, "A"),
                """
                {
                    "type": "NucleotideEquals",
                    "position": 1234,
                    "symbol": "A"
                }
                """,
            ),
            Arguments.of(
                NucleotideSymbolEquals("sequence name", 1234, "A"),
                """
                {
                    "type": "NucleotideEquals",
                    "sequenceName": "sequence name",
                    "position": 1234,
                    "symbol": "A"
                }
                """,
            ),
            Arguments.of(
                HasNucleotideMutation("sequence name", 1234),
                """
                {
                    "type": "HasNucleotideMutation",
                    "sequenceName": "sequence name",
                    "position": 1234
                }
                """,
            ),
            Arguments.of(
                HasNucleotideMutation(null, 1234),
                """
                {
                    "type": "HasNucleotideMutation",
                    "position": 1234
                }
                """,
            ),
            Arguments.of(
                AminoAcidSymbolEquals("gene name", 1234, "A"),
                """
                {
                    "type": "AminoAcidEquals",
                    "sequenceName": "gene name",
                    "position": 1234,
                    "symbol": "A"
                }
                """,
            ),
            Arguments.of(
                HasAminoAcidMutation("gene name", 1234),
                """
                {
                    "type": "HasAminoAcidMutation",
                    "sequenceName": "gene name",
                    "position": 1234
                }
                """,
            ),
            Arguments.of(
                DateBetween("fieldName", LocalDate.of(2021, 3, 31), LocalDate.of(2022, 6, 3)),
                """
                {
                    "type": "DateBetween",
                    "column": "fieldName",
                    "from": "2021-03-31",
                    "to": "2022-06-03"
                }
                """,
            ),
            Arguments.of(
                DateBetween("fieldName", null, LocalDate.of(2022, 6, 3)),
                """
                {
                    "type": "DateBetween",
                    "column": "fieldName",
                    "from": null,
                    "to": "2022-06-03"
                }
                """,
            ),
            Arguments.of(
                DateBetween("fieldName", LocalDate.of(2021, 3, 31), null),
                """
                {
                    "type": "DateBetween",
                    "column": "fieldName",
                    "from": "2021-03-31",
                    "to": null
                }
                """,
            ),
            Arguments.of(
                Not(StringEquals("theColumn", "theValue")),
                """
                {
                    "type": "Not",
                    "child": {
                        "type": "StringEquals",
                        "column": "theColumn",
                        "value": "theValue"
                    }
                }
                """,
            ),
            Arguments.of(
                Or(listOf(StringEquals("theColumn", "theValue"), StringEquals("theOtherColumn", "theOtherValue"))),
                """
                {
                    "type": "Or",
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
                Maybe(StringEquals("theColumn", "theValue")),
                """
                {
                    "type": "Maybe",
                    "child": {
                        "type": "StringEquals",
                        "column": "theColumn",
                        "value": "theValue"
                    }
                }
                """,
            ),
            Arguments.of(
                NOf(
                    2,
                    true,
                    listOf(
                        StringEquals("theColumn", "theValue"),
                        StringEquals("theOtherColumn", "theOtherValue"),
                    ),
                ),
                """
                {
                    "type": "N-Of",
                    "numberOfMatchers": 2,
                    "matchExactly": true,
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
        )
    }
}
