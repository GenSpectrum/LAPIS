package org.genspectrum.lapis.model

import org.genspectrum.lapis.PANGO_LINEAGE_FIELD
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.config.ReferenceSequenceSchema
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.dummySequenceFilterFields
import org.genspectrum.lapis.silo.AminoAcidInsertionContains
import org.genspectrum.lapis.silo.AminoAcidSymbolEquals
import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.DateBetween
import org.genspectrum.lapis.silo.HasAminoAcidMutation
import org.genspectrum.lapis.silo.HasNucleotideMutation
import org.genspectrum.lapis.silo.LineageEquals
import org.genspectrum.lapis.silo.Maybe
import org.genspectrum.lapis.silo.NOf
import org.genspectrum.lapis.silo.Not
import org.genspectrum.lapis.silo.NucleotideInsertionContains
import org.genspectrum.lapis.silo.NucleotideSymbolEquals
import org.genspectrum.lapis.silo.Or
import org.genspectrum.lapis.silo.StringEquals
import org.genspectrum.lapis.silo.StringSearch
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
    private var underTest = AdvancedQueryFacade(dummyReferenceGenomeSchema, dummySequenceFilterFields)

    @Test
    fun `given a complex variant query then map should return the corresponding SiloQuery`() {
        val advancedQuery =
            "300G & (400- | 500B) & !600 & MAYBE(700B | 800-) & [3-of: 123A, 234T, 345G] & " +
                "pangoLineage=jn.1* & some_metadata.regex='^Democratic.*'"

        val result = underTest.map(advancedQuery)

        val expectedResult = And(
            StringSearch("some_metadata.regex", "'^Democratic.*'"),
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
    }

    @Test
    fun `given a advancedQuery with a 'Not' expression then map should return the corresponding SiloQuery`() {
        val advancedQuery = "!300G"

        val result = underTest.map(advancedQuery)

        val expectedResult = Not(NucleotideSymbolEquals(null, 300, "G"))
        assertThat(result, equalTo(expectedResult))
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
    fun `given a advancedQuery with a 'Insertion' expression with sequenceName throws`() {
        //  COVID variant queries do not support segment name in insertion queries
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

        assertThat(exception.message, `is`("Unknown gene from lower case: invalidgene"))
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
                StringSearch("some_metadata.regex", "'BANGALOR'"),
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

        assertThat(result, equalTo(StringEquals("some_metadata", "'Democratic Republic of the Congo'")))
    }

    @Test
    fun `given a valid advancedQuery with string (with regex) metadata expression then returns SILO query`() {
        val advancedQuery = "some_metadata.regex='(Democratic.*Rep$'"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(StringSearch("some_metadata.regex", "'(Democratic.*Rep$'")))
    }

    @Test
    fun `given a valid advancedQuery with date metadata expression then returns SILO query`() {
        val advancedQuery = "date=2020-01-01"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(DateBetween("date", LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-01"))))
    }

    @Test
    fun `given a valid advancedQuery with a 'nextcladePangoLineage' expression then returns SILO query`() {
        val advancedQuery = "pangoLineage=jn.1*"

        val result = underTest.map(advancedQuery)

        assertThat(
            result,
            equalTo(LineageEquals(PANGO_LINEAGE_FIELD, "jn.1", true)),
        )
    }

    @Test
    fun `GIVEN an invalid variant query THEN throw bad request exception`() {
        val advancedQuery = "pangoLineage='jn.1* thisIsInvalid'"

        val exception = assertThrows<BadRequestException> { underTest.map(advancedQuery) }

        assertThat(
            exception.message,
            `is`(
                "Expression contains symbols not allowed for metadata field of type Lineage (allowed symbols: a-z, A-Z, 0-9, ., *, -, _)",
            ),
        )
    }
}
