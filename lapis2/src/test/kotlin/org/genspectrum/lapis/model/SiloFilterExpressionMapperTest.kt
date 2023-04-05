package org.genspectrum.lapis.model

import org.genspectrum.lapis.config.SequenceFilterFieldType
import org.genspectrum.lapis.config.SequenceFilterFields
import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.DateBetween
import org.genspectrum.lapis.silo.NucleotideSymbolEquals
import org.genspectrum.lapis.silo.PangoLineageEquals
import org.genspectrum.lapis.silo.SiloFilterExpression
import org.genspectrum.lapis.silo.StringEquals
import org.genspectrum.lapis.silo.True
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

class SiloFilterExpressionMapperTest {
    private val sequenceFilterFields = SequenceFilterFields(
        mapOf(
            "date" to SequenceFilterFieldType.Date,
            "dateTo" to SequenceFilterFieldType.DateTo("date"),
            "dateFrom" to SequenceFilterFieldType.DateFrom("date"),
            "pangoLineage" to SequenceFilterFieldType.PangoLineage,
            "nucleotideMutations" to SequenceFilterFieldType.MutationsList,
            "some_metadata" to SequenceFilterFieldType.String,
            "other_metadata" to SequenceFilterFieldType.String,
        ),
    )

    private lateinit var underTest: SiloFilterExpressionMapper

    @BeforeEach
    fun setup() {
        underTest = SiloFilterExpressionMapper(sequenceFilterFields, VariantQueryFacade())
    }

    @Test
    fun `given invalid filter key then throws exception`() {
        val filterParameter = mapOf("invalid query key" to "some value")

        val exception = assertThrows<IllegalArgumentException> { underTest.map(filterParameter) }

        assertThat(
            exception.message,
            containsString("'invalid query key' is not a valid sequence filter key. Valid keys are:"),
        )
    }

    @Test
    fun `given empty filter parameters then returns a match-all filter`() {
        val filterParameter = emptyMap<String, String>()

        val result = underTest.map(filterParameter)

        assertThat(result, equalTo(True))
    }

    @ParameterizedTest(name = "FilterParameter: {0}, SiloQuery: {1}")
    @MethodSource("getFilterParametersWithExpectedSiloQuery")
    fun `given filter parameters then maps to expected FilterExpression`(
        filterParameter: Map<String, String>,
        expectedResult: SiloFilterExpression,
    ) {
        val result = underTest.map(filterParameter)

        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given invalid date then should throw an exception`() {
        val filterParameter = mapOf("date" to "this is not a date")

        val exception = assertThrows<IllegalArgumentException> { underTest.map(filterParameter) }
        assertThat(exception.message, containsString("date 'this is not a date' is not a valid date"))
    }

    @Test
    fun `given invalid dateTo then should throw an exception`() {
        val filterParameter = mapOf("dateTo" to "this is not a date")

        val exception = assertThrows<IllegalArgumentException> { underTest.map(filterParameter) }
        assertThat(exception.message, containsString("dateTo 'this is not a date' is not a valid date"))
    }

    @Test
    fun `given invalid dateFrom then should throw an exception`() {
        val filterParameter = mapOf("dateFrom" to "this is not a date either")

        val exception = assertThrows<IllegalArgumentException> { underTest.map(filterParameter) }
        assertThat(exception.message, containsString("dateFrom 'this is not a date either' is not a valid date"))
    }

    @Test
    fun `given date and dateFrom then should throw an exception`() {
        val filterParameter = mapOf(
            "date" to "2021-06-03",
            "dateFrom" to "2021-06-03",
        )
        val exception = assertThrows<IllegalArgumentException> { underTest.map(filterParameter) }
        assertThat(
            exception.message,
            containsString("Cannot filter by exact date field 'date' and by date range field 'dateFrom'."),
        )
    }

    @Test
    fun `given date and dateTo then should throw an exception`() {
        val filterParameter = mapOf(
            "date" to "2021-06-03",
            "dateTo" to "2021-06-03",
        )

        val exception = assertThrows<IllegalArgumentException> { underTest.map(filterParameter) }
        assertThat(
            exception.message,
            containsString("Cannot filter by exact date field 'date' and by date range field 'dateTo'."),
        )
    }

    @Test
    fun `given invalid Pango lineage ending with a dot then should throw an exception`() {
        val filterParameter = mapOf("pangoLineage" to "A.1.2.")

        assertThrows<IllegalArgumentException> {
            underTest.map(filterParameter)
        }
    }

    @ParameterizedTest(name = "nucleotideMutations: {0}")
    @MethodSource("getNucleotideMutationWithWrongSyntax")
    fun `given nucleotideMutations with wrong syntax then should throw an exception`(
        invalidMutation: String,
    ) {
        val filterParameter = mapOf("nucleotideMutations" to invalidMutation)

        assertThrows<IllegalArgumentException> {
            underTest.map(filterParameter)
        }
    }

    @ParameterizedTest(name = "nucleotideMutations: {0}")
    @MethodSource("getNucleotideMutationWithValidSyntax")
    fun `given valid mutations then should return the corresponding SiloQuery`(
        validMutation: String,
        expectedResult: SiloFilterExpression,
    ) {
        val filterParameter = mapOf("nucleotideMutations" to validMutation)

        val result = underTest.map(filterParameter)

        assertThat(result, equalTo(expectedResult))
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
                And(listOf(PangoLineageEquals("pangoLineage", "A.1.2.3", includeSublineages = false))),
            ),
            Arguments.of(
                mapOf("pangoLineage" to "A.1.2.3*"),
                And(listOf(PangoLineageEquals("pangoLineage", "A.1.2.3", includeSublineages = true))),
            ),
            Arguments.of(
                mapOf("pangoLineage" to "A.1.2.3.*"),
                And(listOf(PangoLineageEquals("pangoLineage", "A.1.2.3", includeSublineages = true))),
            ),
            Arguments.of(
                mapOf(
                    "pangoLineage" to "A.1.2.3",
                    "some_metadata" to "ABC",
                    "other_metadata" to "DEF",
                ),
                And(
                    listOf(
                        PangoLineageEquals("pangoLineage", "A.1.2.3", includeSublineages = false),
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
                    "date" to "2021-06-03",
                ),
                And(listOf(DateBetween("date", from = LocalDate.of(2021, 6, 3), to = LocalDate.of(2021, 6, 3)))),
            ),
            Arguments.of(
                mapOf(
                    "dateTo" to "2021-06-03",
                ),
                And(listOf(DateBetween("date", from = null, to = LocalDate.of(2021, 6, 3)))),
            ),
            Arguments.of(
                mapOf(
                    "dateFrom" to "2021-03-28",
                ),
                And(listOf(DateBetween("date", from = LocalDate.of(2021, 3, 28), to = null))),
            ),
            Arguments.of(
                mapOf(
                    "dateFrom" to "2021-03-28",
                    "dateTo" to "2021-06-03",
                ),
                And(listOf(DateBetween("date", from = LocalDate.of(2021, 3, 28), to = LocalDate.of(2021, 6, 3)))),
            ),
            Arguments.of(
                mapOf(
                    "dateTo" to "2021-06-03",
                    "some_metadata" to "ABC",
                ),
                And(
                    listOf(
                        DateBetween("date", from = null, to = LocalDate.of(2021, 6, 3)),
                        StringEquals("some_metadata", "ABC"),
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
