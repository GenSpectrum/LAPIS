package org.genspectrum.lapis.config

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

private const val REFERENCE_GENOME_DEFAULT_FILENAME = "src/test/resources/config/reference-genomes.json"

class ReferenceGenomeSchemaTest {
    @Test
    fun `should detect single segmented sequence`() {
        val singleSegmented = ReferenceGenomeSchema(
            listOf(ReferenceSequenceSchema("main")),
            listOf(ReferenceSequenceSchema("gene1"), ReferenceSequenceSchema("gene2")),
        )
        assertThat(singleSegmented.isSingleSegmented(), equalTo(true))

        val multiSegmented = ReferenceGenomeSchema(
            listOf(ReferenceSequenceSchema("main"), ReferenceSequenceSchema("other_segment")),
            listOf(ReferenceSequenceSchema("gene1"), ReferenceSequenceSchema("gene2")),
        )
        assertThat(multiSegmented.isSingleSegmented(), equalTo(false))
    }

    @Test
    fun `getNucleotideSequences should be case insensitive`() {
        val referenceGenomeSchema = ReferenceGenomeSchema(
            listOf(ReferenceSequenceSchema("Main"), ReferenceSequenceSchema("OtherSegment")),
            emptyList(),
        )

        assertThat(referenceGenomeSchema.getNucleotideSequence("main")?.name, equalTo("Main"))
        assertThat(referenceGenomeSchema.getNucleotideSequence("MAIN")?.name, equalTo("Main"))
        assertThat(referenceGenomeSchema.getNucleotideSequence("mAiN")?.name, equalTo("Main"))
    }

    @Test
    fun `getGene should be case insensitive`() {
        val referenceGenomeSchema = ReferenceGenomeSchema(
            emptyList(),
            listOf(ReferenceSequenceSchema("Gene1"), ReferenceSequenceSchema("Gene2")),
        )

        assertThat(referenceGenomeSchema.getGene("gene1")?.name, equalTo("Gene1"))
        assertThat(referenceGenomeSchema.getGene("GENE1")?.name, equalTo("Gene1"))
        assertThat(referenceGenomeSchema.getGene("gEnE1")?.name, equalTo("Gene1"))
    }
}

class ReferenceGenomeTest {
    @Test
    fun `should read from file`() {
        val referenceGenome = ReferenceGenome.readFromFile(REFERENCE_GENOME_DEFAULT_FILENAME)
        assertThat(referenceGenome.nucleotideSequences.size, equalTo(1))
        assertThat(referenceGenome.nucleotideSequences[0].name, equalTo("main"))
        assertThat(referenceGenome.nucleotideSequences[0].sequence, equalTo("ATTA"))
        assertThat(referenceGenome.genes.size, equalTo(12))
        assertThat(referenceGenome.genes[0].name, equalTo("E"))
        assertThat(referenceGenome.genes[0].sequence, equalTo("MYSFVSEET*"))
    }
}
