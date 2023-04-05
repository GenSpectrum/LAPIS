package org.genspectrum.lapis.model

import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.Maybe
import org.genspectrum.lapis.silo.NOf
import org.genspectrum.lapis.silo.Not
import org.genspectrum.lapis.silo.NucleotideSymbolEquals
import org.genspectrum.lapis.silo.Or
import org.genspectrum.lapis.silo.PangoLineageEquals
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VariantQueryFacadeTest {
    private lateinit var underTest: VariantQueryFacade

    @BeforeEach
    fun setup() {
        underTest = VariantQueryFacade()
    }

    @Test
    fun `given a complex variant query then map should return the corresponding SiloQuery`() {
        val variantQuery = "300G & (400- | 500B) & !600 & MAYBE(700B | 800-) & [3-of: 123A, 234T, 345G] & A.1.2.3*"

        val result = underTest.map(variantQuery)

        val expectedResult =
            And(
                listOf(
                    And(
                        listOf(
                            And(
                                listOf(
                                    And(
                                        listOf(
                                            And(
                                                listOf(
                                                    NucleotideSymbolEquals(300, "G"),
                                                    Or(
                                                        listOf(
                                                            NucleotideSymbolEquals(400, "-"),
                                                            NucleotideSymbolEquals(500, "B"),
                                                        ),
                                                    ),
                                                ),
                                            ),
                                            Not(NucleotideSymbolEquals(600, "-")),
                                        ),
                                    ),
                                    Maybe(
                                        Or(
                                            listOf(
                                                NucleotideSymbolEquals(700, "B"),
                                                NucleotideSymbolEquals(800, "-"),
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                            NOf(
                                3,
                                matchExactly = false,
                                listOf(
                                    NucleotideSymbolEquals(123, "A"),
                                    NucleotideSymbolEquals(234, "T"),
                                    NucleotideSymbolEquals(345, "G"),
                                ),
                            ),
                        ),
                    ),
                    PangoLineageEquals("pangoLineage", "A.1.2.3", true),
                ),
            )

        MatcherAssert.assertThat(result, Matchers.equalTo(expectedResult))
    }

    @Test
    fun `given a variantQuery with a single entry then map should return the corresponding SiloQuery`() {
        val variantQuery = "300G"

        val result = underTest.map(variantQuery)

        val expectedResult = NucleotideSymbolEquals(300, "G")
        MatcherAssert.assertThat(result, Matchers.equalTo(expectedResult))
    }

    @Test
    fun `given a variantQuery with an 'And' expression then map should return the corresponding SiloQuery`() {
        val variantQuery = "300G & 400"

        val result = underTest.map(variantQuery)

        val expectedResult = And(
            listOf(
                NucleotideSymbolEquals(300, "G"),
                NucleotideSymbolEquals(400, "-"),
            ),
        )
        MatcherAssert.assertThat(result, Matchers.equalTo(expectedResult))
    }

    @Test
    fun `given a variantQuery with two 'And' expression then map should return the corresponding SiloQuery`() {
        val variantQuery = "300G & 400- & 500B"

        val result = underTest.map(variantQuery)

        val expectedResult = And(
            listOf(
                And(
                    listOf(
                        NucleotideSymbolEquals(300, "G"),
                        NucleotideSymbolEquals(400, "-"),
                    ),
                ),
                NucleotideSymbolEquals(500, "B"),
            ),
        )
        MatcherAssert.assertThat(result, Matchers.equalTo(expectedResult))
    }

    @Test
    fun `given a variantQuery with a 'Not' expression then map should return the corresponding SiloQuery`() {
        val variantQuery = "!300G"

        val result = underTest.map(variantQuery)

        val expectedResult = Not(NucleotideSymbolEquals(300, "G"))
        MatcherAssert.assertThat(result, Matchers.equalTo(expectedResult))
    }

    @Test
    fun `given a variant variantQuery with an 'Or' expression then map should return the corresponding SiloQuery`() {
        val variantQuery = "300G | 400"

        val result = underTest.map(variantQuery)

        val expectedResult = Or(
            listOf(
                NucleotideSymbolEquals(300, "G"),
                NucleotideSymbolEquals(400, "-"),
            ),
        )
        MatcherAssert.assertThat(result, Matchers.equalTo(expectedResult))
    }

    @Test
    fun `given a variant variantQuery with an bracket expression then map should return the corresponding SiloQuery`() {
        val variantQuery = "300C & (400A | 500G)"

        val result = underTest.map(variantQuery)

        val expectedResult = And(
            listOf(
                NucleotideSymbolEquals(300, "C"),
                Or(
                    listOf(
                        NucleotideSymbolEquals(400, "A"),
                        NucleotideSymbolEquals(500, "G"),
                    ),
                ),
            ),
        )
        MatcherAssert.assertThat(result, Matchers.equalTo(expectedResult))
    }

    @Test
    fun `given a variantQuery with a 'Maybe' expression then map should return the corresponding SiloQuery`() {
        val variantQuery = "MAYBE(300G)"

        val result = underTest.map(variantQuery)

        val expectedResult = Maybe(NucleotideSymbolEquals(300, "G"))
        MatcherAssert.assertThat(result, Matchers.equalTo(expectedResult))
    }

    @Test
    fun `given a variantQuery with a 'Pangolineage' expression then map should return the corresponding SiloQuery`() {
        val variantQuery = "A.1.2.3"

        val result = underTest.map(variantQuery)

        val expectedResult = PangoLineageEquals("pangoLineage", "A.1.2.3", false)
        MatcherAssert.assertThat(result, Matchers.equalTo(expectedResult))
    }

    @Test
    fun `given a variantQuery with a 'Pangolineage' expression (including sublineages) then map should return the corresponding SiloQuery`() { // ktlint-disable max-line-length
        val variantQuery = "A.1.2.3*"

        val result = underTest.map(variantQuery)

        val expectedResult = PangoLineageEquals("pangoLineage", "A.1.2.3", true)
        MatcherAssert.assertThat(result, Matchers.equalTo(expectedResult))
    }

    @Test
    fun `given a variantQuery with a 'Nof' expression then map should return the corresponding SiloQuery`() {
        val variantQuery = "[3-of: 123A, 234T, 345G]"

        val result = underTest.map(variantQuery)

        val expectedResult = NOf(
            3,
            false,
            listOf(
                NucleotideSymbolEquals(123, "A"),
                NucleotideSymbolEquals(234, "T"),
                NucleotideSymbolEquals(345, "G"),
            ),
        )
        MatcherAssert.assertThat(result, Matchers.equalTo(expectedResult))
    }

    @Test
    fun `given a variantQuery with a exact 'Nof' expression then map should return the corresponding SiloQuery`() {
        val variantQuery = "[exactly-3-of: 123A, 234T, 345G]"

        val result = underTest.map(variantQuery)

        val expectedResult = NOf(
            3,
            true,
            listOf(
                NucleotideSymbolEquals(123, "A"),
                NucleotideSymbolEquals(234, "T"),
                NucleotideSymbolEquals(345, "G"),
            ),
        )
        MatcherAssert.assertThat(result, Matchers.equalTo(expectedResult))
    }

    @Test
    fun `given a variantQuery with a 'Insertion' expression then map should throw an error`() {
        val variantQuery = "ins_1234:GAG"

        val exception = assertThrows<NotImplementedError> { underTest.map(variantQuery) }

        MatcherAssert.assertThat(
            exception.message,
            Matchers.equalTo("Nucleotide insertions are not supported yet."),
        )
    }

    @Test
    fun `given a variant variantQuery with a 'AA mutation' expression then map should throw an error`() {
        val variantQuery = "S:N501Y"

        val exception = assertThrows<NotImplementedError> { underTest.map(variantQuery) }

        MatcherAssert.assertThat(
            exception.message,
            Matchers.equalTo("Amino acid mutations are not supported yet."),
        )
    }

    @Test
    fun `given a valid variantQuery with a 'AA insertion' expression then map should throw an error`() {
        val variantQuery = "ins_S:501:EPE"

        val exception = assertThrows<NotImplementedError> { underTest.map(variantQuery) }

        MatcherAssert.assertThat(
            exception.message,
            Matchers.equalTo("Amino acid insertions are not supported yet."),
        )
    }

    @Test
    fun `given a valid variantQuery with a 'nextclade pango lineage' expression then map should throw an error`() {
        val variantQuery = "nextcladePangoLineage:BA.5*"

        val exception = assertThrows<NotImplementedError> { underTest.map(variantQuery) }

        MatcherAssert.assertThat(
            exception.message,
            Matchers.equalTo("Nextclade pango lineages are not supported yet."),
        )
    }

    @Test
    fun `given a valid variantQuery with a 'Nextstrain clade lineage' expression then map should throw an error`() {
        val variantQuery = "nextstrainClade:22B"

        val exception = assertThrows<NotImplementedError> { underTest.map(variantQuery) }

        MatcherAssert.assertThat(
            exception.message,
            Matchers.equalTo("Nextstrain clade lineages are not supported yet."),
        )
    }

    @Test
    fun `given a valid variantQuery with a 'Gisaid clade lineage' expression then map should throw an error`() {
        val variantQuery = "gisaid:AB"

        val exception = assertThrows<NotImplementedError> { underTest.map(variantQuery) }

        MatcherAssert.assertThat(
            exception.message,
            Matchers.equalTo("Gisaid clade lineages are not supported yet."),
        )
    }
}
