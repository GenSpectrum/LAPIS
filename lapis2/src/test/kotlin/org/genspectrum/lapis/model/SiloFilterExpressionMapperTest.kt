package org.genspectrum.lapis.model

import org.genspectrum.lapis.DATE_FIELD
import org.genspectrum.lapis.FIELD_WITH_UPPERCASE_LETTER
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.dummySequenceFilterFields
import org.genspectrum.lapis.request.AminoAcidInsertion
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.CommonSequenceFilters
import org.genspectrum.lapis.request.NucleotideInsertion
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.SequenceFilters
import org.genspectrum.lapis.silo.AminoAcidInsertionContains
import org.genspectrum.lapis.silo.AminoAcidSymbolEquals
import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.DateBetween
import org.genspectrum.lapis.silo.FloatBetween
import org.genspectrum.lapis.silo.FloatEquals
import org.genspectrum.lapis.silo.HasAminoAcidMutation
import org.genspectrum.lapis.silo.HasNucleotideMutation
import org.genspectrum.lapis.silo.IntBetween
import org.genspectrum.lapis.silo.IntEquals
import org.genspectrum.lapis.silo.Maybe
import org.genspectrum.lapis.silo.NucleotideInsertionContains
import org.genspectrum.lapis.silo.NucleotideSymbolEquals
import org.genspectrum.lapis.silo.Or
import org.genspectrum.lapis.silo.PangoLineageEquals
import org.genspectrum.lapis.silo.SiloFilterExpression
import org.genspectrum.lapis.silo.StringEquals
import org.genspectrum.lapis.silo.True
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

private const val SOME_VALUE = "some value"

class SiloFilterExpressionMapperTest {
    private val dummyReferenceGenomeSchema = ReferenceGenomeSchema(emptyList(), emptyList())
    private var variantQueryFacade = VariantQueryFacade(dummyReferenceGenomeSchema)

    private var underTest = SiloFilterExpressionMapper(dummySequenceFilterFields, variantQueryFacade)

    @Test
    fun `given invalid filter key then throws exception`() {
        val filterParameter = getSequenceFilters(mapOf("invalid query key" to SOME_VALUE))

        val exception = assertThrows<BadRequestException> { underTest.map(filterParameter) }

        assertThat(
            exception.message,
            containsString("'invalid query key' is not a valid sequence filter key. Valid keys are:"),
        )
    }

    @Test
    fun `GIVEN all uppercase key THEN is mapped to corresponding field`() {
        val filterParameter = getSequenceFilters(mapOf("PANGOLINEAGE" to SOME_VALUE))

        val result = underTest.map(filterParameter)

        val expected =
            And(Or(PangoLineageEquals(FIELD_WITH_UPPERCASE_LETTER, SOME_VALUE, includeSublineages = false)))
        assertThat(result, equalTo(expected))
    }

    @Test
    fun `GIVEN all lowercase key THEN is mapped to corresponding field`() {
        val filterParameter = getSequenceFilters(mapOf("pangolineage" to SOME_VALUE))

        val result = underTest.map(filterParameter)

        val expected =
            And(Or(PangoLineageEquals(FIELD_WITH_UPPERCASE_LETTER, SOME_VALUE, includeSublineages = false)))
        assertThat(result, equalTo(expected))
    }

    @Test
    fun `given empty filter parameters then returns a match-all filter`() {
        val filterParameter = getSequenceFilters(emptyMap())

        val result = underTest.map(filterParameter)

        assertThat(result, equalTo(True))
    }

