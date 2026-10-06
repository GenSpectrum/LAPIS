package org.genspectrum.lapis.silo

import org.genspectrum.lapis.request.ComputedField
import org.genspectrum.lapis.request.Order
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.OrderBySpec
import org.genspectrum.lapis.request.ScalarFunction
import org.genspectrum.lapis.request.SequencePositionField
import org.genspectrum.lapis.request.toOrderBySpec
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

class RhyDbQueryToSaneQlTest {
    @Test
    fun `GIVEN full query THEN is correctly serialized to SaneQL`() {
        val query = RhyDbQuery(
            RhyDbAction.aggregated(),
            StringEquals("theColumn", "theValue"),
        )

        val result = query.toSaneQl()

        assertThat(
            result,
            equalTo("""default.filter("theColumn" = 'theValue').group(by:={}, aggs:={"count":=count()})"""),
        )
    }

    @Test
    fun `GIVEN query with True filter THEN filters for true`() {
        val query = RhyDbQuery(RhyDbAction.aggregated(), True)

        val result = query.toSaneQl()

        assertThat(result, equalTo("""default.filter(true).group(by:={}, aggs:={"count":=count()})"""))
    }

    @Test
    fun `GIVEN query with details action and no fields THEN produces no action`() {
        val query = RhyDbQuery(RhyDbAction.details(), True)

        val result = query.toSaneQl()

        assertThat(result, equalTo("default.filter(true)"))
    }

    @ParameterizedTest(name = "action: {1}")
    @MethodSource("getRhyDbActionTestCases")
    fun `RhyDbAction is correctly serialized to SaneQL`(
        action: RhyDbAction<*>,
        expectedSaneQl: String,
    ) {
        val query = RhyDbQuery(action, True)

        val result = query.toSaneQl()

        assertThat(result, equalTo("default.filter(true)$expectedSaneQl"))
    }

    @ParameterizedTest(name = "filter: {1}")
    @MethodSource("getFilterExpressionTestCases")
    fun `RhyDbFilterExpression is correctly serialized to SaneQL`(
        filter: RhyDbFilterExpression,
        expectedPredicate: String,
    ) {
        val query = RhyDbQuery(RhyDbAction.aggregated(), filter)

        val result = query.toSaneQl()

        assertThat(
            result,
            equalTo("""default.filter($expectedPredicate).group(by:={}, aggs:={"count":=count()})"""),
        )
    }

    @Test
    fun `GIVEN orderBy field with injection attempt THEN payload is quoted as identifier`() {
        val query = RhyDbQuery(
            RhyDbAction.aggregated(
                groupByFields = listOf("country"),
                orderByFields = listOf(
                    OrderByField("count}).filter(true).groupBy({evil:=count()", Order.ASCENDING),
                ).toOrderBySpec(),
            ),
            True,
        )

        val result = query.toSaneQl()

        assertThat(
            result,
            equalTo(
                """default.filter(true).group(by:={"country"}, aggs:={"count":=count()})""" +
                    """.order(by:={"count}).filter(true).groupBy({evil:=count()"})""", // <- the order field is quoted
            ),
        )
    }

