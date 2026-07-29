package org.genspectrum.lapis.request.converter

import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.config.ReferenceSequenceSchema
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.request.SequencePositionField
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SequencePositionFieldConverterTest {
    private val singleSegmentedConverter = SequencePositionFieldConverter(
        referenceGenomeSchema = ReferenceGenomeSchema(
            nucleotideSequences = listOf(ReferenceSequenceSchema("main")),
            genes = listOf(ReferenceSequenceSchema("ORF1a")),
        ),
    )

    private val multiSegmentedConverter = SequencePositionFieldConverter(
        referenceGenomeSchema = ReferenceGenomeSchema(
            nucleotideSequences = listOf(ReferenceSequenceSchema("S"), ReferenceSequenceSchema("L")),
            genes = emptyList(),
        ),
    )

    @Test
    fun `tryConvert resolves shorthand position syntax on a single-segmented genome`() {
        val result = singleSegmentedConverter.tryConvert("[501]")

        assertThat(result, equalTo(SequencePositionField("main", 501, isSingleSegment = true)))
    }

    @Test
    fun `tryConvert resolves a named position case-insensitively`() {
        val result = singleSegmentedConverter.tryConvert("MAIN[42]")

        assertThat(result, equalTo(SequencePositionField("main", 42)))
    }

    @Test
    fun `tryConvert resolves a gene position`() {
        val result = singleSegmentedConverter.tryConvert("orf1a[7]")

        assertThat(result, equalTo(SequencePositionField("ORF1a", 7)))
    }

    @Test
    fun `tryConvert returns null for strings that are not position syntax`() {
        assertThat(singleSegmentedConverter.tryConvert("country"), nullValue())
    }

    @Test
    fun `tryConvert returns null for non-numeric bracket contents`() {
        assertThat(singleSegmentedConverter.tryConvert("main[abc]"), nullValue())
    }

    @Test
    fun `tryConvert throws for a position that overflows an int`() {
        assertThrows<BadRequestException> { singleSegmentedConverter.tryConvert("main[99999999999999]") }
    }

    @Test
    fun `tryConvert throws for a zero position`() {
        assertThrows<BadRequestException> { singleSegmentedConverter.tryConvert("main[0]") }
    }

    @Test
    fun `tryConvert throws for an unknown sequence name`() {
        assertThrows<BadRequestException> { singleSegmentedConverter.tryConvert("unknown[1]") }
    }

    @Test
    fun `tryConvert throws when shorthand syntax is used on a multi-segmented genome`() {
        assertThrows<BadRequestException> { multiSegmentedConverter.tryConvert("[1]") }
    }

    @Test
    fun `tryConvert resolves a named position on a multi-segmented genome`() {
        val result = multiSegmentedConverter.tryConvert("S[1]")

        assertThat(result, equalTo(SequencePositionField("S", 1)))
    }
}
