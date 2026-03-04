package org.genspectrum.lapis.model

import org.genspectrum.lapis.DATE_FIELD
import org.genspectrum.lapis.FIELD_WITH_UPPERCASE_LETTER
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.config.ReferenceSequenceSchema
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.dummyDatabaseConfig
import org.genspectrum.lapis.dummySequenceFilterFields
import org.genspectrum.lapis.request.AminoAcidInsertion
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.BaseSequenceFilters
import org.genspectrum.lapis.request.CommonSequenceFilters
import org.genspectrum.lapis.request.NucleotideInsertion
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.OrderBySpec
import org.genspectrum.lapis.request.SequenceFilters
import org.genspectrum.lapis.silo.AminoAcidInsertionContains
import org.genspectrum.lapis.silo.AminoAcidSymbolEquals
import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.BooleanEquals
import org.genspectrum.lapis.silo.DateBetween
import org.genspectrum.lapis.silo.FloatBetween
import org.genspectrum.lapis.silo.FloatEquals
import org.genspectrum.lapis.silo.HasAminoAcidMutation
import org.genspectrum.lapis.silo.HasNucleotideMutation
import org.genspectrum.lapis.silo.IntBetween
import org.genspectrum.lapis.silo.IntEquals
import org.genspectrum.lapis.silo.IsNotNull
import org.genspectrum.lapis.silo.IsNull
import org.genspectrum.lapis.silo.LineageEquals
import org.genspectrum.lapis.silo.Maybe
import org.genspectrum.lapis.silo.NucleotideInsertionContains
import org.genspectrum.lapis.silo.NucleotideSymbolEquals
import org.genspectrum.lapis.silo.Or
import org.genspectrum.lapis.silo.PhyloDescendantOf
import org.genspectrum.lapis.silo.SiloFilterExpression
import org.genspectrum.lapis.silo.StringEquals
import org.genspectrum.lapis.silo.StringSearch
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
    private val dummyReferenceGenomeSchema =
        ReferenceGenomeSchema(listOf(ReferenceSequenceSchema("sequenceName")), emptyList())
    private val variantQueryFacade = VariantQueryFacade(dummyReferenceGenomeSchema)
    private val advancedQueryFacade = AdvancedQueryFacade(dummyReferenceGenomeSchema, dummyDatabaseConfig)

    private val underTest =
        SiloFilterExpressionMapper(dummySequenceFilterFields, variantQueryFacade, advancedQueryFacade)

    @ParameterizedTest(name = "GIVEN {0} THEN throws exception")
    @MethodSource("getInvalidFilterScenarios")
    fun `GIVEN invalid filters THEN throws exception`(scenario: InvalidFilterScenario) {
        val exception = assertThrows<BadRequestException> { underTest.map(scenario.filterParameters) }

        assertThat(exception.message, containsString(scenario.expectedErrorMessage))
    }

    @Test
    fun `GIVEN all uppercase key THEN is mapped to corresponding field`() {
        val filterParameter = getSequenceFilters(mapOf("PANGOLINEAGE" to SOME_VALUE))

        val result = underTest.map(filterParameter)

        val expected =
            And(Or(LineageEquals(FIELD_WITH_UPPERCASE_LETTER, SOME_VALUE, includeSublineages = false)))
        assertThat(result, equalTo(expected))
    }

    @Test
    fun `GIVEN all lowercase key THEN is mapped to corresponding field`() {
        val filterParameter = getSequenceFilters(mapOf("pangolineage" to SOME_VALUE))

        val result = underTest.map(filterParameter)

        val expected =
            And(Or(LineageEquals(FIELD_WITH_UPPERCASE_LETTER, SOME_VALUE, includeSublineages = false)))
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
            And(NucleotideInsertionContains(123, "ABCD", "segment"), NucleotideInsertionContains(999, "DEF", null))
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
    fun `given a query with a advancedQuery alongside nucleotide mutations then it returns mapped query`() {
        val filterParameter = DummySequenceFilters(
            mapOf("advancedQuery" to listOf("sequenceName:A124T")),
            listOf(NucleotideMutation("sequenceName", 123, "T")),
            emptyList(),
            emptyList(),
            emptyList(),
        )
        val result = underTest.map(filterParameter)

        assertThat(
            result,
            equalTo(
                And(
                    NucleotideSymbolEquals("sequenceName", 124, "T"),
                    NucleotideSymbolEquals("sequenceName", 123, "T"),
                ),
            ),
        )
    }

    companion object {
        @JvmStatic
        fun getInvalidFilterScenarios() =
            listOf(
                InvalidFilterScenario(
                    description = "invalid query key",
                    filterParameters = getSequenceFilters(mapOf("invalid query key" to SOME_VALUE)),
                    expectedErrorMessage = "'invalid query key' is not a valid sequence filter key. Valid keys are:",
                ),
                InvalidFilterScenario(
                    description = "pango lineage and its corresponding isNull field",
                    filterParameters = getSequenceFilters(
                        mapOf(
                            "pangoLineage" to SOME_VALUE,
                            "pangoLineage.isNull" to "true",
                        ),
                    ),
                    expectedErrorMessage =
                        "Cannot filter for field 'pangoLineage' and 'pangoLineage.isNull' at the same time.",
                ),
            ) +
                invalidStringFilterScenarios +
                invalidDateFilterScenarios +
                invalidIntFilterScenarios +
                invalidFloatFilterScenarios +
                invalidBooleanFilterScenarios +
                invalidAdvancedAndVariantQueryScenarios +
                filterParametersWithMultipleValues

        val invalidStringFilterScenarios = listOf(
            InvalidFilterScenario(
                description = "string field regex that is null",
                filterParameters = DummySequenceFilters(mapOf("some_metadata.regex" to listOf(null))),
                expectedErrorMessage = "String search value for 'some_metadata.regex' must not be null",
            ),
            InvalidFilterScenario(
                description = "string field and its corresponding regex field",
                filterParameters = getSequenceFilters(
                    mapOf(
                        "some_metadata" to "some value",
                        "some_metadata.regex" to "some other value",
                    ),
                ),
                expectedErrorMessage = "Cannot filter for string regex 'some_metadata.regex' " +
                    "and string equals 'some_metadata' for the same field.",
            ),
            InvalidFilterScenario(
                description = "string field and its corresponding isNull field",
                filterParameters = getSequenceFilters(
                    mapOf(
                        "some_metadata" to "some value",
                        "some_metadata.isNull" to "true",
                    ),
                ),
                expectedErrorMessage =
                    "Cannot filter for field 'some_metadata' and 'some_metadata.isNull' at the same time.",
            ),
            InvalidFilterScenario(
                description = "string regex and its corresponding isNull field",
                filterParameters = getSequenceFilters(
                    mapOf(
                        "some_metadata.regex" to "some value",
                        "some_metadata.isNull" to "true",
                    ),
                ),
                expectedErrorMessage =
                    "Cannot filter for field 'some_metadata.regex' and 'some_metadata.isNull' at the same time.",
            ),
            InvalidFilterScenario(
                description = "isNull field with non-boolean value",
                filterParameters = getSequenceFilters(mapOf("some_metadata.isNull" to "not a boolean")),
                expectedErrorMessage = "'not a boolean' is not a valid boolean value for 'some_metadata.isNull'",
            ),
        )

        val invalidDateFilterScenarios = listOf(
            InvalidFilterScenario(
                description = "invalid date value",
                filterParameters = getSequenceFilters(mapOf("date" to "this is not a date")),
                expectedErrorMessage = "date 'this is not a date' is not a valid date",
            ),
            InvalidFilterScenario(
                description = "empty date value",
                filterParameters = getSequenceFilters(mapOf("date" to "")),
                expectedErrorMessage = "date '' is not a valid date",
            ),
            InvalidFilterScenario(
                description = "invalid dateTo value",
                filterParameters = getSequenceFilters(mapOf("dateTo" to "this is not a date")),
                expectedErrorMessage = "dateTo 'this is not a date' is not a valid date",
            ),
            InvalidFilterScenario(
                description = "empty dateTo value",
                filterParameters = getSequenceFilters(mapOf("dateTo" to "")),
                expectedErrorMessage = "dateTo '' is not a valid date",
            ),
            InvalidFilterScenario(
                description = "invalid dateFrom value",
                filterParameters = getSequenceFilters(mapOf("dateFrom" to "this is not a date")),
                expectedErrorMessage = "dateFrom 'this is not a date' is not a valid date",
            ),
            InvalidFilterScenario(
                description = "empty dateFrom value",
                filterParameters = getSequenceFilters(mapOf("dateFrom" to "")),
                expectedErrorMessage = "dateFrom '' is not a valid date",
            ),
            InvalidFilterScenario(
                description = "date and dateFrom at the same time",
                filterParameters = getSequenceFilters(
                    mapOf(
                        "date" to "2021-06-03",
                        "dateFrom" to "2021-06-03",
                    ),
                ),
                expectedErrorMessage =
                    "Cannot filter by exact date field 'date' and by date range field 'dateFrom'.",
            ),
            InvalidFilterScenario(
                description = "date and dateTo at the same time",
                filterParameters = getSequenceFilters(
                    mapOf(
                        "date" to "2021-06-03",
                        "dateTo" to "2021-06-03",
                    ),
                ),
                expectedErrorMessage = "Cannot filter by exact date field 'date' and by date range field 'dateTo'.",
            ),
            InvalidFilterScenario(
                description = "date and its corresponding isNull field",
                filterParameters = getSequenceFilters(
                    mapOf(
                        "date" to "2021-06-03",
                        "date.isNull" to "true",
                    ),
                ),
                expectedErrorMessage = "Cannot filter for field 'date' and 'date.isNull' at the same time.",
            ),
            InvalidFilterScenario(
                description = "dateFrom and its corresponding isNull field",
                filterParameters = getSequenceFilters(
                    mapOf(
                        "dateFrom" to "2021-06-03",
                        "date.isNull" to "true",
                    ),
                ),
                expectedErrorMessage = "Cannot filter for field 'dateFrom' and 'date.isNull' at the same time.",
            ),
            InvalidFilterScenario(
                description = "dateTo and its corresponding isNull field",
                filterParameters = getSequenceFilters(
                    mapOf(
                        "dateTo" to "2021-06-03",
                        "date.isNull" to "true",
                    ),
                ),
                expectedErrorMessage = "Cannot filter for field 'dateTo' and 'date.isNull' at the same time.",
            ),
        )

        val invalidIntFilterScenarios = listOf(
            InvalidFilterScenario(
                description = "int field with non-int value",
                filterParameters = getSequenceFilters(mapOf("intField" to "not a number")),
                expectedErrorMessage = "intField 'not a number' is not a valid integer",
            ),
            InvalidFilterScenario(
                description = "int field with empty value",
                filterParameters = getSequenceFilters(mapOf("intField" to "")),
                expectedErrorMessage = "intField '' is not a valid integer",
            ),
            InvalidFilterScenario(
                description = "intFrom field with non-int value",
                filterParameters = getSequenceFilters(mapOf("intFieldFrom" to "not a number")),
                expectedErrorMessage = "intFieldFrom 'not a number' is not a valid integer",
            ),
            InvalidFilterScenario(
                description = "intFrom field with empty value",
                filterParameters = getSequenceFilters(mapOf("intFieldFrom" to "")),
                expectedErrorMessage = "intFieldFrom '' is not a valid integer",
            ),
            InvalidFilterScenario(
                description = "intTo field with non-int value",
                filterParameters = getSequenceFilters(mapOf("intFieldTo" to "not a number")),
                expectedErrorMessage = "intFieldTo 'not a number' is not a valid integer",
            ),
            InvalidFilterScenario(
                description = "intTo field with empty value",
                filterParameters = getSequenceFilters(mapOf("intFieldTo" to "")),
                expectedErrorMessage = "intFieldTo '' is not a valid integer",
            ),
            InvalidFilterScenario(
                description = "int and intFrom at the same time",
                filterParameters = getSequenceFilters(
                    mapOf(
                        "intField" to "42",
                        "intFieldFrom" to "43",
                    ),
                ),
                expectedErrorMessage =
                    "Cannot filter by exact int field 'intField' and by int range field 'intFieldFrom'.",
            ),
            InvalidFilterScenario(
                description = "int and intTo at the same time",
                filterParameters = getSequenceFilters(
                    mapOf(
                        "intField" to "42",
                        "intFieldTo" to "43",
                    ),
                ),
                expectedErrorMessage =
                    "Cannot filter by exact int field 'intField' and by int range field 'intFieldTo'.",
            ),
            InvalidFilterScenario(
                description = "intField and its corresponding isNull field",
                filterParameters = getSequenceFilters(
                    mapOf(
                        "intField" to "42",
                        "intField.isNull" to "true",
                    ),
                ),
                expectedErrorMessage = "Cannot filter for field 'intField' and 'intField.isNull' at the same time.",
            ),
            InvalidFilterScenario(
                description = "intFieldFrom and its corresponding isNull field",
                filterParameters = getSequenceFilters(
                    mapOf(
                        "intFieldFrom" to "42",
                        "intField.isNull" to "true",
                    ),
                ),
                expectedErrorMessage = "Cannot filter for field 'intFieldFrom' and 'intField.isNull' at the same time.",
            ),
            InvalidFilterScenario(
                description = "intFieldTo and its corresponding isNull field",
                filterParameters = getSequenceFilters(
                    mapOf(
                        "intFieldTo" to "42",
                        "intField.isNull" to "true",
                    ),
                ),
                expectedErrorMessage = "Cannot filter for field 'intFieldTo' and 'intField.isNull' at the same time.",
            ),
        )

        val invalidFloatFilterScenarios = listOf(
            InvalidFilterScenario(
                description = "float field with non-float value",
                filterParameters = getSequenceFilters(mapOf("floatField" to "not a number")),
                expectedErrorMessage = "floatField 'not a number' is not a valid float",
            ),
            InvalidFilterScenario(
                description = "float field with empty value",
                filterParameters = getSequenceFilters(mapOf("floatField" to "")),
                expectedErrorMessage = "floatField '' is not a valid float",
            ),
            InvalidFilterScenario(
                description = "floatFrom field with non-float value",
                filterParameters = getSequenceFilters(mapOf("floatFieldFrom" to "not a number")),
                expectedErrorMessage = "floatFieldFrom 'not a number' is not a valid float",
            ),
            InvalidFilterScenario(
                description = "floatFrom field with empty value",
                filterParameters = getSequenceFilters(mapOf("floatFieldFrom" to "")),
                expectedErrorMessage = "floatFieldFrom '' is not a valid float",
            ),
            InvalidFilterScenario(
                description = "floatTo field with non-float value",
                filterParameters = getSequenceFilters(mapOf("floatFieldTo" to "not a number")),
                expectedErrorMessage = "floatFieldTo 'not a number' is not a valid float",
            ),
            InvalidFilterScenario(
                description = "floatTo field with empty value",
                filterParameters = getSequenceFilters(mapOf("floatFieldTo" to "")),
                expectedErrorMessage = "floatFieldTo '' is not a valid float",
            ),
            InvalidFilterScenario(
                description = "float and floatFrom at the same time",
                filterParameters = getSequenceFilters(
                    mapOf(
                        "floatField" to "42.5",
                        "floatFieldFrom" to "43.5",
                    ),
                ),
                expectedErrorMessage =
                    "Cannot filter by exact float field 'floatField' and by float range field 'floatFieldFrom'.",
            ),
            InvalidFilterScenario(
                description = "float and floatTo at the same time",
                filterParameters = getSequenceFilters(
                    mapOf(
                        "floatField" to "42.5",
                        "floatFieldTo" to "43.5",
                    ),
                ),
                expectedErrorMessage =
                    "Cannot filter by exact float field 'floatField' and by float range field 'floatFieldTo'.",
            ),
            InvalidFilterScenario(
                description = "floatField and its corresponding isNull field",
                filterParameters = getSequenceFilters(
                    mapOf(
                        "floatField" to "42.5",
                        "floatField.isNull" to "true",
                    ),
                ),
                expectedErrorMessage = "Cannot filter for field 'floatField' and 'floatField.isNull' at the same time.",
            ),
            InvalidFilterScenario(
                description = "floatFieldFrom and its corresponding isNull field",
                filterParameters = getSequenceFilters(
                    mapOf(
                        "floatFieldFrom" to "42.5",
                        "floatField.isNull" to "true",
                    ),
                ),
                expectedErrorMessage =
                    "Cannot filter for field 'floatFieldFrom' and 'floatField.isNull' at the same time.",
            ),
            InvalidFilterScenario(
                description = "floatFieldTo and its corresponding isNull field",
                filterParameters = getSequenceFilters(
                    mapOf(
                        "floatFieldTo" to "42.5",
                        "floatField.isNull" to "true",
                    ),
                ),
                expectedErrorMessage =
                    "Cannot filter for field 'floatFieldTo' and 'floatField.isNull' at the same time.",
            ),
        )

        val invalidBooleanFilterScenarios = listOf(
            InvalidFilterScenario(
                description = "boolean field with non-boolean value",
                filterParameters = getSequenceFilters(mapOf("test_boolean_column" to "not a boolean")),
                expectedErrorMessage = "'not a boolean' is not a valid boolean.",
            ),
            InvalidFilterScenario(
                description = "boolean field with empty value",
                filterParameters = getSequenceFilters(mapOf("test_boolean_column" to "")),
                expectedErrorMessage = "'' is not a valid boolean.",
            ),
            InvalidFilterScenario(
                description = "boolean field and its corresponding isNull field",
                filterParameters = getSequenceFilters(
                    mapOf(
                        "test_boolean_column" to "true",
                        "test_boolean_column.isNull" to "true",
                    ),
                ),
                "Cannot filter for field 'test_boolean_column' and 'test_boolean_column.isNull' at the same time.",
            ),
        )

        val invalidAdvancedAndVariantQueryScenarios = listOf(
            InvalidFilterScenario(
                description = "empty variantQuery",
                filterParameters = getSequenceFilters(mapOf("variantQuery" to "")),
                expectedErrorMessage = "variantQuery must not be empty",
            ),
            InvalidFilterScenario(
                description = "empty variantQueries array",
                filterParameters = DummySequenceFilters(mapOf("variantQuery" to emptyList())),
                expectedErrorMessage = "variantQuery must have exactly one value, found 0 values.",
            ),
            InvalidFilterScenario(
                description = "variantQueries array",
                filterParameters = DummySequenceFilters(mapOf("variantQuery" to listOf("A123T", "C123T"))),
                expectedErrorMessage = "variantQuery must have exactly one value, found 2 values.",
            ),
            InvalidFilterScenario(
                description = "variantQuery alongside nucleotide mutations",
                filterParameters = DummySequenceFilters(
                    mapOf("variantQuery" to listOf("A123T")),
                    listOf(NucleotideMutation(null, 123, null)),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                ),
                expectedErrorMessage = "variantQuery filter cannot be used with other variant filters such as: ",
            ),
            InvalidFilterScenario(
                description = "variantQuery alongside amino acid mutations",
                filterParameters = DummySequenceFilters(
                    mapOf("variantQuery" to listOf("A123T")),
                    emptyList(),
                    listOf(AminoAcidMutation("gene", 123, null)),
                    emptyList(),
                    emptyList(),
                ),
                expectedErrorMessage = "variantQuery filter cannot be used with other variant filters such as: ",
            ),
            InvalidFilterScenario(
                description = "variantQuery alongside pangoLineage filter",
                filterParameters = getSequenceFilters(
                    mapOf(
                        "pangoLineage" to "A.1.2.3",
                        "variantQuery" to "A123T",
                    ),
                ),
                expectedErrorMessage = "variantQuery filter cannot be used with other variant filters such as: ",
            ),
            InvalidFilterScenario(
                description = "advancedQuery and variantQuery",
                filterParameters = DummySequenceFilters(
                    mapOf(
                        "advancedQuery" to listOf("A123T"),
                        "variantQuery" to listOf("A123T"),
                    ),
                ),
                expectedErrorMessage = "variantQuery filter cannot be used with advancedQuery filter",
            ),
        )

        val filterParametersWithMultipleValues = listOf(
            InvalidFilterScenario(
                description = "multiple $DATE_FIELD values",
                filterParameters = DummySequenceFilters(mapOf(DATE_FIELD to listOf("2021-06-03", "2021-06-04"))),
                expectedErrorMessage = "Expected exactly one value for '$DATE_FIELD' but got 2 values.",
            ),
            InvalidFilterScenario(
                description = "multiple dateTo values",
                filterParameters = DummySequenceFilters(mapOf("dateTo" to listOf("2021-06-03", "2021-06-04"))),
                expectedErrorMessage = "Expected exactly one value for 'dateTo' but got 2 values.",
            ),
            InvalidFilterScenario(
                description = "multiple dateFrom values",
                filterParameters = DummySequenceFilters(mapOf("dateFrom" to listOf("2021-06-03", "2021-06-04"))),
                expectedErrorMessage = "Expected exactly one value for 'dateFrom' but got 2 values.",
            ),
            InvalidFilterScenario(
                description = "multiple intField values",
                filterParameters = DummySequenceFilters(mapOf("intField" to listOf("1", "2"))),
                expectedErrorMessage = "Expected exactly one value for 'intField' but got 2 values.",
            ),
            InvalidFilterScenario(
                description = "multiple intFieldTo values",
                filterParameters = DummySequenceFilters(mapOf("intFieldTo" to listOf("1", "2"))),
                expectedErrorMessage = "Expected exactly one value for 'intFieldTo' but got 2 values.",
            ),
            InvalidFilterScenario(
                description = "multiple intFieldFrom values",
                filterParameters = DummySequenceFilters(mapOf("intFieldFrom" to listOf("1", "2"))),
                expectedErrorMessage = "Expected exactly one value for 'intFieldFrom' but got 2 values.",
            ),
            InvalidFilterScenario(
                description = "multiple floatField values",
                filterParameters = DummySequenceFilters(mapOf("floatField" to listOf("0.1", "0.2"))),
                expectedErrorMessage = "Expected exactly one value for 'floatField' but got 2 values.",
            ),
            InvalidFilterScenario(
                description = "multiple floatFieldTo values",
                filterParameters = DummySequenceFilters(mapOf("floatFieldTo" to listOf("0.1", "0.2"))),
                expectedErrorMessage = "Expected exactly one value for 'floatFieldTo' but got 2 values.",
            ),
            InvalidFilterScenario(
                description = "multiple floatFieldFrom values",
                filterParameters = DummySequenceFilters(mapOf("floatFieldFrom" to listOf("0.1", "0.2"))),
                expectedErrorMessage = "Expected exactly one value for 'floatFieldFrom' but got 2 values.",
            ),
            InvalidFilterScenario(
                description = "multiple isNull values",
                filterParameters = DummySequenceFilters(mapOf("floatField.isNull" to listOf("true", "false"))),
                expectedErrorMessage = "Expected exactly one value for 'floatField.isNull' but got 2 values.",
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
                    mapOf(
                        "some_metadata" to listOf(null),
                    ),
                    And(
                        Or(IsNull(column = "some_metadata")),
                    ),
                ),
                Arguments.of(
                    mapOf("some_metadata" to listOf("")),
                    And(Or(StringEquals(column = "some_metadata", value = ""))),
                ),
                Arguments.of(
                    mapOf("pangoLineage" to listOf("A.1.2.3")),
                    And(Or(LineageEquals("pangoLineage", "A.1.2.3", includeSublineages = false))),
                ),
                Arguments.of(
                    mapOf("pangoLineage" to listOf("")),
                    And(Or(LineageEquals("pangoLineage", "", includeSublineages = false))),
                ),
                Arguments.of(
                    mapOf("pangoLineage" to listOf(null)),
                    And(Or(IsNull(column = "pangoLineage"))),
                ),
                Arguments.of(
                    mapOf("pangoLineage" to listOf("A.1.2.3*")),
                    And(Or(LineageEquals("pangoLineage", "A.1.2.3", includeSublineages = true))),
                ),
                Arguments.of(
                    mapOf("pangoLineage" to listOf("A.1.2.3.*")),
                    And(Or(LineageEquals("pangoLineage", "A.1.2.3", includeSublineages = true))),
                ),
                Arguments.of(
                    mapOf(
                        "pangoLineage" to listOf("A.1.2.3"),
                        "some_metadata" to listOf("ABC"),
                        "other_metadata" to listOf("DEF"),
                    ),
                    And(
                        listOf(
                            Or(LineageEquals("pangoLineage", "A.1.2.3", includeSublineages = false)),
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
                    mapOf("date" to listOf(null)),
                    And(IsNull(column = "date")),
                ),
                Arguments.of(
                    mapOf(
                        "dateTo" to listOf("2021-06-03"),
                    ),
                    And(DateBetween("date", from = null, to = LocalDate.of(2021, 6, 3))),
                ),
                Arguments.of(
                    mapOf(
                        "dateTo" to listOf(null),
                    ),
                    And(DateBetween("date", from = null, to = null)),
                ),
                Arguments.of(
                    mapOf(
                        "dateFrom" to listOf("2021-03-28"),
                    ),
                    And(DateBetween("date", from = LocalDate.of(2021, 3, 28), to = null)),
                ),
                Arguments.of(
                    mapOf(
                        "dateFrom" to listOf(null),
                    ),
                    And(DateBetween("date", from = null, to = null)),
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
                            NucleotideSymbolEquals(null, 400, "A"),
                            NucleotideSymbolEquals(null, 300, "G"),
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
                        "intField" to listOf(null),
                    ),
                    And(IsNull(column = "intField")),
                ),
                Arguments.of(
                    mapOf(
                        "intFieldFrom" to listOf("42"),
                    ),
                    And(IntBetween("intField", 42, null)),
                ),
                Arguments.of(
                    mapOf(
                        "intFieldFrom" to listOf(null),
                    ),
                    And(IntBetween("intField", null, null)),
                ),
                Arguments.of(
                    mapOf(
                        "intFieldTo" to listOf("42"),
                    ),
                    And(IntBetween("intField", null, 42)),
                ),
                Arguments.of(
                    mapOf(
                        "intFieldTo" to listOf(null),
                    ),
                    And(IntBetween("intField", null, null)),
                ),
                Arguments.of(
                    mapOf(
                        "floatField" to listOf("42.45"),
                    ),
                    And(FloatEquals("floatField", 42.45)),
                ),
                Arguments.of(
                    mapOf(
                        "floatField" to listOf(null),
                    ),
                    And(IsNull("floatField")),
                ),
                Arguments.of(
                    mapOf(
                        "floatFieldFrom" to listOf("42.45"),
                    ),
                    And(FloatBetween("floatField", 42.45, null)),
                ),
                Arguments.of(
                    mapOf(
                        "floatFieldFrom" to listOf(null),
                    ),
                    And(FloatBetween("floatField", null, null)),
                ),
                Arguments.of(
                    mapOf(
                        "floatFieldTo" to listOf("42.45"),
                    ),
                    And(FloatBetween("floatField", null, 42.45)),
                ),
                Arguments.of(
                    mapOf(
                        "floatFieldTo" to listOf(null),
                    ),
                    And(FloatBetween("floatField", null, null)),
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
                            LineageEquals("pangoLineage", "A.1.2.3", includeSublineages = false),
                            LineageEquals("pangoLineage", "B.1.2.3", includeSublineages = false),
                        ),
                    ),
                ),
                Arguments.of(
                    mapOf(
                        "test_boolean_column" to listOf("true", "false", null),
                    ),
                    And(
                        Or(
                            BooleanEquals("test_boolean_column", true),
                            BooleanEquals("test_boolean_column", false),
                            IsNull(column = "test_boolean_column"),
                        ),
                    ),
                ),
                Arguments.of(
                    mapOf(
                        "some_metadata.regex" to listOf("someRegex", "", "otherRegex"),
                    ),
                    And(
                        Or(
                            StringSearch("some_metadata", searchExpression = "someRegex"),
                            StringSearch("some_metadata", searchExpression = ""),
                            StringSearch("some_metadata", searchExpression = "otherRegex"),
                        ),
                    ),
                ),
                Arguments.of(
                    mapOf(
                        "primaryKey.phylodescendantof" to listOf("innerNode"),
                    ),
                    And(
                        PhyloDescendantOf("primaryKey", "innerNode"),
                    ),
                ),
                Arguments.of(
                    mapOf("some_metadata.isNull" to listOf("true")),
                    And(IsNull("some_metadata")),
                ),
                Arguments.of(
                    mapOf("SOME_metadata.ISNULL" to listOf("TRUE")),
                    And(IsNull("some_metadata")),
                ),
                Arguments.of(
                    mapOf("some_metadata.iSnUll" to listOf("true")),
                    And(IsNull("some_metadata")),
                ),
                Arguments.of(
                    mapOf("some_metadata.isNull" to listOf("false")),
                    And(IsNotNull("some_metadata")),
                ),
                Arguments.of(
                    mapOf("intField.isNull" to listOf("true")),
                    And(IsNull("intField")),
                ),
                Arguments.of(
                    mapOf("intField.isNull" to listOf("false")),
                    And(IsNotNull("intField")),
                ),
                Arguments.of(
                    mapOf("floatField.isNull" to listOf("true")),
                    And(IsNull("floatField")),
                ),
                Arguments.of(
                    mapOf("floatField.isNull" to listOf("false")),
                    And(IsNotNull("floatField")),
                ),
                Arguments.of(
                    mapOf("test_boolean_column.isNull" to listOf("true")),
                    And(IsNull("test_boolean_column")),
                ),
                Arguments.of(
                    mapOf("test_boolean_column.isNull" to listOf("false")),
                    And(IsNotNull("test_boolean_column")),
                ),
                Arguments.of(
                    mapOf("pangoLineage.isNull" to listOf("true")),
                    And(IsNull("pangoLineage")),
                ),
                Arguments.of(
                    mapOf("pangoLineage.isNull" to listOf("false")),
                    And(IsNotNull("pangoLineage")),
                ),
                Arguments.of(
                    mapOf("date.isNull" to listOf("true")),
                    And(IsNull("date")),
                ),
                Arguments.of(
                    mapOf("date.isNull" to listOf("false")),
                    And(IsNotNull("date")),
                ),
            )

        private fun getSequenceFilters(sequenceFilters: Map<String, String>) =
            DummySequenceFilters(
                sequenceFilters.mapValues { listOf(it.value) },
            )
    }

    data class DummySequenceFilters(
        override val sequenceFilters: SequenceFilters,
        override val nucleotideMutations: List<NucleotideMutation> = emptyList(),
        override val aminoAcidMutations: List<AminoAcidMutation> = emptyList(),
        override val nucleotideInsertions: List<NucleotideInsertion> = emptyList(),
        override val aminoAcidInsertions: List<AminoAcidInsertion> = emptyList(),
        override val orderByFields: OrderBySpec = OrderBySpec.EMPTY,
        override val limit: Int? = null,
        override val offset: Int? = null,
    ) : CommonSequenceFilters

    data class InvalidFilterScenario(
        val description: String,
        val filterParameters: BaseSequenceFilters,
        val expectedErrorMessage: String,
    ) {
        override fun toString() = description
    }
}
