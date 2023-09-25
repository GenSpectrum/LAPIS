package org.genspectrum.lapis.config

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException

private const val REFERENCE_GENOME_DEFAULT_FILENAME = "src/test/resources/config/reference-genomes.json"

class ReferenceGenomeTest {
    @Test
    fun `should read from file`() {
        val referenceGenome = ReferenceGenome.readFromFile(REFERENCE_GENOME_DEFAULT_FILENAME)
        assertThat(referenceGenome.nucleotideSequences.size, equalTo(1))
        assertThat(referenceGenome.nucleotideSequences[0].name, equalTo("main"))
    }

    @Test
    fun `should read from file through program args`() {
        val args = arrayOf("--referenceGenomeFilename=$REFERENCE_GENOME_DEFAULT_FILENAME")
        val referenceGenome = ReferenceGenome.readFromFileFromProgramArgs(args)
        assertThat(referenceGenome.nucleotideSequences.size, equalTo(1))
        assertThat(referenceGenome.nucleotideSequences[0].name, equalTo("main"))
    }

    @Test
    fun `should throw if no reference genome filename is given in the args`() {
        val args = emptyArray<String>()
        assertThrows<IllegalArgumentException> { ReferenceGenome.readFromFileFromProgramArgs(args) }
    }

    @Test
    fun `should generate spring application arguments`() {
        val referenceGenome = ReferenceGenome(
            listOf(NucleotideSequence("main"), NucleotideSequence("other_segment")),
        )
        val springArgs = referenceGenome.toSpringApplicationArgs()
        assertThat(springArgs[0], equalTo("--$REFERENCE_GENOME_APPLICATION_ARG_PREFIX=main,other_segment"))
    }

    @Test
    fun `should detect single segmented sequence`() {
        val singleSegmented = ReferenceGenome(
            listOf(NucleotideSequence("main")),
        )
        assertThat(singleSegmented.isSingleSegmented(), equalTo(true))

        val multiSegmented = ReferenceGenome(
            listOf(NucleotideSequence("main"), NucleotideSequence("other_segment")),
        )
        assertThat(multiSegmented.isSingleSegmented(), equalTo(false))
    }
}
