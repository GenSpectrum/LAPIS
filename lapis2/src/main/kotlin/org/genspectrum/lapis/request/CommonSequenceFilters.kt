package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import org.genspectrum.lapis.controller.AMINO_ACID_INSERTIONS_PROPERTY
import org.genspectrum.lapis.controller.AMINO_ACID_MUTATIONS_PROPERTY
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.controller.LIMIT_PROPERTY
import org.genspectrum.lapis.controller.NUCLEOTIDE_INSERTIONS_PROPERTY
import org.genspectrum.lapis.controller.NUCLEOTIDE_MUTATIONS_PROPERTY
import org.genspectrum.lapis.controller.OFFSET_PROPERTY
import org.genspectrum.lapis.controller.ORDER_BY_PROPERTY
import org.genspectrum.lapis.controller.SPECIAL_REQUEST_PROPERTIES

interface CommonSequenceFilters {
    val sequenceFilters: Map<String, String>
    val nucleotideMutations: List<NucleotideMutation>
    val aaMutations: List<AminoAcidMutation>
    val nucleotideInsertions: List<NucleotideInsertion>
    val aminoAcidInsertions: List<AminoAcidInsertion>
    val orderByFields: List<OrderByField>
    val limit: Int?
    val offset: Int?

    fun isEmpty() =
        sequenceFilters.isEmpty() && nucleotideMutations.isEmpty() &&
            aaMutations.isEmpty() && nucleotideInsertions.isEmpty() && aminoAcidInsertions.isEmpty()
}

fun parseCommonFields(node: JsonNode, codec: ObjectCodec): ParsedCommonFields {
    val nucleotideMutations = when (val nucleotideMutationsNode = node.get(NUCLEOTIDE_MUTATIONS_PROPERTY)) {
        null -> emptyList()
        is ArrayNode -> nucleotideMutationsNode.map { codec.treeToValue(it, NucleotideMutation::class.java) }
        else -> throw BadRequestException(
            "nucleotideMutations must be an array or null",
        )
    }

    val aminoAcidMutations = when (val aminoAcidMutationsNode = node.get(AMINO_ACID_MUTATIONS_PROPERTY)) {
        null -> emptyList()
        is ArrayNode -> aminoAcidMutationsNode.map { codec.treeToValue(it, AminoAcidMutation::class.java) }
        else -> throw BadRequestException(
            "aminoAcidMutations must be an array or null",
        )
    }

    val nucleotideInsertions = when (val nucleotideInsertionsNode = node.get(NUCLEOTIDE_INSERTIONS_PROPERTY)) {
        null -> emptyList()
        is ArrayNode -> nucleotideInsertionsNode.map { codec.treeToValue(it, NucleotideInsertion::class.java) }
        else -> throw BadRequestException(
            "nucleotideInsertions must be an array or null",
        )
    }

    val aminoAcidInsertions = when (val aminoAcidInsertionsNode = node.get(AMINO_ACID_INSERTIONS_PROPERTY)) {
        null -> emptyList()
        is ArrayNode -> aminoAcidInsertionsNode.map { codec.treeToValue(it, AminoAcidInsertion::class.java) }
        else -> throw BadRequestException(
            "aminoAcidInsertions must be an array or null",
        )
    }

    val orderByFields = when (val orderByNode = node.get(ORDER_BY_PROPERTY)) {
        null -> emptyList()
        is ArrayNode -> orderByNode.map { codec.treeToValue(it, OrderByField::class.java) }
        else -> throw BadRequestException(
            "orderBy must be an array or null",
        )
    }

    val limitNode = node.get(LIMIT_PROPERTY)
    val limit = when (limitNode?.nodeType) {
        null -> null
        JsonNodeType.NULL, JsonNodeType.NUMBER -> limitNode.asInt()
        else -> throw BadRequestException("limit must be a number or null")
    }

    val offsetNode = node.get(OFFSET_PROPERTY)
    val offset = when (offsetNode?.nodeType) {
        null -> null
        JsonNodeType.NULL, JsonNodeType.NUMBER -> offsetNode.asInt()
        else -> throw BadRequestException("offset must be a number or null")
    }

    val sequenceFilters = node.fields().asSequence().filter { isStringOrNumber(it.value) }
        .filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) }.associate { it.key to it.value.asText() }
    return ParsedCommonFields(
        nucleotideMutations,
        aminoAcidMutations,
        nucleotideInsertions,
        aminoAcidInsertions,
        sequenceFilters,
        orderByFields,
        limit,
        offset,
    )
}

data class ParsedCommonFields(
    val nucleotideMutations: List<NucleotideMutation>,
    val aminoAcidMutations: List<AminoAcidMutation>,
    val nucleotideInsertions: List<NucleotideInsertion>,
    val aminoAcidInsertions: List<AminoAcidInsertion>,
    val sequenceFilters: Map<String, String>,
    val orderByFields: List<OrderByField>,
    val limit: Int?,
    val offset: Int?,
)

private fun isStringOrNumber(jsonNode: JsonNode) = when (jsonNode.nodeType) {
    JsonNodeType.STRING,
    JsonNodeType.NUMBER,
    -> true

    else -> false
}
