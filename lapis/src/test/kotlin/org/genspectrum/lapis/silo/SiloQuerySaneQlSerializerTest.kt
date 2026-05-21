package org.genspectrum.lapis.silo

import org.genspectrum.lapis.request.Order
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.OrderBySpec
import org.genspectrum.lapis.request.toOrderBySpec
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

class SiloQuerySaneQlSerializerTest {
    @Test
    fun `GIVEN full query THEN is correctly serialized to SaneQL`() {
        val query = SiloQuery(
            SiloAction.aggregated(),
            StringEquals("theColumn", "theValue"),
        )

        val result = SiloQuerySaneQlSerializer.serialize(query)

        assertThat(
            result,
            equalTo("default.filter(theColumn = 'theValue').groupBy({count:=count()})"),
        )
    }

    @Test
    fun `GIVEN query with True filter THEN filters for true`() {
        val query = SiloQuery(SiloAction.aggregated(), True)

        val result = SiloQuerySaneQlSerializer.serialize(query)

        assertThat(result, equalTo("default.filter(true).groupBy({count:=count()})"))
    }

    @Test
    fun `GIVEN query with details action and no fields THEN produces no action`() {
        val query = SiloQuery(SiloAction.details(), True)

        val result = SiloQuerySaneQlSerializer.serialize(query)

        assertThat(result, equalTo("default.filter(true)"))
    }

    @ParameterizedTest(name = "action: {1}")
    @MethodSource("getSiloActionTestCases")
    fun `SiloAction is correctly serialized to SaneQL`(
        action: SiloAction<*>,
        expectedSaneQl: String,
    ) {
        val query = SiloQuery(action, True)

        val result = SiloQuerySaneQlSerializer.serialize(query)

        assertThat(result, equalTo("default.filter(true)$expectedSaneQl"))
    }

    @ParameterizedTest(name = "filter: {1}")
    @MethodSource("getFilterExpressionTestCases")
    fun `SiloFilterExpression is correctly serialized to SaneQL`(
        filter: SiloFilterExpression,
        expectedPredicate: String,
    ) {
        val query = SiloQuery(SiloAction.aggregated(), filter)

        val result = SiloQuerySaneQlSerializer.serialize(query)

        assertThat(
            result,
            equalTo("default.filter($expectedPredicate).groupBy({count:=count()})"),
        )
    }

