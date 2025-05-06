package org.genspectrum.lapis.model

import org.genspectrum.lapis.PANGO_LINEAGE_FIELD
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.config.ReferenceSequenceSchema
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.dummyDatabaseConfig
import org.genspectrum.lapis.silo.AminoAcidInsertionContains
import org.genspectrum.lapis.silo.AminoAcidSymbolEquals
import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.DateBetween
import org.genspectrum.lapis.silo.FloatBetween
import org.genspectrum.lapis.silo.HasAminoAcidMutation
import org.genspectrum.lapis.silo.HasNucleotideMutation
import org.genspectrum.lapis.silo.IntBetween
import org.genspectrum.lapis.silo.LineageEquals
import org.genspectrum.lapis.silo.Maybe
import org.genspectrum.lapis.silo.NOf
import org.genspectrum.lapis.silo.Not
import org.genspectrum.lapis.silo.NucleotideInsertionContains
import org.genspectrum.lapis.silo.NucleotideSymbolEquals
import org.genspectrum.lapis.silo.Or
import org.genspectrum.lapis.silo.SiloFilterExpression
import org.genspectrum.lapis.silo.StringEquals
import org.genspectrum.lapis.silo.StringSearch
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

class AdvancedQueryFacadeTest {
    private val dummyReferenceGenomeSchema = ReferenceGenomeSchema(
        listOf(
            ReferenceSequenceSchema("main"),
        ),
        listOf(
            ReferenceSequenceSchema("S"),
            ReferenceSequenceSchema("ORF1a"),
        ),
    )
    private var underTest = AdvancedQueryFacade(dummyReferenceGenomeSchema, dummyDatabaseConfig)

