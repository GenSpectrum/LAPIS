package org.genspectrum.lapis.openApi

import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.config.ReferenceSequenceSchema
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class AggregatedFieldsDescriptionTest {
    @Test
    fun `mentions the position shorthand for single-segmented genomes`() {
        val referenceGenomeSchema = ReferenceGenomeSchema(
            nucleotideSequences = listOf(ReferenceSequenceSchema("main")),
            genes = emptyList(),
        )

        val description = aggregatedFieldsDescription(referenceGenomeSchema)

        assertThat(description, containsString("shorthand `[position]`"))
    }

    @Test
    fun `does not mention the position shorthand for multi-segmented genomes`() {
        val referenceGenomeSchema = ReferenceGenomeSchema(
            nucleotideSequences = listOf(ReferenceSequenceSchema("segment1"), ReferenceSequenceSchema("segment2")),
            genes = emptyList(),
        )

        val description = aggregatedFieldsDescription(referenceGenomeSchema)

        assertThat(description, not(containsString("shorthand `[position]`")))
        assertThat(description, containsString("SequenceName[position]"))
    }
}