    companion object {
        @JvmStatic
        fun getRhyDbActionTestCases() =
            listOf(
                // Aggregated
                Arguments.of(
                    RhyDbAction.aggregated(),
                    """.group(by:={}, aggs:={"count":=count()})""",
                ),
                Arguments.of(
                    RhyDbAction.aggregated(listOf("field1", "field2")),
                    """.group(by:={"field1", "field2"}, aggs:={"count":=count()})""",
                ),
                Arguments.of(
                    RhyDbAction.aggregated(
                        groupByFields = listOf("country"),
                        computedFields = listOf(ComputedField("date", ScalarFunction.ISO_WEEK)),
                    ),
                    """.map({"date.isoWeek":="date".isoWeek()}).group(by:={"country", "date.isoWeek"}, aggs:={"count":=count()})""",
                ),
                Arguments.of(
                    RhyDbAction.aggregated(
                        groupByFields = listOf("field1", "field2"),
                        orderByFields = listOf(
                            OrderByField("field3", Order.ASCENDING),
                            OrderByField("field4", Order.DESCENDING),
                        ).toOrderBySpec(),
                        limit = 100,
                        offset = 50,
                    ),
                    """.group(by:={"field1", "field2"}, aggs:={"count":=count()}).order(by:={"field3", "field4".desc()}).offset(50).limit(100)""",
                ),
                Arguments.of(
                    RhyDbAction.aggregated(orderByFields = OrderBySpec.Random(seed = null)),
                    """.group(by:={}, aggs:={"count":=count()}).randomize()""",
                ),
                Arguments.of(
                    RhyDbAction.aggregated(orderByFields = OrderBySpec.Random(seed = 123)),
                    """.group(by:={}, aggs:={"count":=count()}).randomize(seed:=123)""",
                ),
                Arguments.of(
                    RhyDbAction.aggregated(
                        orderByFields = OrderBySpec.Random(seed = 42),
                        limit = 10,
                    ),
                    """.group(by:={}, aggs:={"count":=count()}).randomize(seed:=42).limit(10)""",
                ),
                Arguments.of(
                    RhyDbAction.aggregated(
                        groupByFields = listOf("country"),
                        sequencePositionFields = listOf(SequencePositionField("S", 501)),
                    ),
                    """.map({"S[501]":="S".at(501)}).group(by:={"country", "S[501]"}, aggs:={"count":=count()})""",
                ),
                Arguments.of(
                    RhyDbAction.aggregated(
                        sequencePositionFields = listOf(
                            SequencePositionField("S", 123),
                            SequencePositionField("main", 456),
                        ),
                    ),
                    """.map({"S[123]":="S".at(123), "main[456]":="main".at(456)}).group(by:={"S[123]", "main[456]"}, aggs:={"count":=count()})""",
                ),
                // Mutations
                Arguments.of(
                    RhyDbAction.mutations(),
                    ".mutations()",
                ),
                Arguments.of(
                    RhyDbAction.mutations(
                        0.5,
                        listOf(
                            OrderByField("field3", Order.ASCENDING),
                            OrderByField("field4", Order.DESCENDING),
                        ).toOrderBySpec(),
                        100,
                        50,
                    ),
                    """.mutations(minProportion:=0.5).order(by:={"field3", "field4".desc()}).offset(50).limit(100)""",
                ),
                Arguments.of(
                    RhyDbAction.mutations(0.05, fields = listOf("mutation", "count", "proportion")),
                    """.mutations(minProportion:=0.05, fields:={"mutation", "count", "proportion"})""",
                ),
                Arguments.of(
                    RhyDbAction.aminoAcidMutations(),
                    ".aminoAcidMutations()",
                ),
                Arguments.of(
                    RhyDbAction.aminoAcidMutations(
                        0.5,
                        listOf(
                            OrderByField("field3", Order.ASCENDING),
                            OrderByField("field4", Order.DESCENDING),
                        ).toOrderBySpec(),
                        100,
                        50,
                    ),
                    """.aminoAcidMutations(minProportion:=0.5).order(by:={"field3", "field4".desc()}).offset(50).limit(100)""",
                ),
                // Details
                Arguments.of(
                    RhyDbAction.details(),
                    "",
                ),
                Arguments.of(
                    RhyDbAction.details(
                        listOf("age", "pango_lineage"),
                        listOf(
                            OrderByField("field3", Order.ASCENDING),
                            OrderByField("field4", Order.DESCENDING),
                        ).toOrderBySpec(),
                        100,
                        50,
                    ),
                    """.project({"age", "pango_lineage"}).order(by:={"field3", "field4".desc()}).offset(50).limit(100)""",
                ),
                Arguments.of(
                    RhyDbAction.details(orderByFields = OrderBySpec.Random(seed = 0)),
                    ".randomize(seed:=0)",
                ),
                Arguments.of(
                    RhyDbAction.details(
                        fields = listOf("country", "date"),
                        orderByFields = OrderBySpec.ByFields(
                            listOf(
                                OrderByField("country", Order.ASCENDING),
                                OrderByField("date", Order.DESCENDING),
                            ),
                        ),
                    ),
                    """.project({"country", "date"}).order(by:={"country", "date".desc()})""",
                ),
                Arguments.of(
                    RhyDbAction.details(
                        fields = listOf("country"),
                        orderByFields = OrderBySpec.Random(seed = null),
                        limit = 5,
                    ),
                    """.project({"country"}).randomize().limit(5)""",
                ),
                // NucleotideInsertions
                Arguments.of(
                    RhyDbAction.nucleotideInsertions(),
                    ".insertions()",
                ),
                Arguments.of(
                    RhyDbAction.nucleotideInsertions(
                        listOf(
                            OrderByField("field3", Order.ASCENDING),
                            OrderByField("field4", Order.DESCENDING),
                        ).toOrderBySpec(),
                        100,
                        50,
                    ),
                    """.insertions().order(by:={"field3", "field4".desc()}).offset(50).limit(100)""",
                ),
                Arguments.of(
                    RhyDbAction.aminoAcidInsertions(),
                    ".aminoAcidInsertions()",
                ),
                Arguments.of(
                    RhyDbAction.aminoAcidInsertions(
                        listOf(
                            OrderByField("field3", Order.ASCENDING),
                            OrderByField("field4", Order.DESCENDING),
                        ).toOrderBySpec(),
                        100,
                        50,
                    ),
                    """.aminoAcidInsertions().order(by:={"field3", "field4".desc()}).offset(50).limit(100)""",
                ),
                // Sequence
                Arguments.of(
                    RhyDbAction.genomicSequence(SequenceType.ALIGNED, listOf("someSequenceName")),
                    """.project({"someSequenceName"})""",
                ),
                Arguments.of(
                    RhyDbAction.genomicSequence(SequenceType.UNALIGNED, listOf("someSequenceName")),
                    """.project({"someSequenceName"})""",
                ),
                Arguments.of(
                    RhyDbAction.genomicSequence(
                        type = SequenceType.ALIGNED,
                        sequenceNames = listOf("someSequenceName"),
                        additionalFields = listOf("field1", "field2"),
                        orderByFields = listOf(
                            OrderByField("field3", Order.ASCENDING),
                            OrderByField("field4", Order.DESCENDING),
                        ).toOrderBySpec(),
                        limit = 100,
                        offset = 50,
                    ),
                    """.project({"field1", "field2", "someSequenceName"}).order(by:={"field3", "field4".desc()}).offset(50).limit(100)""",
                ),
                // MostRecentCommonAncestor
                Arguments.of(
                    RhyDbAction.mostRecentCommonAncestor("phyloTreeField"),
                    ".mostRecentCommonAncestor('phyloTreeField')",
                ),
                Arguments.of(
                    RhyDbAction.mostRecentCommonAncestor("phyloTreeField", printNodesNotInTree = true),
                    ".mostRecentCommonAncestor('phyloTreeField', printNodesNotInTree:=true)",
                ),
                // PhyloSubtree
                Arguments.of(
                    RhyDbAction.phyloSubtree("phyloTreeField"),
                    ".phyloSubtree('phyloTreeField')",
                ),
                Arguments.of(
                    RhyDbAction.phyloSubtree("phyloTreeField", printNodesNotInTree = true),
                    ".phyloSubtree('phyloTreeField', printNodesNotInTree:=true)",
                ),
            )

        @JvmStatic
        fun getFilterExpressionTestCases() =
            listOf(
                // StringEquals
                Arguments.of(
                    StringEquals("theColumn", "theValue"),
                    """"theColumn" = 'theValue'""",
                ),
                Arguments.of(
                    StringEquals("theColumn", null),
                    """isNull("theColumn")""",
                ),
                // StringEquals with single quote in value
                Arguments.of(
                    StringEquals("country", "Côte d'Ivoire"),
                    """"country" = 'Côte d''Ivoire'""",
                ),
                // BooleanEquals
                Arguments.of(
                    BooleanEquals("theColumn", true),
                    """"theColumn" = true""",
                ),
                Arguments.of(
                    BooleanEquals("theColumn", false),
                    """"theColumn" = false""",
                ),
                Arguments.of(
                    BooleanEquals("theColumn", null),
                    """isNull("theColumn")""",
                ),
                // IntEquals
                Arguments.of(
                    IntEquals("theColumn", 42),
                    """"theColumn" = 42""",
                ),
                Arguments.of(
                    IntEquals("theColumn", null),
                    """isNull("theColumn")""",
                ),
                // FloatEquals
                Arguments.of(
                    FloatEquals("theColumn", 1.0),
                    """"theColumn" = 1.0""",
                ),
                Arguments.of(
                    FloatEquals("theColumn", null),
                    """isNull("theColumn")""",
                ),
                // DateBetween
                Arguments.of(
                    DateBetween("fieldName", LocalDate.of(2021, 3, 31), LocalDate.of(2022, 6, 3)),
                    """"fieldName".between('2021-03-31'::date, '2022-06-03'::date)""",
                ),
                Arguments.of(
                    DateBetween("fieldName", null, LocalDate.of(2022, 6, 3)),
                    """"fieldName".between(null, '2022-06-03'::date)""",
                ),
                Arguments.of(
                    DateBetween("fieldName", LocalDate.of(2021, 3, 31), null),
                    """"fieldName".between('2021-03-31'::date, null)""",
                ),
                // IntBetween
                Arguments.of(
                    IntBetween("age", 18, 65),
                    """"age".between(18, 65)""",
                ),
                Arguments.of(
                    IntBetween("age", 18, null),
                    """"age".between(18, null)""",
                ),
                Arguments.of(
                    IntBetween("age", null, 65),
                    """"age".between(null, 65)""",
                ),
                // FloatBetween
                Arguments.of(
                    FloatBetween("score", 0.5, 1.0),
                    """"score".between(0.5, 1.0)""",
                ),
                Arguments.of(
                    FloatBetween("score", 0.5, null),
                    """"score".between(0.5, null)""",
                ),
                // LineageEquals
                Arguments.of(
                    LineageEquals("fieldName", "ABC", includeSublineages = false),
                    """"fieldName".lineage('ABC', includeSublineages:=false)""",
                ),
                Arguments.of(
                    LineageEquals("fieldName", "ABC", includeSublineages = true),
                    """"fieldName".lineage('ABC', includeSublineages:=true)""",
                ),
                Arguments.of(
                    LineageEquals("fieldName", null, includeSublineages = false),
                    """"fieldName".lineage(null, includeSublineages:=false)""",
                ),
                // StringSearch
                Arguments.of(
                    StringSearch("theColumn", "theValue"),
                    """"theColumn".like('theValue')""",
                ),
                // IsNull / IsNotNull
                Arguments.of(
                    IsNull("theColumn"),
                    """isNull("theColumn")""",
                ),
                Arguments.of(
                    IsNotNull("theColumn"),
                    """isNotNull("theColumn")""",
                ),
                // NucleotideSymbolEquals
                Arguments.of(
                    NucleotideSymbolEquals("sequence name", 1234, "A"),
                    "nucleotideEquals(position:=1234, symbol:='A', sequenceName:='sequence name')",
                ),
                // HasNucleotideMutation
                Arguments.of(
                    HasNucleotideMutation("sequence name", 1234),
                    "hasMutation(position:=1234, sequenceName:='sequence name')",
                ),
                // AminoAcidSymbolEquals
                Arguments.of(
                    AminoAcidSymbolEquals("gene name", 1234, "A"),
                    "aminoAcidEquals(position:=1234, symbol:='A', sequenceName:='gene name')",
                ),
                // HasAminoAcidMutation
                Arguments.of(
                    HasAminoAcidMutation("gene name", 1234),
                    "hasAAMutation(position:=1234, sequenceName:='gene name')",
                ),
                // NucleotideInsertionContains
                Arguments.of(
                    NucleotideInsertionContains(1234, "A", "segment"),
                    "insertionContains(position:=1234, value:='A', sequenceName:='segment')",
                ),
                // AminoAcidInsertionContains
                Arguments.of(
                    AminoAcidInsertionContains(1234, "A", "someGene"),
                    "aminoAcidInsertionContains(position:=1234, value:='A', sequenceName:='someGene')",
                ),
                Arguments.of(
                    AminoAcidInsertionContains(1234, "A\\*B", "someGene"),
                    "aminoAcidInsertionContains(position:=1234, value:='A\\*B', sequenceName:='someGene')",
                ),
                // PhyloDescendantOf
                Arguments.of(
                    PhyloDescendantOf("theColumn", "internalNode"),
                    """"theColumn".phyloDescendantOf('internalNode')""",
                ),
                // And
                Arguments.of(
                    And(StringEquals("theColumn", "theValue"), StringEquals("theOtherColumn", "theOtherValue")),
                    """"theColumn" = 'theValue' && "theOtherColumn" = 'theOtherValue'""",
                ),
                // Or
                Arguments.of(
                    Or(StringEquals("theColumn", "theValue"), StringEquals("theOtherColumn", "theOtherValue")),
                    """"theColumn" = 'theValue' || "theOtherColumn" = 'theOtherValue'""",
                ),
                // Not
                Arguments.of(
                    Not(StringEquals("theColumn", "theValue")),
                    """!("theColumn" = 'theValue')""",
                ),
                // Maybe
                Arguments.of(
                    Maybe(StringEquals("theColumn", "theValue")),
                    """maybe("theColumn" = 'theValue')""",
                ),
                // NOf
                Arguments.of(
                    NOf(
                        2,
                        true,
                        listOf(
                            StringEquals("theColumn", "theValue"),
                            StringEquals("theOtherColumn", "theOtherValue"),
                        ),
                    ),
                    """nOf(2, {"theColumn" = 'theValue', "theOtherColumn" = 'theOtherValue'}, matchExactly:=true)""",
                ),
                Arguments.of(
                    NOf(
                        2,
                        false,
                        listOf(
                            StringEquals("theColumn", "theValue"),
                            StringEquals("theOtherColumn", "theOtherValue"),
                        ),
                    ),
                    """nOf(2, {"theColumn" = 'theValue', "theOtherColumn" = 'theOtherValue'})""",
                ),
                // Nested And inside Or — verifies parenthesization
                Arguments.of(
                    Or(
                        And(StringEquals("a", "1"), StringEquals("b", "2")),
                        StringEquals("c", "3"),
                    ),
                    """("a" = '1' && "b" = '2') || "c" = '3'""",
                ),
                // Nested Or inside And — verifies parenthesization
                Arguments.of(
                    And(
                        Or(StringEquals("a", "1"), StringEquals("b", "2")),
                        StringEquals("c", "3"),
                    ),
                    """("a" = '1' || "b" = '2') && "c" = '3'""",
                ),
            )
    }
}