    companion object {
        @JvmStatic
        fun getSiloActionTestCases() =
            listOf(
                // Aggregated
                Arguments.of(
                    SiloAction.aggregated(),
                    ".groupBy({count:=count()})",
                ),
                Arguments.of(
                    SiloAction.aggregated(listOf("field1", "field2")),
                    ".groupBy({count:=count()}, {field1, field2})",
                ),
                Arguments.of(
                    SiloAction.aggregated(
                        listOf("field1", "field2"),
                        listOf(
                            OrderByField("field3", Order.ASCENDING),
                            OrderByField("field4", Order.DESCENDING),
                        ).toOrderBySpec(),
                        100,
                        50,
                    ),
                    ".groupBy({count:=count()}, {field1, field2}).orderBy({field3, field4.desc()}).offset(50).limit(100)",
                ),
                Arguments.of(
                    SiloAction.aggregated(orderByFields = OrderBySpec.Random(seed = null)),
                    ".groupBy({count:=count()}).randomize()",
                ),
                Arguments.of(
                    SiloAction.aggregated(orderByFields = OrderBySpec.Random(seed = 123)),
                    ".groupBy({count:=count()}).randomize(seed:=123)",
                ),
                Arguments.of(
                    SiloAction.aggregated(
                        orderByFields = OrderBySpec.Random(seed = 42),
                        limit = 10,
                    ),
                    ".groupBy({count:=count()}).randomize(seed:=42).limit(10)",
                ),
                // Mutations
                Arguments.of(
                    SiloAction.mutations(),
                    ".mutations()",
                ),
                // Mutations (with minProportion, orderBy, limit, offset)
                Arguments.of(
                    SiloAction.mutations(
                        0.5,
                        listOf(
                            OrderByField("field3", Order.ASCENDING),
                            OrderByField("field4", Order.DESCENDING),
                        ).toOrderBySpec(),
                        100,
                        50,
                    ),
                    ".mutations(minProportion:=0.5).orderBy({field3, field4.desc()}).offset(50).limit(100)",
                ),
                Arguments.of(
                    SiloAction.mutations(0.05, fields = listOf("mutation", "count", "proportion")),
                    ".mutations(minProportion:=0.05, fields:={mutation, count, proportion})",
                ),
                Arguments.of(
                    SiloAction.aminoAcidMutations(),
                    ".aminoAcidMutations()",
                ),
                Arguments.of(
                    SiloAction.aminoAcidMutations(
                        0.5,
                        listOf(
                            OrderByField("field3", Order.ASCENDING),
                            OrderByField("field4", Order.DESCENDING),
                        ).toOrderBySpec(),
                        100,
                        50,
                    ),
                    ".aminoAcidMutations(minProportion:=0.5).orderBy({field3, field4.desc()}).offset(50).limit(100)",
                ),
                // Details
                Arguments.of(
                    SiloAction.details(),
                    "",
                ),
                Arguments.of(
                    SiloAction.details(
                        listOf("age", "pango_lineage"),
                        listOf(
                            OrderByField("field3", Order.ASCENDING),
                            OrderByField("field4", Order.DESCENDING),
                        ).toOrderBySpec(),
                        100,
                        50,
                    ),
                    ".project({age, pango_lineage}).orderBy({field3, field4.desc()}).offset(50).limit(100)",
                ),
                Arguments.of(
                    SiloAction.details(orderByFields = OrderBySpec.Random(seed = 0)),
                    ".randomize(seed:=0)",
                ),
                Arguments.of(
                    SiloAction.details(
                        fields = listOf("country", "date"),
                        orderByFields = OrderBySpec.ByFields(
                            listOf(
                                OrderByField("country", Order.ASCENDING),
                                OrderByField("date", Order.DESCENDING),
                            ),
                        ),
                    ),
                    ".project({country, date}).orderBy({country, date.desc()})",
                ),
                Arguments.of(
                    SiloAction.details(
                        fields = listOf("country"),
                        orderByFields = OrderBySpec.Random(seed = null),
                        limit = 5,
                    ),
                    ".project({country}).randomize().limit(5)",
                ),
                // NucleotideInsertions
                Arguments.of(
                    SiloAction.nucleotideInsertions(),
                    ".insertions()",
                ),
                Arguments.of(
                    SiloAction.nucleotideInsertions(
                        listOf(
                            OrderByField("field3", Order.ASCENDING),
                            OrderByField("field4", Order.DESCENDING),
                        ).toOrderBySpec(),
                        100,
                        50,
                    ),
                    ".insertions().orderBy({field3, field4.desc()}).offset(50).limit(100)",
                ),
                Arguments.of(
                    SiloAction.aminoAcidInsertions(),
                    ".aminoAcidInsertions()",
                ),
                Arguments.of(
                    SiloAction.aminoAcidInsertions(
                        listOf(
                            OrderByField("field3", Order.ASCENDING),
                            OrderByField("field4", Order.DESCENDING),
                        ).toOrderBySpec(),
                        100,
                        50,
                    ),
                    ".aminoAcidInsertions().orderBy({field3, field4.desc()}).offset(50).limit(100)",
                ),
                // Sequence
                Arguments.of(
                    SiloAction.genomicSequence(SequenceType.ALIGNED, listOf("someSequenceName")),
                    ".project({someSequenceName})",
                ),
                Arguments.of(
                    SiloAction.genomicSequence(SequenceType.UNALIGNED, listOf("someSequenceName")),
                    ".project({someSequenceName})",
                ),
                Arguments.of(
                    SiloAction.genomicSequence(
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
                    ".project({field1, field2, someSequenceName}).orderBy({field3, field4.desc()}).offset(50).limit(100)",
                ),
                // MostRecentCommonAncestor
                Arguments.of(
                    SiloAction.mostRecentCommonAncestor("phyloTreeField"),
                    ".mostRecentCommonAncestor('phyloTreeField')",
                ),
                Arguments.of(
                    SiloAction.mostRecentCommonAncestor("phyloTreeField", printNodesNotInTree = true),
                    ".mostRecentCommonAncestor('phyloTreeField', printNodesNotInTree:=true)",
                ),
                // PhyloSubtree
                Arguments.of(
                    SiloAction.phyloSubtree("phyloTreeField"),
                    ".phyloSubtree('phyloTreeField')",
                ),
                Arguments.of(
                    SiloAction.phyloSubtree("phyloTreeField", printNodesNotInTree = true),
                    ".phyloSubtree('phyloTreeField', printNodesNotInTree:=true)",
                ),
            )

        @JvmStatic
        fun getFilterExpressionTestCases() =
            listOf(
                // StringEquals
                Arguments.of(
                    StringEquals("theColumn", "theValue"),
                    "theColumn = 'theValue'",
                ),
                Arguments.of(
                    StringEquals("theColumn", null),
                    "isNull(theColumn)",
                ),
                // StringEquals with single quote in value
                Arguments.of(
                    StringEquals("country", "Côte d'Ivoire"),
                    "country = 'Côte d''Ivoire'",
                ),
                // BooleanEquals
                Arguments.of(
                    BooleanEquals("theColumn", true),
                    "theColumn = true",
                ),
                Arguments.of(
                    BooleanEquals("theColumn", false),
                    "theColumn = false",
                ),
                Arguments.of(
                    BooleanEquals("theColumn", null),
                    "isNull(theColumn)",
                ),
                // IntEquals
                Arguments.of(
                    IntEquals("theColumn", 42),
                    "theColumn = 42",
                ),
                Arguments.of(
                    IntEquals("theColumn", null),
                    "isNull(theColumn)",
                ),
                // FloatEquals
                Arguments.of(
                    FloatEquals("theColumn", 1.0),
                    "theColumn = 1.0",
                ),
                Arguments.of(
                    FloatEquals("theColumn", null),
                    "isNull(theColumn)",
                ),
                // DateBetween
                Arguments.of(
                    DateBetween("fieldName", LocalDate.of(2021, 3, 31), LocalDate.of(2022, 6, 3)),
                    "fieldName.between('2021-03-31'::date, '2022-06-03'::date)",
                ),
                Arguments.of(
                    DateBetween("fieldName", null, LocalDate.of(2022, 6, 3)),
                    "fieldName.between(null, '2022-06-03'::date)",
                ),
                Arguments.of(
                    DateBetween("fieldName", LocalDate.of(2021, 3, 31), null),
                    "fieldName.between('2021-03-31'::date, null)",
                ),
                // IntBetween
                Arguments.of(
                    IntBetween("age", 18, 65),
                    "age.between(18, 65)",
                ),
                Arguments.of(
                    IntBetween("age", 18, null),
                    "age.between(18, null)",
                ),
                Arguments.of(
                    IntBetween("age", null, 65),
                    "age.between(null, 65)",
                ),
                // FloatBetween
                Arguments.of(
                    FloatBetween("score", 0.5, 1.0),
                    "score.between(0.5, 1.0)",
                ),
                Arguments.of(
                    FloatBetween("score", 0.5, null),
                    "score.between(0.5, null)",
                ),
                // LineageEquals
                Arguments.of(
                    LineageEquals("fieldName", "ABC", includeSublineages = false),
                    "fieldName.lineage('ABC', includeSublineages:=false)",
                ),
                Arguments.of(
                    LineageEquals("fieldName", "ABC", includeSublineages = true),
                    "fieldName.lineage('ABC', includeSublineages:=true)",
                ),
                Arguments.of(
                    LineageEquals("fieldName", null, includeSublineages = false),
                    "fieldName.lineage(null, includeSublineages:=false)",
                ),
                // StringSearch
                Arguments.of(
                    StringSearch("theColumn", "theValue"),
                    "theColumn.like('theValue')",
                ),
                // IsNull / IsNotNull
                Arguments.of(
                    IsNull("theColumn"),
                    "isNull(theColumn)",
                ),
                Arguments.of(
                    IsNotNull("theColumn"),
                    "isNotNull(theColumn)",
                ),
                // NucleotideSymbolEquals
                Arguments.of(
                    NucleotideSymbolEquals(null, 1234, "A"),
                    "nucleotideEquals(position:=1234, symbol:='A')",
                ),
                Arguments.of(
                    NucleotideSymbolEquals("sequence name", 1234, "A"),
                    "nucleotideEquals(position:=1234, symbol:='A', sequenceName:='sequence name')",
                ),
                // HasNucleotideMutation
                Arguments.of(
                    HasNucleotideMutation("sequence name", 1234),
                    "hasMutation(position:=1234, sequenceName:='sequence name')",
                ),
                Arguments.of(
                    HasNucleotideMutation(null, 1234),
                    "hasMutation(position:=1234)",
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
                Arguments.of(
                    NucleotideInsertionContains(1234, "A", null),
                    "insertionContains(position:=1234, value:='A')",
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
                    "theColumn.phyloDescendantOf('internalNode')",
                ),
                // And
                Arguments.of(
                    And(StringEquals("theColumn", "theValue"), StringEquals("theOtherColumn", "theOtherValue")),
                    "theColumn = 'theValue' && theOtherColumn = 'theOtherValue'",
                ),
                // Or
                Arguments.of(
                    Or(StringEquals("theColumn", "theValue"), StringEquals("theOtherColumn", "theOtherValue")),
                    "theColumn = 'theValue' || theOtherColumn = 'theOtherValue'",
                ),
                // Not
                Arguments.of(
                    Not(StringEquals("theColumn", "theValue")),
                    "!(theColumn = 'theValue')",
                ),
                // Maybe
                Arguments.of(
                    Maybe(StringEquals("theColumn", "theValue")),
                    "maybe(theColumn = 'theValue')",
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
                    "nOf(2, {theColumn = 'theValue', theOtherColumn = 'theOtherValue'}, matchExactly:=true)",
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
                    "nOf(2, {theColumn = 'theValue', theOtherColumn = 'theOtherValue'})",
                ),
                // Nested And inside Or — verifies parenthesization
                Arguments.of(
                    Or(
                        And(StringEquals("a", "1"), StringEquals("b", "2")),
                        StringEquals("c", "3"),
                    ),
                    "(a = '1' && b = '2') || c = '3'",
                ),
                // Nested Or inside And — verifies parenthesization
                Arguments.of(
                    And(
                        Or(StringEquals("a", "1"), StringEquals("b", "2")),
                        StringEquals("c", "3"),
                    ),
                    "(a = '1' || b = '2') && c = '3'",
                ),
            )
    }
}