    @Test
    fun `given a complex advanced query then map should return the corresponding SiloQuery`() {
        val advancedQuery =
            "300G & (400- | 500B) & !600 & MAYBE(700B | 800-) & [3-of: 123A, 234T, 345G] & " +
                "pangoLineage=jn.1* & some_metadata.regex='^Democratic.*'"

        val result = underTest.map(advancedQuery)

        val expectedResult = And(
            StringSearch("some_metadata", "^Democratic.*"),
            LineageEquals(PANGO_LINEAGE_COLUMN, "jn.1", true),
            NOf(
                3,
                matchExactly = false,
                listOf(
                    NucleotideSymbolEquals(null, 123, "A"),
                    NucleotideSymbolEquals(null, 234, "T"),
                    NucleotideSymbolEquals(null, 345, "G"),
                ),
            ),
            Maybe(
                Or(
                    NucleotideSymbolEquals(null, 800, "-"),
                    NucleotideSymbolEquals(null, 700, "B"),
                ),
            ),
            Not(HasNucleotideMutation(null, 600)),
            Or(
                NucleotideSymbolEquals(null, 500, "B"),
                NucleotideSymbolEquals(null, 400, "-"),
            ),
            NucleotideSymbolEquals(null, 300, "G"),
        )

        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given a complex advanced query with MAYBE then map should return the corresponding SiloQuery`() {
        val advancedQuery =
            "MAYBE((700B | 800-) & !600 & [3-of: 123A, 234T, 345G]) & " +
                "pangoLineage=jn.1* & some_metadata.regex='^Democratic.*'"

        val result = underTest.map(advancedQuery)

        val expectedResult = And(
            StringSearch("some_metadata", "^Democratic.*"),
            LineageEquals(PANGO_LINEAGE_COLUMN, "jn.1", true),
            Maybe(
                And(
                    NOf(
                        3,
                        matchExactly = false,
                        listOf(
                            NucleotideSymbolEquals(null, 123, "A"),
                            NucleotideSymbolEquals(null, 234, "T"),
                            NucleotideSymbolEquals(null, 345, "G"),
                        ),
                    ),
                    Not(HasNucleotideMutation(null, 600)),
                    Or(
                        NucleotideSymbolEquals(null, 800, "-"),
                        NucleotideSymbolEquals(null, 700, "B"),
                    ),
                ),
            ),
        )

        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given a advancedQuery with a single entry then map should return the corresponding SiloQuery`() {
        val advancedQuery = "300G"

        val result = underTest.map(advancedQuery)

        val expectedResult = NucleotideSymbolEquals(null, 300, "G")
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given a advancedQuery with mutation with position only then should return HasNucleotideMutation filter`() {
        val advancedQuery = "400"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(HasNucleotideMutation(null, 400)))
    }

    @Test
    fun `given a advancedQuery with an 'And' expression then map should return the corresponding SiloQuery`() {
        val advancedQuery = "300G & 400-"

        val result = underTest.map(advancedQuery)

        val expectedResult = And(
            NucleotideSymbolEquals(null, 400, "-"),
            NucleotideSymbolEquals(null, 300, "G"),
        )
        assertThat(result, equalTo(expectedResult))

        val advancedQueryWords = "300G AND 400-"

        val resultWords = underTest.map(advancedQueryWords)

        assertThat(resultWords, equalTo(result))
    }

    @Test
    fun `given a advancedQuery with two 'And' expression then map should return the corresponding SiloQuery`() {
        val advancedQuery = "300G & 400- & 500B"

        val result = underTest.map(advancedQuery)

        val expectedResult = And(
            NucleotideSymbolEquals(null, 500, "B"),
            NucleotideSymbolEquals(null, 400, "-"),
            NucleotideSymbolEquals(null, 300, "G"),
        )
        assertThat(result, equalTo(expectedResult))

        val advancedQueryWords = "300G & 400- AND 500B"

        val resultWords = underTest.map(advancedQueryWords)

        assertThat(resultWords, equalTo(result))
    }

    @Test
    fun `given a advancedQuery with a 'Not' expression then map should return the corresponding SiloQuery`() {
        val advancedQuery = "!300G"

        val result = underTest.map(advancedQuery)

        val expectedResult = Not(NucleotideSymbolEquals(null, 300, "G"))
        assertThat(result, equalTo(expectedResult))

        val advancedQueryWords = "NOT 300G"

        val resultWords = underTest.map(advancedQueryWords)

        assertThat(resultWords, equalTo(result))
    }

    @Test
    fun `given a variant advancedQuery with an 'Or' expression then map should return the corresponding SiloQuery`() {
        val advancedQuery = "300G | 400-"

        val result = underTest.map(advancedQuery)

        val expectedResult = Or(
            NucleotideSymbolEquals(null, 400, "-"),
            NucleotideSymbolEquals(null, 300, "G"),
        )
        assertThat(result, equalTo(expectedResult))

        val advancedQueryWords = "300G OR 400-"

        val resultWords = underTest.map(advancedQueryWords)

        assertThat(resultWords, equalTo(result))
    }

    @Test
    fun `given a advancedQuery with an bracket expression then map should return the corresponding SiloQuery`() {
        val advancedQuery = "300C & (400A | 500G)"

        val result = underTest.map(advancedQuery)

        val expectedResult = And(
            Or(
                NucleotideSymbolEquals(null, 500, "G"),
                NucleotideSymbolEquals(null, 400, "A"),
            ),
            NucleotideSymbolEquals(null, 300, "C"),
        )
        assertThat(result, equalTo(expectedResult))

        val advancedQueryWords = "300C AND (400A OR 500G)"

        val resultWords = underTest.map(advancedQueryWords)

        assertThat(resultWords, equalTo(result))
    }

    @Test
    fun `given a advancedQuery with a 'Maybe' expression then map should return the corresponding SiloQuery`() {
        val advancedQuery = "MAYBE(300G)"

        val result = underTest.map(advancedQuery)

        val expectedResult = Maybe(NucleotideSymbolEquals(null, 300, "G"))
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `GIVEN a advancedQuery with a mixed-case 'Maybe' expression THEN map should return 'Maybe' SiloQuery`() {
        val advancedQuery = "maYbE(T12C)"

        val result = underTest.map(advancedQuery)

        val expectedResult = Maybe(NucleotideSymbolEquals(null, 12, "C"))
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given a advancedQuery with a 'Pangolineage' expression then map should return the corresponding SiloQuery`() {
        val advancedQuery = "pangolineage=A.1.2.3"

        val result = underTest.map(advancedQuery)

        val expectedResult = LineageEquals(PANGO_LINEAGE_FIELD, "A.1.2.3", false)
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `given a advancedQuery with a 'Pangolineage' expression with casing then map should return the corresponding SiloQuery`() {
        val advancedQuery = "Pangolineage=A.1.2.3*"

        val result = underTest.map(advancedQuery)

        val expectedResult = LineageEquals(PANGO_LINEAGE_COLUMN, "A.1.2.3", true)
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given a advancedQuery with a 'Nof' expression then map should return the corresponding SiloQuery`() {
        val advancedQuery = "[3-of: 123A, 234T, 345G, 456A]"

        val result = underTest.map(advancedQuery)

        val expectedResult = NOf(
            3,
            false,
            listOf(
                NucleotideSymbolEquals(null, 123, "A"),
                NucleotideSymbolEquals(null, 234, "T"),
                NucleotideSymbolEquals(null, 345, "G"),
                NucleotideSymbolEquals(null, 456, "A"),
            ),
        )
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given a advancedQuery with a exact 'Nof' expression then map should return the corresponding SiloQuery`() {
        val advancedQuery = "[exactly-3-of: 123A, 234T, 345G, 456A]"

        val result = underTest.map(advancedQuery)

        val expectedResult = NOf(
            3,
            true,
            listOf(
                NucleotideSymbolEquals(null, 123, "A"),
                NucleotideSymbolEquals(null, 234, "T"),
                NucleotideSymbolEquals(null, 345, "G"),
                NucleotideSymbolEquals(null, 456, "A"),
            ),
        )
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `given a advancedQuery with a nested exact 'Nof' expression then map should return the corresponding SiloQuery`() {
        val advancedQuery = "[exactly-3-of: 123A, !234G, 345G, 456A]"

        val result = underTest.map(advancedQuery)

        val expectedResult = NOf(
            3,
            true,
            listOf(
                NucleotideSymbolEquals(null, 123, "A"),
                Not(NucleotideSymbolEquals(null, 234, "G")),
                NucleotideSymbolEquals(null, 345, "G"),
                NucleotideSymbolEquals(null, 456, "A"),
            ),
        )
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `given a advancedQuery with a exact 'Nof' expression with casing then map should return the corresponding SiloQuery`() {
        val advancedQuery = "[exAcTly-3-oF: 123A, 234T, 345G]"

        val result = underTest.map(advancedQuery)

        val expectedResult = NOf(
            3,
            true,
            listOf(
                NucleotideSymbolEquals(null, 123, "A"),
                NucleotideSymbolEquals(null, 234, "T"),
                NucleotideSymbolEquals(null, 345, "G"),
            ),
        )
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given a advancedQuery with a 'Insertion' expression then returns SILO query`() {
        val advancedQuery = "ins_1234:GAG"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(NucleotideInsertionContains(1234, "GAG", null)))
    }

    @Test
    fun `given a advancedQuery with a 'Insertion' expression with invalid sequenceName throws`() {
        val advancedQuery = "ins_sequence:1234:GAG"

        assertThrows<BadRequestException> { underTest.map(advancedQuery) }
    }

    @Test
    fun `given a advancedQuery with a 'Insertion' expression with lower case letters then returns SILO query`() {
        val advancedQuery = "ins_1234:gAG"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(NucleotideInsertionContains(1234, "GAG", null)))
    }

    @Test
    fun `given a advancedQuery with a 'Insertion' expression with casing letters then returns SILO query`() {
        val advancedQuery = "iNs_1234:gAG"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(NucleotideInsertionContains(1234, "GAG", null)))
    }

    @Test
    fun `given a advancedQuery with a 'Insertion' with wildcard expression then returns SILO query`() {
        val advancedQuery = "ins_1234:G?A?G"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(NucleotideInsertionContains(1234, "G.*A.*G", null)))
    }

    @Test
    fun `given amino acid mutation expression then should map to AminoAcidSymbolEquals`() {
        val advancedQuery = "S:N501Y"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(AminoAcidSymbolEquals("S", 501, "Y")))
    }

    @Test
    fun `given amino acid mutation expression with lower case letters then should map to AminoAcidSymbolEquals`() {
        val advancedQuery = "S:n501y"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(AminoAcidSymbolEquals("S", 501, "Y")))
    }

    @Test
    fun `given amino acid mutation expression with gene lower case letters then should map to AminoAcidSymbolEquals`() {
        val advancedQuery = "orf1a:N501Y"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(AminoAcidSymbolEquals("ORF1a", 501, "Y")))
    }

    @Test
    fun `given amino acid mutation expression without first symbol then should map to AminoAcidSymbolEquals`() {
        val advancedQuery = "S:501Y"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(AminoAcidSymbolEquals("S", 501, "Y")))
    }

    @Test
    fun `given amino acid mutation expression without second symbol then should return HasAminoAcidMutation`() {
        val advancedQuery = "S:N501"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(HasAminoAcidMutation("S", 501)))
    }

    @Test
    fun `given amino acid mutation expression without any symbol then should return HasAminoAcidMutation`() {
        val advancedQuery = "S:501"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(HasAminoAcidMutation("S", 501)))
    }

    @Test
    fun `given a valid advancedQuery with a 'AA insertion' expression then returns SILO query`() {
        val advancedQuery = "ins_S:501:EPE"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(AminoAcidInsertionContains(501, "EPE", "S")))
    }

    @Test
    fun `given a valid advancedQuery with a stop codon expression then returns SILO query`() {
        val advancedQuery = "ins_S:501:A*C"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(AminoAcidInsertionContains(501, "A\\*C", "S")))
    }

    @Test
    fun `given a valid advancedQuery with a 'AA insertion' expression with lower case then returns SILO query`() {
        val advancedQuery = "ins_ORF1a:501:ePe"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(AminoAcidInsertionContains(501, "EPE", "ORF1a")))
    }

    @Test
    fun `given an invalid advancedQuery with an invalid gene return error`() {
        val advancedQuery = "ins_invalidGene:501:EPE"

        val exception = assertThrows<BadRequestException> { underTest.map(advancedQuery) }

        assertThat(exception.message, `is`("invalidGene is not a known segment or gene"))
    }

    @Test
    fun `given a valid advancedQuery with a 'AA insertion' expression with lower case gene then returns SILO query`() {
        val advancedQuery = "ins_orF1a:501:EPE"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(AminoAcidInsertionContains(501, "EPE", "ORF1a")))
    }

    @Test
    fun `given a valid advancedQuery with a 'AA insertion' with wildcard then returns SILO query`() {
        val advancedQuery = "ins_S:501:E?E?"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(AminoAcidInsertionContains(501, "E.*E.*", "S")))
    }

    @Test
    fun `given a valid advancedQuery with a 'AA insertion' with stop codon and wildcard then returns SILO query`() {
        val advancedQuery = "ins_S:501:E?*E"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(AminoAcidInsertionContains(501, "E.*\\*E", "S")))
    }

    @Test
    fun `given a valid advancedQuery with string metadata expression then returns SILO query`() {
        val advancedQuery = "some_metadata=AB"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(StringEquals("some_metadata", "AB")))
    }

    @Test
    fun `given a valid advancedQuery with mutation and metadata expression then returns SILO query`() {
        val advancedQuery = "(NOT some_metadata=AB) & 300G"

        val result = underTest.map(advancedQuery)

        val expectedResult = And(
            NucleotideSymbolEquals(null, 300, "G"),
            Not(StringEquals("some_metadata", "AB")),
        )

        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given a valid advancedQuery with mutation and regex metadata expression then returns SILO query`() {
        val advancedQuery = "(some_metadata=BANGALOR AND 300G)&(some_metadata.regex='BANGALOR' OR NOT S:501Y)"

        val result = underTest.map(advancedQuery)

        val expectedResult = And(
            Or(
                Not(AminoAcidSymbolEquals("S", 501, "Y")),
                StringSearch("some_metadata", "BANGALOR"),
            ),
            NucleotideSymbolEquals(null, 300, "G"),
            StringEquals("some_metadata", "BANGALOR"),
        )

        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given a valid advancedQuery with string (with whitespace) metadata expression then returns SILO query`() {
        val advancedQuery = "some_metadata='Democratic Republic of the Congo'"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(StringEquals("some_metadata", "Democratic Republic of the Congo")))
    }

    @Test
    fun `given a valid advancedQuery with string (with regex) metadata expression then returns SILO query`() {
        val advancedQuery = "some_metadata.regex='Basel\\{1,2\\}'"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(StringSearch("some_metadata", "Basel\\{1,2\\}")))
    }

    @Test
    fun `given a valid advancedQuery with escaped char in regex then returns SILO query`() {
        val advancedQuery = "some_metadata.regex='(Democratic.*Rep$'"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(StringSearch("some_metadata", "(Democratic.*Rep$")))
    }

    @Test
    fun `given a valid advancedQuery with date metadata expression then returns SILO query`() {
        val advancedQuery = "date=2020-01-01"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(DateBetween("date", LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-01"))))
    }

    data class ValidTestCase(
        val description: String,
        val query: String,
        val expected: SiloFilterExpression,
    )

    data class InvalidTestCase(
        val description: String,
        val query: String,
        val expected: String,
    )

    companion object {
        @JvmStatic
        fun validRangeQueryProvider() =
            listOf(
                ValidTestCase(
                    "intField",
                    "intField>=1 AND intField<=18",
                    And(
                        IntBetween("intField", null, 18),
                        IntBetween("intField", 1, null),
                    ),
                ),
                ValidTestCase(
                    "floatField",
                    "floatField>=0 AND floatField<=10e-1",
                    And(
                        FloatBetween("floatField", null, 1.0),
                        FloatBetween("floatField", 0.0, null),
                    ),
                ),
                ValidTestCase(
                    "date",
                    "date>=2020-01-01 AND date<=2021-01-01",
                    And(
                        DateBetween("date", null, LocalDate.parse("2021-01-01")),
                        DateBetween("date", LocalDate.parse("2020-01-01"), null),
                    ),
                ),
            ).map { arrayOf(it.description, it.query, it.expected) }

        @JvmStatic
        fun invalidRangeQueryProvider() =
            listOf(
                InvalidTestCase(
                    "date",
                    "date<=2021.01.01",
                    "'2021.01.01' is not a valid date",
                ),
                InvalidTestCase(
                    "metadata",
                    "some_metadata<=2021.01.01",
                    "expression <= cannot be used for STRING",
                ),
                InvalidTestCase(
                    "intField",
                    "intField>=One",
                    "'One' is not a valid int",
                ),
                InvalidTestCase(
                    "floatField",
                    "floatField>=One",
                    "'One' is not a valid float",
                ),
            ).map { arrayOf(it.description, it.query, it.expected) }

        @JvmStatic
        fun invalidMetadataProvider() =
            listOf(
                InvalidTestCase(
                    "dateFrom",
                    "dateFrom=2020-01-01",
                    "Metadata field dateFrom does not exist",
                ),
                InvalidTestCase(
                    "dateTo",
                    "dateTo=2020-01-01",
                    "Metadata field dateTo does not exist",
                ),
                InvalidTestCase(
                    "intFieldFrom",
                    "intFieldFrom=1",
                    "Metadata field intFieldFrom does not exist",
                ),
                InvalidTestCase(
                    "intFieldTo",
                    "intFieldTo=1",
                    "Metadata field intFieldTo does not exist",
                ),
                InvalidTestCase(
                    "floatFieldFrom",
                    "floatFieldFrom=1",
                    "Metadata field floatFieldFrom does not exist",
                ),
                InvalidTestCase(
                    "floatFieldTo",
                    "floatFieldTo=1",
                    "Metadata field floatFieldTo does not exist",
                ),
            ).map { arrayOf(it.description, it.query, it.expected) }
    }

    @ParameterizedTest(name = "valid {0} with >= and <=")
    @MethodSource("validRangeQueryProvider")
    fun `test valid advanced queries`(
        description: String,
        query: String,
        expected: SiloFilterExpression,
    ) {
        val result = underTest.map(query)
        assertThat(result, equalTo(expected))
    }

    @ParameterizedTest(name = "invalid {0} with >= and <=")
    @MethodSource("invalidRangeQueryProvider")
    fun `test invalid advanced queries`(
        description: String,
        query: String,
        expected: String,
    ) {
        val exception = assertThrows<BadRequestException> { underTest.map(query) }
        assertThat(exception.message, containsString(expected))
    }

    @ParameterizedTest(name = "invalid metadata field {0}")
    @MethodSource("invalidMetadataProvider")
    fun `given a advancedQuery with xFrom or xTo field throw BadRequestException`(
        description: String,
        query: String,
        expected: String,
    ) {
        val exception =
            assertThrows<BadRequestException> { underTest.map(query) }
        assertThat(
            exception.message,
            `is`(
                expected,
            ),
        )
    }

    @Test
    fun `given a advancedQuery with not allowed fields in IsNull throw BadRequestException`() {
        val advancedQueries =
            listOf(
                "dateFrom=2020-01-01",
                "dateTo=2020-01-01",
                "intFieldFrom=1",
                "intFieldTo=1",
                "floatFieldFrom=1",
                "floatFieldTo=1",
                "metadata.regex=1",
            )
        for (advancedQuery in advancedQueries) {
            val query = "IsNull(%s)".format(advancedQuery)
            val exception = assertThrows<BadRequestException> { underTest.map(query) }
        }
    }

    @Test
    fun `GIVEN an invalid advanced regex query THEN throw bad request exception`() {
        val advancedQuery = "date='jn.1* thisIsInvalid'"

        val exception = assertThrows<BadRequestException> { underTest.map(advancedQuery) }

        assertThat(
            exception.message,
            `is`(
                "Query expression for metadata field `date` of type DATE was wrapped in `'`- this is only allowed for string searches and regex queries",
            ),
        )
    }

    @Test
    fun `GIVEN an invalid advanced query where MAYBE is used fro metadata THEN throw bad request exception`() {
        val advancedQuery = "MAYBE(PangoLineage=jn.1* AND country=Switzerland)"

        val exception = assertThrows<BadRequestException> { underTest.map(advancedQuery) }

        assertThat(
            exception.message,
            `is`(
                "Failed to parse advanced query (line 1:18): mismatched input '=' expecting {':', A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z, '-', '_', '.', '*', NUMBER}.",
            ),
        )
    }

    @Test
    fun `GIVEN an invalid advanced query THEN throw bad request exception`() {
        val advancedQuery = "PangoLineage=jn.1* AND thisIsInvalid"

        val exception = assertThrows<BadRequestException> { underTest.map(advancedQuery) }

        assertThat(
            exception.message,
            `is`("Failed to parse advanced query (line 1:36): no viable alternative at input 'thisIsInvalid'."),
        )
    }
}
