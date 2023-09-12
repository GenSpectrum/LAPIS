package org.genspectrum.lapis.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

const val REFERENCE_GENOME_APPLICATION_ARG_PREFIX = "referenceGenome.nucleotideSequences"

@JsonIgnoreProperties(ignoreUnknown = true)
class ReferenceGenome(
    @JsonProperty("nucleotide_sequences")
    val nucleotideSequences: List<NucleotideSequence>,
) {
    fun isSingleSegmented(): Boolean {
        return nucleotideSequences.size == 1
    }

    companion object {
        fun readFromFile(filename: String): ReferenceGenome {
            return jacksonObjectMapper().readValue(File(filename))
        }

        private fun readFilenameFromProgramArgs(args: Array<String>): String {
            val referenceGenomeArg = args.find { it.startsWith("--referenceGenomeFilename=") }
            return referenceGenomeArg?.substringAfter("=") ?: throw IllegalArgumentException(
                "No reference genome filename specified. Please specify a reference genome filename using the " +
                    "--referenceGenomeFilename argument.",
            )
        }

        fun readFromFileFromProgramArgs(args: Array<String>): ReferenceGenome {
            return readFromFile(readFilenameFromProgramArgs(args))
        }
    }

    fun toSpringApplicationArgs(): Array<String> {
        val nucleotideSequenceArgs =
            "--$REFERENCE_GENOME_APPLICATION_ARG_PREFIX=" + this.nucleotideSequences.joinToString(
                separator = ",",
            ) {
                it.name
            }

        return arrayOf(nucleotideSequenceArgs)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class NucleotideSequence(
    val name: String,
)