    @ParameterizedTest(name = "FilterParameter: {0}, SiloQuery: {1}")
    @MethodSource("getFilterParametersWithExpectedSiloQuery")
    fun `given filter parameters then maps to expected FilterExpression`(
        filterParameter: Map<String, List<String>>,
        expectedResult: SiloFilterExpression,
    ) {
        val result = underTest.map(DummySequenceFilters(filterParameter))

        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given invalid date then should throw an exception`() {
        val filterParameter = getSequenceFilters(mapOf("date" to "this is not a date"))

        val exception = assertThrows<BadRequestException> { underTest.map(filterParameter) }
        assertThat(exception.message, containsString("date 'this is not a date' is not a valid date"))
    }

    @Test
    fun `given invalid dateTo then should throw an exception`() {
        val filterParameter = getSequenceFilters(mapOf("dateTo" to "this is not a date"))

        val exception = assertThrows<BadRequestException> { underTest.map(filterParameter) }
        assertThat(exception.message, containsString("dateTo 'this is not a date' is not a valid date"))
    }

    @Test
    fun `given invalid dateFrom then should throw an exception`() {
        val filterParameter = getSequenceFilters(mapOf("dateFrom" to "this is not a date either"))

        val exception = assertThrows<BadRequestException> { underTest.map(filterParameter) }
        assertThat(exception.message, containsString("dateFrom 'this is not a date either' is not a valid date"))
    }

    @Test
    fun `given date and dateFrom then should throw an exception`() {
        val filterParameter = getSequenceFilters(
            mapOf(
                "date" to "2021-06-03",
                "dateFrom" to "2021-06-03",
            ),
        )
        val exception = assertThrows<BadRequestException> { underTest.map(filterParameter) }
        assertThat(
            exception.message,
            containsString("Cannot filter by exact date field 'date' and by date range field 'dateFrom'."),
        )
    }

    @Test
    fun `given date and dateTo then should throw an exception`() {
        val filterParameter = getSequenceFilters(
            mapOf(
                "date" to "2021-06-03",
                "dateTo" to "2021-06-03",
            ),
        )
        val exception = assertThrows<BadRequestException> { underTest.map(filterParameter) }
        assertThat(
            exception.message,
            containsString("Cannot filter by exact date field 'date' and by date range field 'dateTo'."),
        )
    }

    @Test
    fun `given int field with non-int value then should throw an exception`() {
        val filterParameter = getSequenceFilters(
            mapOf(
                "intField" to "not a number",
            ),
        )
        val exception = assertThrows<BadRequestException> { underTest.map(filterParameter) }
        assertThat(
            exception.message,
            containsString("intField 'not a number' is not a valid integer"),
        )
    }

    @Test
    fun `given intTo field with non-int value then should throw an exception`() {
        val filterParameter = getSequenceFilters(
            mapOf(
                "intFieldTo" to "not a number",
            ),
        )
        val exception = assertThrows<BadRequestException> { underTest.map(filterParameter) }
        assertThat(
            exception.message,
            containsString("intFieldTo 'not a number' is not a valid integer"),
        )
    }

    @Test
    fun `given float field with non-float value then should throw an exception`() {
        val filterParameter = getSequenceFilters(
            mapOf(
                "floatField" to "not a number",
            ),
        )
        val exception = assertThrows<BadRequestException> { underTest.map(filterParameter) }
        assertThat(
            exception.message,
            containsString("floatField 'not a number' is not a valid float"),
        )
    }

    @Test
    fun `given floatTo field with non-float value then should throw an exception`() {
        val filterParameter = getSequenceFilters(
            mapOf(
                "floatFieldTo" to "not a number",
            ),
        )
        val exception = assertThrows<BadRequestException> { underTest.map(filterParameter) }
        assertThat(
            exception.message,
            containsString("floatFieldTo 'not a number' is not a valid float"),
        )
    }

    @Test
    fun `given int and intFrom then should throw an exception`() {
        val filterParameter = getSequenceFilters(
            mapOf(
                "intField" to "42",
                "intFieldFrom" to "43",
            ),
        )
        val exception = assertThrows<BadRequestException> { underTest.map(filterParameter) }
        assertThat(
            exception.message,
            containsString("Cannot filter by exact int field 'intField' and by int range field 'intFieldFrom'."),
        )
    }

    @Test
    fun `given int and intTo then should throw an exception`() {
        val filterParameter = getSequenceFilters(
            mapOf(
                "intFieldTo" to "43",
                "intField" to "42",
            ),
        )
        val exception = assertThrows<BadRequestException> { underTest.map(filterParameter) }
        assertThat(
            exception.message,
            containsString("Cannot filter by exact int field 'intField' and by int range field 'intFieldTo'."),
        )
    }

    @Test
    fun `given float and floatFrom then should throw an exception`() {
        val filterParameter = getSequenceFilters(
            mapOf(
                "floatField" to "42.1",
                "floatFieldFrom" to "42.3",
            ),
        )
        val exception = assertThrows<BadRequestException> { underTest.map(filterParameter) }
        assertThat(
            exception.message,
            containsString(
                "Cannot filter by exact float field 'floatField' and by float range field 'floatFieldFrom'.",
            ),
        )
    }

    @Test
    fun `given float and floatTo then should throw an exception`() {
        val filterParameter = getSequenceFilters(
            mapOf(
                "floatFieldTo" to "42.3",
                "floatField" to "42.1",
            ),
        )
        val exception = assertThrows<BadRequestException> { underTest.map(filterParameter) }
        assertThat(
            exception.message,
            containsString("Cannot filter by exact float field 'floatField' and by float range field 'floatFieldTo'."),
        )
    }

    @Test
    fun `given nucleotide mutation with symbol then is mapped to NucleotideSymbolEquals`() {
        val filterParameter = DummySequenceFilters(
            emptyMap(),
            listOf(
                NucleotideMutation(null, 123, "B", maybe = true),
                NucleotideMutation("sequenceName", 999, "A", maybe = false),
            ),
            emptyList(),
            emptyList(),
            emptyList(),
        )

        val result = underTest.map(filterParameter)

        val expected = And(
            Maybe(NucleotideSymbolEquals(null, 123, "B")),
            NucleotideSymbolEquals("sequenceName", 999, "A"),
        )
        assertThat(result, equalTo(expected))
    }

    @Test
    fun `given nucleotide mutation without symbol then is mapped to HasNucleotideMutation`() {
        val filterParameter = DummySequenceFilters(
            emptyMap(),
            listOf(NucleotideMutation(null, 123, null), NucleotideMutation("sequenceName", 999, null)),
            emptyList(),
            emptyList(),
            emptyList(),
        )

        val result = underTest.map(filterParameter)

        val expected = And(
            HasNucleotideMutation(null, 123),
            HasNucleotideMutation("sequenceName", 999),
        )
        assertThat(result, equalTo(expected))
    }

    @Test
    fun `given amino acid mutation with symbol then is mapped to AASymbolEquals`() {
        val filterParameter = DummySequenceFilters(
            emptyMap(),
            emptyList(),
            listOf(
                AminoAcidMutation("geneName1", 123, "B", maybe = true),
                AminoAcidMutation("geneName2", 999, "A", maybe = false),
            ),
            emptyList(),
            emptyList(),
        )

        val result = underTest.map(filterParameter)

        val expected = And(
            Maybe(AminoAcidSymbolEquals("geneName1", 123, "B")),
            AminoAcidSymbolEquals("geneName2", 999, "A"),
        )
        assertThat(result, equalTo(expected))
    }

    @Test
    fun `given amino acid mutation without symbol then is mapped to HasAAMutation`() {
        val filterParameter = DummySequenceFilters(
            emptyMap(),
            emptyList(),
            listOf(AminoAcidMutation("geneName1", 123, null), AminoAcidMutation("geneName2", 999, null)),
            emptyList(),
            emptyList(),
        )

        val result = underTest.map(filterParameter)

        val expected = And(
            HasAminoAcidMutation("geneName1", 123),
            HasAminoAcidMutation("geneName2", 999),
        )
        assertThat(result, equalTo(expected))
    }

    @Test
    fun `given nucleotide insertion it is mapped to NucleotideInsertionContains without using the segment`() {
        val filterParameter = DummySequenceFilters(
            emptyMap(),
            emptyList(),
            emptyList(),
            listOf(NucleotideInsertion(123, "ABCD", "segment"), NucleotideInsertion(999, "DEF", null)),
            emptyList(),
        )

        val result = underTest.map(filterParameter)

        val expected =
            And(NucleotideInsertionContains(123, "ABCD"), NucleotideInsertionContains(999, "DEF"))
        assertThat(result, equalTo(expected))
    }

    @Test
    fun `given amino acid insertion then it is mapped to AminoAcidInsertionContains`() {
        val filterParameter = DummySequenceFilters(
            emptyMap(),
            emptyList(),
            emptyList(),
            emptyList(),
            listOf(AminoAcidInsertion(123, "gene", "ABCD"), AminoAcidInsertion(999, "ORF1", "DEF")),
        )

        val result = underTest.map(filterParameter)

        val expected =
            And(AminoAcidInsertionContains(123, "ABCD", "gene"), AminoAcidInsertionContains(999, "DEF", "ORF1"))
        assertThat(result, equalTo(expected))
    }

    @Test
    fun `given a query with an empty variantQuery then it should throw an error`() {
        val filterParameter = getSequenceFilters(mapOf("variantQuery" to ""))

        val exception = assertThrows<BadRequestException> { underTest.map(filterParameter) }
        assertThat(exception.message, containsString("variantQuery must not be empty"))
    }

    @Test
    fun `given a query with a variantQuery alongside nucleotide mutations then it should throw an error`() {
        val filterParameter = DummySequenceFilters(
            mapOf("variantQuery" to listOf("A123T")),
            listOf(NucleotideMutation(null, 123, null)),
            emptyList(),
            emptyList(),
            emptyList(),
        )

        val exception = assertThrows<BadRequestException> { underTest.map(filterParameter) }
        assertThat(
            exception.message,
            containsString("variantQuery filter cannot be used with other variant filters such as: "),
        )
    }

    @Test
    fun `given a query with a variantQuery alongside amino acid mutations then it should throw an error`() {
        val filterParameter = DummySequenceFilters(
            mapOf("variantQuery" to listOf("A123T")),
            emptyList(),
            listOf(AminoAcidMutation("gene", 123, null)),
            emptyList(),
            emptyList(),
        )

        val exception = assertThrows<BadRequestException> { underTest.map(filterParameter) }
        assertThat(
            exception.message,
            containsString("variantQuery filter cannot be used with other variant filters such as: "),
        )
    }

    @Test
    fun `given a query with a variantQuery alongside pangoLineage filter then it should throw an error`() {
        val filterParameter = getSequenceFilters(
            mapOf(
                "pangoLineage" to "A.1.2.3",
                "variantQuery" to "A123T",
            ),
        )

        val exception = assertThrows<BadRequestException> { underTest.map(filterParameter) }
        assertThat(
            exception.message,
            containsString("variantQuery filter cannot be used with other variant filters such as: "),
        )
    }

    @ParameterizedTest
    @MethodSource("getFilterParametersWithMultipleValues")
    fun `GIVEN multiple value for date field THEN throws an error`(
        sequenceFilters: SequenceFilters,
        expectedErrorMessage: String,
    ) {
        val exception = assertThrows<BadRequestException> { underTest.map(DummySequenceFilters(sequenceFilters)) }

        assertThat(
            exception.message,
            containsString(expectedErrorMessage),
        )
    }

    companion object {
        @JvmStatic
        val filterParametersWithMultipleValues = listOf(
            Arguments.of(
                mapOf(DATE_FIELD to listOf("2021-06-03", "2021-06-04")),
                "Expected exactly one value for '$DATE_FIELD' but got 2 values.",
            ),
            Arguments.of(
                mapOf("dateTo" to listOf("2021-06-03", "2021-06-04")),
                "Expected exactly one value for 'dateTo' but got 2 values.",
            ),
            Arguments.of(
                mapOf("dateFrom" to listOf("2021-06-03", "2021-06-04")),
                "Expected exactly one value for 'dateFrom' but got 2 values.",
            ),
            Arguments.of(
                mapOf("intField" to listOf("1", "2")),
                "Expected exactly one value for 'intField' but got 2 values.",
            ),
            Arguments.of(
                mapOf("intFieldTo" to listOf("1", "2")),
                "Expected exactly one value for 'intFieldTo' but got 2 values.",
            ),
            Arguments.of(
                mapOf("intFieldFrom" to listOf("1", "2")),
                "Expected exactly one value for 'intFieldFrom' but got 2 values.",
            ),
            Arguments.of(
                mapOf("floatField" to listOf("0.1", "0.2")),
                "Expected exactly one value for 'floatField' but got 2 values.",
            ),
            Arguments.of(
                mapOf("floatFieldTo" to listOf("0.1", "0.2")),
                "Expected exactly one value for 'floatFieldTo' but got 2 values.",
            ),
            Arguments.of(
                mapOf("floatFieldFrom" to listOf("0.1", "0.2")),
                "Expected exactly one value for 'floatFieldFrom' but got 2 values.",
            ),
        )

        @JvmStatic
        fun getFilterParametersWithExpectedSiloQuery() =
            listOf(
                Arguments.of(
                    mapOf(
                        "some_metadata" to listOf("ABC"),
                        "other_metadata" to listOf("def"),
                    ),
                    And(
                        Or(StringEquals("some_metadata", "ABC")),
                        Or(StringEquals("other_metadata", "def")),
                    ),
                ),
                Arguments.of(
                    mapOf("pangoLineage" to listOf("A.1.2.3")),
                    And(Or(PangoLineageEquals("pangoLineage", "A.1.2.3", includeSublineages = false))),
                ),
                Arguments.of(
                    mapOf("pangoLineage" to listOf("A.1.2.3*")),
                    And(Or(PangoLineageEquals("pangoLineage", "A.1.2.3", includeSublineages = true))),
                ),
                Arguments.of(
                    mapOf("pangoLineage" to listOf("A.1.2.3.*")),
                    And(Or(PangoLineageEquals("pangoLineage", "A.1.2.3", includeSublineages = true))),
                ),
                Arguments.of(
                    mapOf(
                        "pangoLineage" to listOf("A.1.2.3"),
                        "some_metadata" to listOf("ABC"),
                        "other_metadata" to listOf("DEF"),
                    ),
                    And(
                        listOf(
                            Or(PangoLineageEquals("pangoLineage", "A.1.2.3", includeSublineages = false)),
                            Or(StringEquals("some_metadata", "ABC")),
                            Or(StringEquals("other_metadata", "DEF")),
                        ),
                    ),
                ),
                Arguments.of(
                    mapOf(
                        "date" to listOf("2021-06-03"),
                    ),
                    And(DateBetween("date", from = LocalDate.of(2021, 6, 3), to = LocalDate.of(2021, 6, 3))),
                ),
                Arguments.of(
                    mapOf(
                        "dateTo" to listOf("2021-06-03"),
                    ),
                    And(DateBetween("date", from = null, to = LocalDate.of(2021, 6, 3))),
                ),
                Arguments.of(
                    mapOf(
                        "dateFrom" to listOf("2021-03-28"),
                    ),
                    And(DateBetween("date", from = LocalDate.of(2021, 3, 28), to = null)),
                ),
                Arguments.of(
                    mapOf(
                        "dateFrom" to listOf("2021-03-28"),
                        "dateTo" to listOf("2021-06-03"),
                    ),
                    And(DateBetween("date", from = LocalDate.of(2021, 3, 28), to = LocalDate.of(2021, 6, 3))),
                ),
                Arguments.of(
                    mapOf(
                        "dateTo" to listOf("2021-06-03"),
                        "some_metadata" to listOf("ABC"),
                    ),
                    And(
                        DateBetween("date", from = null, to = LocalDate.of(2021, 6, 3)),
                        Or(StringEquals("some_metadata", "ABC")),
                    ),
                ),
                Arguments.of(
                    mapOf(
                        "variantQuery" to listOf("300G & 400A"),
                    ),
                    And(
                        And(
                            NucleotideSymbolEquals(null, 300, "G"),
                            NucleotideSymbolEquals(null, 400, "A"),
                        ),
                    ),
                ),
                Arguments.of(
                    mapOf(
                        "variantQuery" to listOf("300G"),
                        "some_metadata" to listOf("ABC"),
                    ),
                    And(
                        NucleotideSymbolEquals(null, 300, "G"),
                        Or(StringEquals("some_metadata", "ABC")),
                    ),
                ),
                Arguments.of(
                    mapOf(
                        "intField" to listOf("42"),
                    ),
                    And(IntEquals("intField", 42)),
                ),
                Arguments.of(
                    mapOf(
                        "intFieldFrom" to listOf("42"),
                    ),
                    And(IntBetween("intField", 42, null)),
                ),
                Arguments.of(
                    mapOf(
                        "intFieldTo" to listOf("42"),
                    ),
                    And(IntBetween("intField", null, 42)),
                ),
                Arguments.of(
                    mapOf(
                        "floatField" to listOf("42.45"),
                    ),
                    And(FloatEquals("floatField", 42.45)),
                ),
                Arguments.of(
                    mapOf(
                        "floatFieldFrom" to listOf("42.45"),
                    ),
                    And(FloatBetween("floatField", 42.45, null)),
                ),
                Arguments.of(
                    mapOf(
                        "floatFieldTo" to listOf("42.45"),
                    ),
                    And(FloatBetween("floatField", null, 42.45)),
                ),
                Arguments.of(
                    mapOf(
                        "some_metadata" to listOf("value1", "value2"),
                    ),
                    And(
                        Or(
                            StringEquals("some_metadata", "value1"),
                            StringEquals("some_metadata", "value2"),
                        ),
                    ),
                ),
                Arguments.of(
                    mapOf(
                        "pangoLineage" to listOf("A.1.2.3", "B.1.2.3"),
                    ),
                    And(
                        Or(
                            PangoLineageEquals("pangoLineage", "A.1.2.3", includeSublineages = false),
                            PangoLineageEquals("pangoLineage", "B.1.2.3", includeSublineages = false),
                        ),
                    ),
                ),
            )
    }

    private fun getSequenceFilters(sequenceFilters: Map<String, String>) =
        DummySequenceFilters(
            sequenceFilters.mapValues { listOf(it.value) },
        )

    data class DummySequenceFilters(
        override val sequenceFilters: SequenceFilters,
        override val nucleotideMutations: List<NucleotideMutation> = emptyList(),
        override val aaMutations: List<AminoAcidMutation> = emptyList(),
        override val nucleotideInsertions: List<NucleotideInsertion> = emptyList(),
        override val aminoAcidInsertions: List<AminoAcidInsertion> = emptyList(),
        override val orderByFields: List<OrderByField> = emptyList(),
        override val limit: Int? = null,
        override val offset: Int? = null,
    ) : CommonSequenceFilters
}
