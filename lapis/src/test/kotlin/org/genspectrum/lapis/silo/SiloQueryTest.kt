package org.genspectrum.lapis.silo

import com.fasterxml.jackson.databind.ObjectMapper
import org.genspectrum.lapis.request.Order
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.toOrderBySpec
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
                    "type": "Aggregated",
                    "randomize": false
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
    fun `SiloAction is correctly serialized to JSON`(
        underTest: SiloAction<*>,
        expected: String,
    ) {
        val result = objectMapper.writeValueAsString(underTest)

        assertThat(objectMapper.readTree(result), equalTo(objectMapper.readTree(expected)))
    }

    @ParameterizedTest(name = "Test SiloFilterExpression {1}")
    @MethodSource("getTestSiloFilterExpression")
    fun `SiloFilterExpressions is correctly serialized to JSON`(
        underTest: SiloFilterExpression,
        expected: String,
    ) {
        val result = objectMapper.writeValueAsString(underTest)

        assertThat(objectMapper.readTree(result), equalTo(objectMapper.readTree(expected)))
    }

    companion object {
        @JvmStatic
        fun getTestSiloActions() =
            listOf(
                Arguments.of(
                    SiloAction.aggregated(),
                    """
                        {
                            "type": "Aggregated",
                            "randomize": false
                        }
                    """,
                ),
                Arguments.of(
                    SiloAction.aggregated(
                        listOf("field1", "field2"),
                        listOf(
                            OrderByField("field3", Order.ASCENDING),
                            OrderByField("field4", Order.DESCENDING),
                            OrderByField("random", Order.DESCENDING),
                        ).toOrderBySpec(),
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
                            "randomize": true,
                            "limit": 100,
                            "offset": 50
                        }
                    """,
                ),
                Arguments.of(
                    SiloAction.mutations(),
                    """
                        {
                            "type": "Mutations",
                            "randomize": false
                        }
                    """,
                ),
                Arguments.of(
                    SiloAction.mutations(
                        0.5,
                        listOf(
                            OrderByField("field3", Order.ASCENDING),
                            OrderByField("field4", Order.DESCENDING),
                            OrderByField("random", Order.DESCENDING),
                        ).toOrderBySpec(),
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
                            "randomize": true,
                            "limit": 100,
                            "offset": 50
                        }
                    """,
                ),
                Arguments.of(
                    SiloAction.aminoAcidMutations(),
                    """
                        {
                            "type": "AminoAcidMutations",
                            "randomize": false
                        }
                    """,
                ),
                Arguments.of(
                    SiloAction.aminoAcidMutations(
                        0.5,
                        listOf(
                            OrderByField("field3", Order.ASCENDING),
                            OrderByField("field4", Order.DESCENDING),
                            OrderByField("random", Order.DESCENDING),
                        ).toOrderBySpec(),
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
                            "randomize": true,
                            "limit": 100,
                            "offset": 50
                        }
                    """,
                ),
                Arguments.of(
                    SiloAction.details(),
                    """
                        {
                            "type": "Details",
                            "randomize": false
                        }
                    """,
                ),
                Arguments.of(
                    SiloAction.details(
                        listOf("age", "pango_lineage"),
                        listOf(
                            OrderByField("field3", Order.ASCENDING),
                            OrderByField("field4", Order.DESCENDING),
                            OrderByField("random", Order.DESCENDING),
                        ).toOrderBySpec(),
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
                            "randomize": true,
                            "limit": 100,
                            "offset": 50
                        }
                    """,
                ),
                Arguments.of(
                    SiloAction.nucleotideInsertions(),
                    """
                        {
                            "type": "Insertions",
                            "randomize": false
                        }
                    """,
                ),
                Arguments.of(
                    SiloAction.nucleotideInsertions(
                        listOf(
                            OrderByField("field3", Order.ASCENDING),
                            OrderByField("field4", Order.DESCENDING),
                            OrderByField("random", Order.DESCENDING),
                        ).toOrderBySpec(),
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
                            "randomize": true,
                            "limit": 100,
                            "offset": 50
                        }
                    """,
                ),
                Arguments.of(
                    SiloAction.aminoAcidInsertions(),
                    """
                        {
                            "type": "AminoAcidInsertions",
                            "randomize": false
                        }
                    """,
                ),
                Arguments.of(
                    SiloAction.aminoAcidInsertions(
                        listOf(
                            OrderByField("field3", Order.ASCENDING),
                            OrderByField("field4", Order.DESCENDING),
                            OrderByField("random", Order.DESCENDING),
                        ).toOrderBySpec(),
                        100,
                        50,
                    ),
                    """
                        {
                            "type": "AminoAcidInsertions",
                            "orderByFields": [
                                {"field": "field3", "order": "ascending"},
                                {"field": "field4", "order": "descending"}
                            ],
                            "randomize": true,
                            "limit": 100,
                            "offset": 50
                        }
                    """,
                ),
                Arguments.of(
                    SiloAction.genomicSequence(SequenceType.ALIGNED, listOf("someSequenceName")),
                    """
                        {
                            "type": "FastaAligned",
                            "sequenceNames": ["someSequenceName"],
                            "randomize": false
                        }
                    """,
                ),
                Arguments.of(
                    SiloAction.genomicSequence(
                        type = SequenceType.ALIGNED,
                        sequenceNames = listOf("someSequenceName"),
                        additionalFields = listOf("field1", "field2"),
                        orderByFields = listOf(
                            OrderByField("field3", Order.ASCENDING),
                            OrderByField("field4", Order.DESCENDING),
                            OrderByField("random", Order.DESCENDING),
                        ).toOrderBySpec(),
                        limit = 100,
                        offset = 50,
                    ),
                    """
                        {
                            "type": "FastaAligned",
                            "sequenceNames": ["someSequenceName"],
                            "additionalFields": ["field1", "field2"],
                            "orderByFields": [
                                {"field": "field3", "order": "ascending"},
                                {"field": "field4", "order": "descending"}
                            ],
                            "randomize": true,
                            "limit": 100,
                            "offset": 50
                        }
                    """,
                ),
                Arguments.of(
                    SiloAction.genomicSequence(SequenceType.UNALIGNED, listOf("someSequenceName")),
                    """
                        {
                            "type": "Fasta",
                            "sequenceNames": ["someSequenceName"],
                            "randomize": false
                        }
                    """,
                ),
                Arguments.of(
                    SiloAction.genomicSequence(
                        type = SequenceType.UNALIGNED,
                        sequenceNames = listOf("someSequenceName"),
                        additionalFields = listOf("field1", "field2"),
                        orderByFields = listOf(
                            OrderByField("field3", Order.ASCENDING),
                            OrderByField("field4", Order.DESCENDING),
                            OrderByField("random", Order.DESCENDING),
                        ).toOrderBySpec(),
                        limit = 100,
                        offset = 50,
                    ),
                    """
                        {
                            "type": "Fasta",
                            "sequenceNames": ["someSequenceName"],
                            "additionalFields": ["field1", "field2"],
                            "orderByFields": [
                                {"field": "field3", "order": "ascending"},
                                {"field": "field4", "order": "descending"}
                            ],
                            "randomize": true,
                            "limit": 100,
                            "offset": 50
                        }
                    """,
                ),
                Arguments.of(
                    SiloAction.mostRecentCommonAncestor(
                        "phyloTreeField",
                    ),
                    """
                        {
                            "columnName": "phyloTreeField",
                            "printNodesNotInTree": false,
                            "type": "MostRecentCommonAncestor"
                        }
                    """,
                ),
                Arguments.of(
                    SiloAction.phyloSubtree(
                        "phyloTreeField",
                    ),
                    """
                        {
                            "columnName": "phyloTreeField",
                            "printNodesNotInTree": false,
                            "type": "PhyloSubtree"
                        }
                    """,
                ),
            )

        @JvmStatic
        fun getTestSiloFilterExpression() =
            listOf(
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
                    StringEquals("theColumn", null),
                    """
                        {
                            "type": "StringEquals",
                            "column": "theColumn",
                            "value": null
                        }
                    """,
                ),
                Arguments.of(
                    LineageEquals("fieldName", "ABC", includeSublineages = false),
                    """
                        {
                            "type": "Lineage",
                            "column": "fieldName",
                            "value": "ABC",
                            "includeSublineages": false
                        }
                    """,
                ),
                Arguments.of(
                    LineageEquals("fieldName", "ABC", includeSublineages = true),
                    """
                        {
                            "type": "Lineage",
                            "column": "fieldName",
                            "value": "ABC",
                            "includeSublineages": true
                        }
                    """,
                ),
                Arguments.of(
                    LineageEquals("fieldName", null, includeSublineages = false),
                    """
                        {
                            "type": "Lineage",
                            "column": "fieldName",
                            "value": null,
                            "includeSublineages": false
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
                    NucleotideInsertionContains(1234, "A", "segment"),
                    """
                        {
                            "type": "InsertionContains",
                            "position": 1234,
                            "value": "A",
                            "sequenceName":"segment"
                        }
                    """,
                ),
                Arguments.of(
                    NucleotideInsertionContains(1234, "A", null),
                    """
                        {
                            "type": "InsertionContains",
                            "position": 1234,
                            "value": "A"
                        }
                    """,
                ),
                Arguments.of(
                    AminoAcidInsertionContains(1234, "A", "someGene"),
                    """
                        {
                            "type": "AminoAcidInsertionContains",
                            "position": 1234,
                            "value": "A",
                            "sequenceName":"someGene"
                        }
                    """,
                ),
                Arguments.of(
                    AminoAcidInsertionContains(1234, "A\\*B", "someGene"),
                    """
                        {
                            "type": "AminoAcidInsertionContains",
                            "position": 1234,
                            "value": "A\\*B",
                            "sequenceName":"someGene"
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
                Arguments.of(
                    BooleanEquals(
                        column = "theColumn",
                        value = true,
                    ),
                    """
                        {
                            "type": "BooleanEquals",
                            "column": "theColumn",
                            "value": true
                        }
                    """,
                ),
                Arguments.of(
                    BooleanEquals(
                        column = "theColumn",
                        value = null,
                    ),
                    """
                        {
                            "type": "BooleanEquals",
                            "column": "theColumn",
                            "value": null
                        }
                    """,
                ),
                Arguments.of(
                    FloatEquals(
                        column = "theColumn",
                        value = 1.0,
                    ),
                    """
                        {
                            "type": "FloatEquals",
                            "column": "theColumn",
                            "value": 1.0
                        }
                    """,
                ),
                Arguments.of(
                    FloatEquals(
                        column = "theColumn",
                        value = null,
                    ),
                    """
                        {
                            "type": "FloatEquals",
                            "column": "theColumn",
                            "value": null
                        }
                    """,
                ),
                Arguments.of(
                    IntEquals(
                        column = "theColumn",
                        value = 1,
                    ),
                    """
                        {
                            "type": "IntEquals",
                            "column": "theColumn",
                            "value": 1
                        }
                    """,
                ),
                Arguments.of(
                    IntEquals(
                        column = "theColumn",
                        value = null,
                    ),
                    """
                        {
                            "type": "IntEquals",
                            "column": "theColumn",
                            "value": null
                        }
                    """,
                ),
                Arguments.of(
                    StringSearch("theColumn", "theValue"),
                    """
                        {
                            "type": "StringSearch",
                            "column": "theColumn",
                            "searchExpression": "theValue"
                        }
                    """,
                ),
                Arguments.of(
                    PhyloDescendantOf("theColumn", "internalNode"),
                    """
                        {
                            "type": "PhyloDescendantOf",
                            "column": "theColumn",
                            "internalNode": "internalNode"
                        }
                    """,
                ),
            )
    }
}
