package org.genspectrum.lapis.model

import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.config.ReferenceSequenceSchema
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.silo.AminoAcidInsertionContains
import org.genspectrum.lapis.silo.AminoAcidSymbolEquals
import org.genspectrum.lapis.silo.And
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
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VariantQueryFacadeTest {
    private val dummyReferenceGenomeSchema = ReferenceGenomeSchema(
        listOf(
            ReferenceSequenceSchema("main"),
        ),
        listOf(
            ReferenceSequenceSchema("S"),
            ReferenceSequenceSchema("ORF1a"),
        ),
    )
    private val underTest = VariantQueryFacade(dummyReferenceGenomeSchema)

    @Test
    fun `given a complex variant query then map should return the corresponding SiloQuery`() {
        val variantQuery =
            "300G & (400- | 500B) & !600 & MAYBE(700B | 800-) & [3-of: 123A, 234T, 345G] & " +
                "nextcladePangoLineage:jn.1* & A.1.2.3*"

        val result = underTest.map(variantQuery)

        val expectedResult =
            And(
                LineageEquals(PANGO_LINEAGE_COLUMN, "A.1.2.3", true),
                LineageEquals(NEXTCLADE_PANGO_LINEAGE_COLUMN, "jn.1", true),
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
    fun `given a variantQuery with a single entry then map should return the corresponding SiloQuery`() {
        val variantQuery = "300G"

        val result = underTest.map(variantQuery)

        val expectedResult = NucleotideSymbolEquals(null, 300, "G")
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given a variantQuery with mutation with position only then should return HasNucleotideMutation filter`() {
        val variantQuery = "400"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(HasNucleotideMutation(null, 400)))
    }

    @Test
    fun `given a variantQuery with an 'And' expression then map should return the corresponding SiloQuery`() {
        val variantQuery = "300G & 400-"

        val result = underTest.map(variantQuery)

        val expectedResult = And(
            NucleotideSymbolEquals(null, 400, "-"),
            NucleotideSymbolEquals(null, 300, "G"),
        )
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given a variantQuery with two 'And' expression then map should return the corresponding SiloQuery`() {
        val variantQuery = "300G & 400- & 500B"

        val result = underTest.map(variantQuery)

        val expectedResult = And(
            NucleotideSymbolEquals(null, 500, "B"),
            NucleotideSymbolEquals(null, 400, "-"),
            NucleotideSymbolEquals(null, 300, "G"),
        )
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given a variantQuery with a 'Not' expression then map should return the corresponding SiloQuery`() {
        val variantQuery = "!300G"

        val result = underTest.map(variantQuery)

        val expectedResult = Not(NucleotideSymbolEquals(null, 300, "G"))
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given a variant variantQuery with an 'Or' expression then map should return the corresponding SiloQuery`() {
        val variantQuery = "300G | 400-"

        val result = underTest.map(variantQuery)

        val expectedResult = Or(
            NucleotideSymbolEquals(null, 400, "-"),
            NucleotideSymbolEquals(null, 300, "G"),
        )
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given a variantQuery with an bracket expression then map should return the corresponding SiloQuery`() {
        val variantQuery = "300C & (400A | 500G)"

        val result = underTest.map(variantQuery)

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
    fun `given a variantQuery with a 'Maybe' expression then map should return the corresponding SiloQuery`() {
        val variantQuery = "MAYBE(300G)"

        val result = underTest.map(variantQuery)

        val expectedResult = Maybe(NucleotideSymbolEquals(null, 300, "G"))
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `GIVEN a variantQuery with a mixed-case 'Maybe' expression THEN map should return 'Maybe' SiloQuery`() {
        val variantQuery = "maYbE(T12C)"

        val result = underTest.map(variantQuery)

        val expectedResult = Maybe(NucleotideSymbolEquals(null, 12, "C"))
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given a variantQuery with a 'Pangolineage' expression then map should return the corresponding SiloQuery`() {
        val variantQuery = "A.1.2.3"

        val result = underTest.map(variantQuery)

        val expectedResult = LineageEquals(PANGO_LINEAGE_COLUMN, "A.1.2.3", false)
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `given a variantQuery with a 'Pangolineage' expression (including sublineages) then map should return the corresponding SiloQuery`() {
        val variantQuery = "A.1.2.3*"

        val result = underTest.map(variantQuery)

        val expectedResult = LineageEquals(PANGO_LINEAGE_COLUMN, "A.1.2.3", true)
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `given a variantQuery with a 'NextcladePangolineage' expression then map should return the corresponding SiloQuery`() {
        val variantQuery = "nextcladePangoLineage:A.1.2.3*"

        val result = underTest.map(variantQuery)

        val expectedResult = LineageEquals(NEXTCLADE_PANGO_LINEAGE_COLUMN, "A.1.2.3", true)
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `given a variantQuery with a 'NextcladePangolineage' expression with casing then map should return the corresponding SiloQuery`() {
        val variantQuery = "NeXtcladePaNgoLineage:A.1.2.3*"

        val result = underTest.map(variantQuery)

        val expectedResult = LineageEquals(NEXTCLADE_PANGO_LINEAGE_COLUMN, "A.1.2.3", true)
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given a variantQuery with a 'Nof' expression then map should return the corresponding SiloQuery`() {
        val variantQuery = "[3-of: 123A, 234T, 345G, 456A]"

        val result = underTest.map(variantQuery)

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
    fun `given a variantQuery with a exact 'Nof' expression then map should return the corresponding SiloQuery`() {
        val variantQuery = "[exactly-3-of: 123A, 234T, 345G, 456A]"

        val result = underTest.map(variantQuery)

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
    fun `given a variantQuery with a nested exact 'Nof' expression then map should return the corresponding SiloQuery`() {
        val variantQuery = "[exactly-3-of: 123A, !234G, 345G, 456A]"

        val result = underTest.map(variantQuery)

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
    fun `given a variantQuery with a exact 'Nof' expression with casing then map should return the corresponding SiloQuery`() {
        val variantQuery = "[exAcTly-3-oF: 123A, 234T, 345G]"

        val result = underTest.map(variantQuery)

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
    fun `given a variantQuery with a 'Insertion' expression then returns SILO query`() {
        val variantQuery = "ins_1234:GAG"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(NucleotideInsertionContains(1234, "GAG", null)))
    }

    @Test
    fun `given a variantQuery with a 'Insertion' expression with sequenceName throws`() {
        //  COVID variant queries do not support segment name in insertion queries
        val variantQuery = "ins_sequence:1234:GAG"

        assertThrows<BadRequestException> { underTest.map(variantQuery) }
    }

    @Test
    fun `given a variantQuery with a 'Insertion' expression with lower case letters then returns SILO query`() {
        val variantQuery = "ins_1234:gAG"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(NucleotideInsertionContains(1234, "GAG", null)))
    }

    @Test
    fun `given a variantQuery with a 'Insertion' expression with casing letters then returns SILO query`() {
        val variantQuery = "iNs_1234:gAG"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(NucleotideInsertionContains(1234, "GAG", null)))
    }

    @Test
    fun `given a variantQuery with a 'Insertion' with wildcard expression then returns SILO query`() {
        val variantQuery = "ins_1234:G?A?G"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(NucleotideInsertionContains(1234, "G.*A.*G", null)))
    }

    @Test
    fun `given amino acid mutation expression then should map to AminoAcidSymbolEquals`() {
        val variantQuery = "S:N501Y"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(AminoAcidSymbolEquals("S", 501, "Y")))
    }

    @Test
    fun `given amino acid mutation expression with lower case letters then should map to AminoAcidSymbolEquals`() {
        val variantQuery = "S:n501y"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(AminoAcidSymbolEquals("S", 501, "Y")))
    }

    @Test
    fun `given amino acid mutation expression with gene lower case letters then should map to AminoAcidSymbolEquals`() {
        val variantQuery = "orf1a:N501Y"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(AminoAcidSymbolEquals("ORF1a", 501, "Y")))
    }

    @Test
    fun `given amino acid mutation expression without first symbol then should map to AminoAcidSymbolEquals`() {
        val variantQuery = "S:501Y"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(AminoAcidSymbolEquals("S", 501, "Y")))
    }

    @Test
    fun `given amino acid mutation expression without second symbol then should return HasAminoAcidMutation`() {
        val variantQuery = "S:N501"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(HasAminoAcidMutation("S", 501)))
    }

    @Test
    fun `given amino acid mutation expression without any symbol then should return HasAminoAcidMutation`() {
        val variantQuery = "S:501"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(HasAminoAcidMutation("S", 501)))
    }

    @Test
    fun `given a valid variantQuery with a 'AA insertion' expression then returns SILO query`() {
        val variantQuery = "ins_S:501:EPE"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(AminoAcidInsertionContains(501, "EPE", "S")))
    }

    @Test
    fun `given a valid variantQuery with a stop codon expression then returns SILO query`() {
        val variantQuery = "ins_S:501:A*C"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(AminoAcidInsertionContains(501, "A\\*C", "S")))
    }

    @Test
    fun `given a valid variantQuery with a 'AA insertion' expression with lower case then returns SILO query`() {
        val variantQuery = "ins_ORF1a:501:ePe"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(AminoAcidInsertionContains(501, "EPE", "ORF1a")))
    }

    @Test
    fun `given a valid variantQuery with a 'AA insertion' expression with lower case gene then returns SILO query`() {
        val variantQuery = "ins_orF1a:501:EPE"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(AminoAcidInsertionContains(501, "EPE", "ORF1a")))
    }

    @Test
    fun `given a valid variantQuery with a 'AA insertion' with wildcard then returns SILO query`() {
        val variantQuery = "ins_S:501:E?E?"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(AminoAcidInsertionContains(501, "E.*E.*", "S")))
    }

    @Test
    fun `given a valid variantQuery with a 'AA insertion' with stop codon and wildcard then returns SILO query`() {
        val variantQuery = "ins_S:501:E?*E"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(AminoAcidInsertionContains(501, "E.*\\*E", "S")))
    }

    @Test
    fun `given a valid variantQuery with a 'NextstrainCladeLineage' expression then returns SILO query`() {
        val variantQuery = "nextstrainClade:22B"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(StringEquals(NEXTSTRAIN_CLADE_COLUMN, "22B")))
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `given a valid variantQuery with a 'NextstrainCladeLineage' expression in lower case then returns SILO query`() {
        val variantQuery = "nextstrainClade:22b"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(StringEquals(NEXTSTRAIN_CLADE_COLUMN, "22B")))
    }

    @Test
    fun `given a valid variantQuery with a 'NextstrainCladeLineage' recombinant expression then returns SILO query`() {
        val variantQuery = "nextstrainClade:RECOMBINANT"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(StringEquals(NEXTSTRAIN_CLADE_COLUMN, "recombinant")))
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `given a valid variantQuery with a 'NextstrainCladeLineage' recombinant expression in lower case then returns SILO query`() {
        val variantQuery = "nextstrainClade:recombinant"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(StringEquals(NEXTSTRAIN_CLADE_COLUMN, "recombinant")))
    }

    @Test
    fun `given a valid variantQuery with a single letter 'GisaidCladeLineage' expression then returns SILO query`() {
        val variantQuery = "gisaid:X"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(StringEquals(GISAID_CLADE_COLUMN, "X")))
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `given a valid variantQuery with a single letter 'GisaidCladeLineage' expression with lower case then returns SILO query`() {
        val variantQuery = "gisaid:x"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(StringEquals(GISAID_CLADE_COLUMN, "X")))
    }

    @Test
    fun `given a valid variantQuery with a 'GisaidCladeLineage' expression then returns SILO query`() {
        val variantQuery = "gisaid:AB"

        val result = underTest.map(variantQuery)

        assertThat(result, equalTo(StringEquals(GISAID_CLADE_COLUMN, "AB")))
    }

    @Test
    fun `given a valid variantQuery with a 'nextcladePangoLineage' expression then returns SILO query`() {
        val variantQuery = "nextcladePangoLineage:jn.1*"

        val result = underTest.map(variantQuery)

        assertThat(
            result,
            equalTo(LineageEquals(NEXTCLADE_PANGO_LINEAGE_COLUMN, "jn.1", true)),
        )
    }

    @Test
    fun `GIVEN an invalid variant query THEN throw bad request exception`() {
        val variantQuery = "nextcladePangoLineage:jn.1* thisIsInvalid"

        val exception = assertThrows<BadRequestException> { underTest.map(variantQuery) }

        assertThat(
            exception.message,
            `is`("Failed to parse variant query (line 1:28): mismatched input 't' expecting {<EOF>, '&', '|'}."),
        )
    }
}
