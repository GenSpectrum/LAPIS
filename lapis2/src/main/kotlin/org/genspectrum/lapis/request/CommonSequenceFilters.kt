package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import org.genspectrum.lapis.auth.ACCESS_KEY_PROPERTY

const val NUCLEOTIDE_MUTATIONS_PROPERTY = "nucleotideMutations"
const val AMINO_ACID_MUTATIONS_PROPERTY = "aminoAcidMutations"

interface CommonSequenceFilters {
    val sequenceFilters: Map<String, String>
    val nucleotideMutations: List<NucleotideMutation>
    val aaMutations: List<AminoAcidMutation>

    fun isEmpty() = sequenceFilters.isEmpty() && nucleotideMutations.isEmpty() && aaMutations.isEmpty()
}

fun parseCommonFields(
    node: JsonNode,
    codec: ObjectCodec,
): Triple<List<NucleotideMutation>, List<AminoAcidMutation>, Map<String, String>> {
    val nucleotideMutations = when (val nucleotideMutationsNode = node.get(NUCLEOTIDE_MUTATIONS_PROPERTY)) {
        null -> emptyList()
        is ArrayNode -> nucleotideMutationsNode.map { codec.treeToValue(it, NucleotideMutation::class.java) }
        else -> throw IllegalArgumentException(
            "nucleotideMutations must be an array or null",
        )
    }

    val aminoAcidMutations = when (val aminoAcidMutationsNode = node.get(AMINO_ACID_MUTATIONS_PROPERTY)) {
        null -> emptyList()
        is ArrayNode -> aminoAcidMutationsNode.map { codec.treeToValue(it, AminoAcidMutation::class.java) }
        else -> throw IllegalArgumentException(
            "aminoAcidMutations must be an array or null",
        )
    }

    val sequenceFilters = node.fields()
        .asSequence()
        .filter { isStringOrNumber(it.value) }
        .filter { it.key != ACCESS_KEY_PROPERTY }
        .associate { it.key to it.value.asText() }
    return Triple(nucleotideMutations, aminoAcidMutations, sequenceFilters)
}

private fun isStringOrNumber(jsonNode: JsonNode) =
    when (jsonNode.nodeType) {
        JsonNodeType.STRING,
        JsonNodeType.NUMBER,
        -> true

        else -> false
    }
