package org.genspectrum.lapis.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

const val REFERENCE_GENOME_SEGMENTS_APPLICATION_ARG_PREFIX = "referenceGenome.segments"
const val REFERENCE_GENOME_GENES_APPLICATION_ARG_PREFIX = "referenceGenome.genes"

private const val ENV_VARIABLE_NAME = "LAPIS_REFERENCE_GENOME_FILENAME"
private const val ARGS_NAME = "referenceGenomeFilename"

@JsonIgnoreProperties(ignoreUnknown = true)
class ReferenceGenome(val nucleotideSequences: List<ReferenceSequence>, val genes: List<ReferenceSequence>) {
    private val geneNames: Map<LowercaseName, ReferenceSequence> = genes
        .associateBy { it.name.lowercase() }

    fun getGeneFromLowercaseName(lowercaseName: LowercaseName): ReferenceSequence {
        return geneNames[lowercaseName]
            ?: throw RuntimeException("Unknown gene: $lowercaseName")
    }

    fun isSingleSegmented(): Boolean {
        return nucleotideSequences.size == 1
    }

    companion object {
        fun readFromFileFromProgramArgsOrEnv(args: Array<String>): ReferenceGenome {
            val filename = readFilenameFromProgramArgs(args)
                ?: System.getenv(ENV_VARIABLE_NAME)
                ?: throw IllegalArgumentException(
                    "No reference genome filename specified. Please specify a reference genome filename using the " +
                        "--$ARGS_NAME argument or the $ENV_VARIABLE_NAME environment variable.",
                )

            return readFromFile(filename)
        }

        fun readFromFile(filename: String): ReferenceGenome {
            return jacksonObjectMapper().readValue(File(filename))
        }

        private fun readFilenameFromProgramArgs(args: Array<String>): String? {
            val referenceGenomeArg = args.find { it.startsWith("--$ARGS_NAME=") }
            return referenceGenomeArg?.substringAfter("=")
        }
    }

    fun toSpringApplicationArgs(): Array<String> {
        val nucleotideSequenceArg = "--$REFERENCE_GENOME_SEGMENTS_APPLICATION_ARG_PREFIX=" +
            this.nucleotideSequences.joinToString(separator = ",") { it.name }
        val genesArg = "--$REFERENCE_GENOME_GENES_APPLICATION_ARG_PREFIX=" +
            this.genes.joinToString(separator = ",") { it.name }

        return arrayOf(nucleotideSequenceArg, genesArg)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ReferenceSequence(
    val name: String,
)
