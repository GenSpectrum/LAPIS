package org.genspectrum.lapis.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.io.File

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
