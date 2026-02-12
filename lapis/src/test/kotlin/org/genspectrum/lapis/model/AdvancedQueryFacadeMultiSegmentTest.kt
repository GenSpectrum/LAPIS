package org.genspectrum.lapis.model

import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.config.ReferenceSequenceSchema
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.dummyDatabaseConfig
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
import org.genspectrum.lapis.silo.PhyloDescendantOf
import org.genspectrum.lapis.silo.StringEquals
import org.genspectrum.lapis.silo.StringSearch
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AdvancedQueryFacadeMultiSegmentTest {
    private val dummyReferenceGenomeSchema = ReferenceGenomeSchema(
        listOf(
            ReferenceSequenceSchema("seg1"),
            ReferenceSequenceSchema("seg2"),
        ),
        listOf(
            ReferenceSequenceSchema("S"),
            ReferenceSequenceSchema("ORF1a"),
        ),
    )
    private val underTest = AdvancedQueryFacade(dummyReferenceGenomeSchema, dummyDatabaseConfig)

    @Test
    fun `given a complex advanced query THEN map should return the corresponding SiloQuery`() {
        val advancedQuery =
            "seg1:300G & (seg1:400- | seg1:500B) & !seg1:600 & MAYBE(seg1:700B | seg2:800-) " +
                "& [3-of: seg1:123A, seg2:234T, seg1:345G] & " +
                "pangoLineage=jn.1* & some_metadata.regex='^Democratic.*' & " +
                "treeKey.PhyloDescendantOf='internalNodeId'"

        val result = underTest.map(advancedQuery)

        val expectedResult = And(
            PhyloDescendantOf(
                "treeKey",
                "internalNodeId",
            ),
            StringSearch("some_metadata", "^Democratic.*"),
            LineageEquals(PANGO_LINEAGE_COLUMN, "jn.1", true),
            NOf(
                3,
                matchExactly = false,
                listOf(
                    NucleotideSymbolEquals("seg1", 123, "A"),
                    NucleotideSymbolEquals("seg2", 234, "T"),
                    NucleotideSymbolEquals("seg1", 345, "G"),
                ),
            ),
            Maybe(
                Or(
                    NucleotideSymbolEquals("seg2", 800, "-"),
                    NucleotideSymbolEquals("seg1", 700, "B"),
                ),
            ),
            Not(HasNucleotideMutation("seg1", 600)),
            Or(
                NucleotideSymbolEquals("seg1", 500, "B"),
                NucleotideSymbolEquals("seg1", 400, "-"),
            ),
            NucleotideSymbolEquals("seg1", 300, "G"),
        )

        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `GIVEN advanced query with PhyloDescendantOf in different casing THEN map same`() {
        val advancedQueryCorrectCasing = "treeKey.PhyloDescendantOf='internalNodeId'"
        val advancedQueryRandomCasing = "treeKey.pHylodescEndantoF='internalNodeId'"

        val resultCorrectCasing = underTest.map(advancedQueryCorrectCasing)
        val resultRandomCasing = underTest.map(advancedQueryRandomCasing)

        val expectedResult = PhyloDescendantOf("treeKey", "internalNodeId")
        assertThat(resultCorrectCasing, equalTo(expectedResult))
        assertThat(resultRandomCasing, equalTo(expectedResult))
    }

    @Test
    fun `given a complex advanced query with MAYBE THEN map should return the corresponding SiloQuery`() {
        val advancedQuery =
            "MAYBE((seg1:700B | seg1:800-) & !seg2:600 & [3-of: seg1:123A, seg2:234T, seg1:345G]) & " +
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
                            NucleotideSymbolEquals("seg1", 123, "A"),
                            NucleotideSymbolEquals("seg2", 234, "T"),
                            NucleotideSymbolEquals("seg1", 345, "G"),
                        ),
                    ),
                    Not(HasNucleotideMutation("seg2", 600)),
                    Or(
                        NucleotideSymbolEquals("seg1", 800, "-"),
                        NucleotideSymbolEquals("seg1", 700, "B"),
                    ),
                ),
            ),
        )

        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given a advancedQuery for a nucleotide mutation without a segment for a segmented virus THEN error`() {
        val aaMutation = "300T"

        val exceptionAa = assertThrows<BadRequestException> { underTest.map(aaMutation) }
        assertThat(
            exceptionAa.message,
            `is`("Reference genome is multi-segmented, you must specify segment as part of mutation query"),
        )
    }

    @Test
    fun `given a advancedQuery for an insertion without a segment for a segmented virus THEN error`() {
        val aaInsertion = "ins_300:T"

        val exceptionAa = assertThrows<BadRequestException> { underTest.map(aaInsertion) }
        assertThat(
            exceptionAa.message,
            `is`("Reference genome is multi-segmented, you must specify segment as part of mutation query"),
        )
    }

    @Test
    fun `given a advancedQuery with an invalid nucleotide mutation THEN throw error`() {
        val aaMutation = "seg1:300P"

        val exceptionAa = assertThrows<BadRequestException> { underTest.map(aaMutation) }
        assertThat(exceptionAa.message, `is`("Invalid nucleotide symbol: P"))
    }

    @Test
    fun `given a advancedQuery with an invalid amino acid mutation THEN throw error`() {
        val nucMutation = "S:300B"

        val exceptionNuc = assertThrows<BadRequestException> { underTest.map(nucMutation) }
        assertThat(exceptionNuc.message, `is`("Invalid amino acid symbol: B"))
    }

    @Test
    fun `given a advancedQuery with an invalid nucleotide insertion THEN throw error`() {
        val aaMutation = "ins_seg1:300:GP"

        val exceptionAa = assertThrows<BadRequestException> { underTest.map(aaMutation) }
        assertThat(exceptionAa.message, `is`("Invalid nucleotide symbol: P"))
    }

    @Test
    fun `given a advancedQuery with an invalid amino acid insertion THEN throw error`() {
        val nucMutation = "ins_S:300:GB"

        val exceptionNuc = assertThrows<BadRequestException> { underTest.map(nucMutation) }
        assertThat(exceptionNuc.message, `is`("Invalid amino acid symbol: B"))
    }

    @Test
    fun `given a advancedQuery with an invalid nucleotide segment THEN throw error`() {
        val nucMutation = "invalidGene:300T"

        val exceptionNuc = assertThrows<BadRequestException> { underTest.map(nucMutation) }
        assertThat(exceptionNuc.message, `is`("invalidGene is not a known segment or gene"))
    }

    @Test
    fun `given a advancedQuery with a single entry THEN map should return the corresponding SiloQuery`() {
        val advancedQuery = "seg1:300G"
        val advancedQueryWithFrom = "seg1:A300G"

        val result = underTest.map(advancedQuery)
        val resultWithFrom = underTest.map(advancedQueryWithFrom)

        val expectedResult = NucleotideSymbolEquals("seg1", 300, "G")
        assertThat(result, equalTo(expectedResult))
        assertThat(resultWithFrom, equalTo(expectedResult))
    }

    @Test
    fun `given a advancedQuery with mutation with position only THEN should return HasNucleotideMutation filter`() {
        val advancedQuery = "seg2:400"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(HasNucleotideMutation("seg2", 400)))
    }

    @Test
    fun `given a advancedQuery with an 'And' expression THEN map should return the corresponding SiloQuery`() {
        val advancedQuery = "seg1:300G & seg2:400-"

        val result = underTest.map(advancedQuery)

        val expectedResult = And(
            NucleotideSymbolEquals("seg2", 400, "-"),
            NucleotideSymbolEquals("seg1", 300, "G"),
        )
        assertThat(result, equalTo(expectedResult))

        val advancedQueryWords = "seg1:300G AND seg2:400-"

        val resultWords = underTest.map(advancedQueryWords)

        assertThat(resultWords, equalTo(result))
    }

    @Test
    fun `given a advancedQuery with two 'And' expression THEN map should return the corresponding SiloQuery`() {
        val advancedQuery = "seg1:300G & seg2:400- & seg1:500B"

        val result = underTest.map(advancedQuery)

        val expectedResult = And(
            NucleotideSymbolEquals("seg1", 500, "B"),
            NucleotideSymbolEquals("seg2", 400, "-"),
            NucleotideSymbolEquals("seg1", 300, "G"),
        )
        assertThat(result, equalTo(expectedResult))

        val advancedQueryWords = "seg1:300G & seg2:400- AND seg1:500B"

        val resultWords = underTest.map(advancedQueryWords)

        assertThat(resultWords, equalTo(result))
    }

    @Test
    fun `given a advancedQuery with a 'Not' expression THEN map should return the corresponding SiloQuery`() {
        val advancedQuery = "!seg1:300G"

        val result = underTest.map(advancedQuery)

        val expectedResult = Not(NucleotideSymbolEquals("seg1", 300, "G"))
        assertThat(result, equalTo(expectedResult))

        val advancedQueryWords = "NOT seg1:300G"

        val resultWords = underTest.map(advancedQueryWords)

        assertThat(resultWords, equalTo(result))
    }

    @Test
    fun `given a variant advancedQuery with an 'Or' expression THEN map should return the corresponding SiloQuery`() {
        val advancedQuery = "seg1:300G | seg2:400-"

        val result = underTest.map(advancedQuery)

        val expectedResult = Or(
            NucleotideSymbolEquals("seg2", 400, "-"),
            NucleotideSymbolEquals("seg1", 300, "G"),
        )
        assertThat(result, equalTo(expectedResult))

        val advancedQueryWords = "seg1:300G OR seg2:400-"

        val resultWords = underTest.map(advancedQueryWords)

        assertThat(resultWords, equalTo(result))
    }

    @Test
    fun `given a advancedQuery with an bracket expression THEN map should return the corresponding SiloQuery`() {
        val advancedQuery = "seg1:300C & (seg1:400A | seg2:500G)"

        val result = underTest.map(advancedQuery)

        val expectedResult = And(
            Or(
                NucleotideSymbolEquals("seg2", 500, "G"),
                NucleotideSymbolEquals("seg1", 400, "A"),
            ),
            NucleotideSymbolEquals("seg1", 300, "C"),
        )
        assertThat(result, equalTo(expectedResult))

        val advancedQueryWords = "seg1:300C AND (seg1:400A OR seg2:500G)"

        val resultWords = underTest.map(advancedQueryWords)

        assertThat(resultWords, equalTo(result))
    }

    @Test
    fun `given a advancedQuery with a 'Maybe' expression THEN map should return the corresponding SiloQuery`() {
        val advancedQuery = "MAYBE(seg1:300G)"

        val result = underTest.map(advancedQuery)

        val expectedResult = Maybe(NucleotideSymbolEquals("seg1", 300, "G"))
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `GIVEN a advancedQuery with a mixed-case 'Maybe' expression THEN map should return 'Maybe' SiloQuery`() {
        val advancedQuery = "maYbE(seg2:T12C)"

        val result = underTest.map(advancedQuery)

        val expectedResult = Maybe(NucleotideSymbolEquals("seg2", 12, "C"))
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given a advancedQuery with a 'Nof' expression THEN map should return the corresponding SiloQuery`() {
        val advancedQuery = "[3-of: seg1:123A, seg2:234T, seg1:345G, seg1:456A]"

        val result = underTest.map(advancedQuery)

        val expectedResult = NOf(
            3,
            false,
            listOf(
                NucleotideSymbolEquals("seg1", 123, "A"),
                NucleotideSymbolEquals("seg2", 234, "T"),
                NucleotideSymbolEquals("seg1", 345, "G"),
                NucleotideSymbolEquals("seg1", 456, "A"),
            ),
        )
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given a advancedQuery with a exact 'Nof' expression THEN map should return the corresponding SiloQuery`() {
        val advancedQuery = "[exactly-3-of: seg1:123A, seg2:234T, seg1:345G, seg1:456A]"

        val result = underTest.map(advancedQuery)

        val expectedResult = NOf(
            3,
            true,
            listOf(
                NucleotideSymbolEquals("seg1", 123, "A"),
                NucleotideSymbolEquals("seg2", 234, "T"),
                NucleotideSymbolEquals("seg1", 345, "G"),
                NucleotideSymbolEquals("seg1", 456, "A"),
            ),
        )
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `given a advancedQuery with a nested exact 'Nof' expression THEN map should return the corresponding SiloQuery`() {
        val advancedQuery = "[exactly-3-of: seg1:123A, !seg2:234G, seg1:345G, seg2:456A]"

        val result = underTest.map(advancedQuery)

        val expectedResult = NOf(
            3,
            true,
            listOf(
                NucleotideSymbolEquals("seg1", 123, "A"),
                Not(NucleotideSymbolEquals("seg2", 234, "G")),
                NucleotideSymbolEquals("seg1", 345, "G"),
                NucleotideSymbolEquals("seg2", 456, "A"),
            ),
        )
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `given a advancedQuery with a exact 'Nof' expression with casing THEN map should return the corresponding SiloQuery`() {
        val advancedQuery = "[exAcTly-3-oF: seg1:123A, seg1:234T, seg1:345G]"

        val result = underTest.map(advancedQuery)

        val expectedResult = NOf(
            3,
            true,
            listOf(
                NucleotideSymbolEquals("seg1", 123, "A"),
                NucleotideSymbolEquals("seg1", 234, "T"),
                NucleotideSymbolEquals("seg1", 345, "G"),
            ),
        )
        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given a advancedQuery with a 'Insertion' expression THEN returns SILO query`() {
        val advancedQuery = "ins_seg1:1234:GAG"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(NucleotideInsertionContains(1234, "GAG", "seg1")))
    }

    @Test
    fun `given a advancedQuery with a 'Insertion' expression with lower case letters THEN returns SILO query`() {
        val advancedQuery = "ins_seg2:1234:gAG"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(NucleotideInsertionContains(1234, "GAG", "seg2")))
    }

    @Test
    fun `given a advancedQuery with a 'Insertion' expression with casing letters THEN returns SILO query`() {
        val advancedQuery = "iNs_Seg1:1234:gAG"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(NucleotideInsertionContains(1234, "GAG", "seg1")))
    }

    @Test
    fun `given a advancedQuery with a 'Insertion' with wildcard expression THEN returns SILO query`() {
        val advancedQuery = "ins_seg1:1234:G?A?G"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(NucleotideInsertionContains(1234, "G.*A.*G", "seg1")))
    }

    @Test
    fun `given amino acid mutation expression THEN should map to AminoAcidSymbolEquals`() {
        val advancedQuery = "S:N501Y"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(AminoAcidSymbolEquals("S", 501, "Y")))
    }

    @Test
    fun `given amino acid mutation expression with lower case letters THEN should map to AminoAcidSymbolEquals`() {
        val advancedQuery = "S:n501y"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(AminoAcidSymbolEquals("S", 501, "Y")))
    }

    @Test
    fun `given amino acid mutation expression with gene lower case letters THEN should map to AminoAcidSymbolEquals`() {
        val advancedQuery = "orf1a:N501Y"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(AminoAcidSymbolEquals("ORF1a", 501, "Y")))
    }

    @Test
    fun `given amino acid mutation expression without first symbol THEN should map to AminoAcidSymbolEquals`() {
        val advancedQuery = "S:501Y"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(AminoAcidSymbolEquals("S", 501, "Y")))
    }

    @Test
    fun `given amino acid mutation expression without second symbol THEN should return HasAminoAcidMutation`() {
        val advancedQuery = "S:N501"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(HasAminoAcidMutation("S", 501)))
    }

    @Test
    fun `given amino acid mutation expression without any symbol THEN should return HasAminoAcidMutation`() {
        val advancedQuery = "S:501"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(HasAminoAcidMutation("S", 501)))
    }

    @Test
    fun `given a valid advancedQuery with a 'AA insertion' expression THEN returns SILO query`() {
        val advancedQuery = "ins_S:501:EPE"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(AminoAcidInsertionContains(501, "EPE", "S")))
    }

    @Test
    fun `given a valid advancedQuery with a stop codon expression THEN returns SILO query`() {
        val advancedQuery = "ins_S:501:A*C"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(AminoAcidInsertionContains(501, "A\\*C", "S")))
    }

    @Test
    fun `given a valid advancedQuery with a 'AA insertion' expression with lower case THEN returns SILO query`() {
        val advancedQuery = "ins_ORF1a:501:ePe"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(AminoAcidInsertionContains(501, "EPE", "ORF1a")))
    }

    @Test
    fun `given an invalid advancedQuery with an invalid gene and invalid segment return error`() {
        val advancedQuery = "ins_invalidGene:501:EPE"

        val exception = assertThrows<BadRequestException> { underTest.map(advancedQuery) }

        assertThat(exception.message, `is`("invalidGene is not a known segment or gene"))
    }

    @Test
    fun `given a valid advancedQuery with a 'AA insertion' expression with lower case gene THEN returns SILO query`() {
        val advancedQuery = "ins_orF1a:501:EPE"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(AminoAcidInsertionContains(501, "EPE", "ORF1a")))
    }

    @Test
    fun `given a valid advancedQuery with a 'AA insertion' with wildcard THEN returns SILO query`() {
        val advancedQuery = "ins_S:501:E?E?"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(AminoAcidInsertionContains(501, "E.*E.*", "S")))
    }

    @Test
    fun `given a valid advancedQuery with a 'AA insertion' with stop codon and wildcard THEN returns SILO query`() {
        val advancedQuery = "ins_S:501:E?*E"

        val result = underTest.map(advancedQuery)

        assertThat(result, equalTo(AminoAcidInsertionContains(501, "E.*\\*E", "S")))
    }

    @Test
    fun `given a valid advancedQuery with mutation and metadata expression THEN returns SILO query`() {
        val advancedQuery = "(NOT some_metadata=AB) & seg1:300G"

        val result = underTest.map(advancedQuery)

        val expectedResult = And(
            NucleotideSymbolEquals("seg1", 300, "G"),
            Not(StringEquals("some_metadata", "AB")),
        )

        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `given a valid advancedQuery with mutation and regex metadata expression THEN returns SILO query`() {
        val advancedQuery = "(some_metadata=BANGALOR AND seg1:300G)&(some_metadata.regex='BANGALOR' OR NOT S:501Y)"

        val result = underTest.map(advancedQuery)

        val expectedResult = And(
            Or(
                Not(AminoAcidSymbolEquals("S", 501, "Y")),
                StringSearch("some_metadata", "BANGALOR"),
            ),
            NucleotideSymbolEquals("seg1", 300, "G"),
            StringEquals("some_metadata", "BANGALOR"),
        )

        assertThat(result, equalTo(expectedResult))
    }

    @Test
    fun `ambiguous nucleotide from symbol in multi-segmented genome`() {
        val result = underTest.map("seg1:N123A")

        assertThat(
            result,
            equalTo(NucleotideSymbolEquals(sequenceName = "seg1", position = 123, symbol = "A")),
        )
    }

    @Test
    fun `ambiguous amino acid from symbol in multi-segmented genome`() {
        val result = underTest.map("S:X501Y")

        assertThat(
            result,
            equalTo(AminoAcidSymbolEquals(sequenceName = "S", position = 501, symbol = "Y")),
        )
    }
}
