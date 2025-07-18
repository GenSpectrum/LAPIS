package org.genspectrum.lapis.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

const val REFERENCE_GENOME_SEGMENTS_APPLICATION_ARG_PREFIX = "referenceGenome.segments"
const val REFERENCE_GENOME_GENES_APPLICATION_ARG_PREFIX = "referenceGenome.genes"

const val REFERENCE_GENOME_ENV_VARIABLE_NAME = "LAPIS_REFERENCE_GENOME_FILENAME"
const val REFERENCE_GENOME_FILENAME_ARGS_NAME = "referenceGenomeFilename"

const val NO_REFERENCE_GENOME_FILENAME_ERROR_MESSAGE =
    """No reference genome filename specified.
    Please specify a reference genome filename using the --$REFERENCE_GENOME_FILENAME_ARGS_NAME argument 
    or the $REFERENCE_GENOME_ENV_VARIABLE_NAME environment variable."""

@JsonIgnoreProperties(ignoreUnknown = true)
class ReferenceGenomeSchema(
    val nucleotideSequences: List<ReferenceSequenceSchema>,
    val genes: List<ReferenceSequenceSchema>,
) {
    private val nucleotideSequenceNames: Map<LowercaseName, ReferenceSequenceSchema> = nucleotideSequences
        .associateBy { it.name.lowercase() }
    private val geneNames: Map<LowercaseName, ReferenceSequenceSchema> = genes
        .associateBy { it.name.lowercase() }

    fun getNucleotideSequence(name: String): ReferenceSequenceSchema? = nucleotideSequenceNames[name.lowercase()]

    fun getNucleotideSequenceNames() = nucleotideSequenceNames.values.map { it.name }

    fun getGene(name: String): ReferenceSequenceSchema? = geneNames[name.lowercase()]

    fun getGeneNames() = geneNames.values.map { it.name }

    fun isSingleSegmented(): Boolean = nucleotideSequences.size == 1

    fun getSequenceNameFromCaseInsensitiveName(name: String) =
        nucleotideSequenceNames[name.lowercase()]?.name
            ?: geneNames[name.lowercase()]?.name

    companion object {
        fun readFromFileFromProgramArgsOrEnv(args: Array<String>): ReferenceGenomeSchema {
            val filename = readFilenameFromProgramArgs(args)
                ?: System.getenv(REFERENCE_GENOME_ENV_VARIABLE_NAME)
                ?: throw IllegalArgumentException(NO_REFERENCE_GENOME_FILENAME_ERROR_MESSAGE)

            return readFromFile(filename)
        }

        fun readFromFile(filename: String): ReferenceGenomeSchema = jacksonObjectMapper().readValue(File(filename))

        private fun readFilenameFromProgramArgs(args: Array<String>): String? {
            val referenceGenomeArg = args.find { it.startsWith("--$REFERENCE_GENOME_FILENAME_ARGS_NAME=") }
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
data class ReferenceSequenceSchema(
    val name: String,
)

data class ReferenceGenome(
    val nucleotideSequences: List<ReferenceSequence>,
    val genes: List<ReferenceSequence>,
) {
    companion object {
        fun readFromFile(filename: String): ReferenceGenome = jacksonObjectMapper().readValue(File(filename))
    }

    fun getNucleotideSequence(sequenceName: String?): String {
        if (sequenceName == null) {
            return nucleotideSequences.first().sequence
        }
        return nucleotideSequences.first { it.name == sequenceName }.sequence
    }

    fun getGeneSequence(gene: String): String = genes.first { it.name == gene }.sequence
}

data class ReferenceSequence(
    val name: String,
    val sequence: String,
)
