package org.genspectrum.lapis.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

const val REFERENCE_GENOME_APPLICATION_ARG_PREFIX = "referenceGenome.nucleotideSequences"

private const val ENV_VARIABLE_NAME = "LAPIS_REFERENCE_GENOME_FILENAME"
private const val ARGS_NAME = "referenceGenomeFilename"

@JsonIgnoreProperties(ignoreUnknown = true)
class ReferenceGenome(val nucleotideSequences: List<NucleotideSequence>) {

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
