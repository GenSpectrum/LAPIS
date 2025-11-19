package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.util.MultiValueMap

typealias SequenceFilters = Map<String, List<String?>>
typealias GetRequestSequenceFilters = MultiValueMap<String, String>

interface BaseSequenceFilters {
    val sequenceFilters: SequenceFilters
    val nucleotideMutations: List<NucleotideMutation>
    val aminoAcidMutations: List<AminoAcidMutation>
    val nucleotideInsertions: List<NucleotideInsertion>
    val aminoAcidInsertions: List<AminoAcidInsertion>

    fun isEmpty() =
        sequenceFilters.isEmpty() &&
            nucleotideMutations.isEmpty() &&
            aminoAcidMutations.isEmpty() &&
            nucleotideInsertions.isEmpty() &&
            aminoAcidInsertions.isEmpty()
}

interface CommonSequenceFilters : BaseSequenceFilters {
    val orderByFields: OrderBySpec
    val limit: Int?
    val offset: Int?
}

fun parseCommonFields(
    node: JsonNode,
    codec: ObjectCodec,
): ParsedCommonFields {
    val parsedMutationsAndInsertions = parseMutationsAndInsertions(node, codec)

    val orderByFields =
        when (val orderByNode = node.get(ORDER_BY_PROPERTY)) {
            null -> OrderBySpec.ByFields(emptyList())
            else -> codec.treeToValue(orderByNode, OrderBySpec::class.java)
        }

    val limitNode = node.get(LIMIT_PROPERTY)
    val limit = when (limitNode?.nodeType) {
        null -> null
        JsonNodeType.NULL, JsonNodeType.NUMBER -> limitNode.asInt()
        else -> throw BadRequestException("limit must be a number or null, ${butWas(limitNode)}")
    }

    val offsetNode = node.get(OFFSET_PROPERTY)
    val offset = when (offsetNode?.nodeType) {
        null -> null
        JsonNodeType.NULL, JsonNodeType.NUMBER -> offsetNode.asInt()
        else -> throw BadRequestException("offset must be a number or null, ${butWas(offsetNode)}")
    }

    val sequenceFilters = parseSequenceFilters(node, codec)

    return ParsedCommonFields(
        nucleotideMutations = parsedMutationsAndInsertions.nucleotideMutations,
        aminoAcidMutations = parsedMutationsAndInsertions.aminoAcidMutations,
        nucleotideInsertions = parsedMutationsAndInsertions.nucleotideInsertions,
        aminoAcidInsertions = parsedMutationsAndInsertions.aminoAcidInsertions,
        sequenceFilters = sequenceFilters,
        orderByFields = orderByFields,
        limit = limit,
        offset = offset,
    )
}

fun parseMutationsAndInsertions(
    node: JsonNode,
    codec: ObjectCodec,
): ParsedMutationsAndInsertions {
    val nucleotideMutations = when (val nucleotideMutationsNode = node.get(NUCLEOTIDE_MUTATIONS_PROPERTY)) {
        null -> emptyList()
        is ArrayNode -> nucleotideMutationsNode.map { codec.treeToValue(it, NucleotideMutation::class.java) }
        else -> throw BadRequestException(
            "nucleotideMutations must be an array or null, ${butWas(nucleotideMutationsNode)}",
        )
    }

    val aminoAcidMutations = when (val aminoAcidMutationsNode = node.get(AMINO_ACID_MUTATIONS_PROPERTY)) {
        null -> emptyList()
        is ArrayNode -> aminoAcidMutationsNode.map { codec.treeToValue(it, AminoAcidMutation::class.java) }
        else -> throw BadRequestException(
            "aminoAcidMutations must be an array or null, ${butWas(aminoAcidMutationsNode)}",
        )
    }

    val nucleotideInsertions = when (val nucleotideInsertionsNode = node.get(NUCLEOTIDE_INSERTIONS_PROPERTY)) {
        null -> emptyList()
        is ArrayNode -> nucleotideInsertionsNode.map { codec.treeToValue(it, NucleotideInsertion::class.java) }
        else -> throw BadRequestException(
            "nucleotideInsertions must be an array or null, ${butWas(nucleotideInsertionsNode)}",
        )
    }

    val aminoAcidInsertions = when (val aminoAcidInsertionsNode = node.get(AMINO_ACID_INSERTIONS_PROPERTY)) {
        null -> emptyList()
        is ArrayNode -> aminoAcidInsertionsNode.map { codec.treeToValue(it, AminoAcidInsertion::class.java) }
        else -> throw BadRequestException(
            "aminoAcidInsertions must be an array or null, ${butWas(aminoAcidInsertionsNode)}",
        )
    }

    return ParsedMutationsAndInsertions(
        nucleotideMutations,
        aminoAcidMutations,
        nucleotideInsertions,
        aminoAcidInsertions,
    )
}

fun parseSequenceFilters(
    node: JsonNode,
    codec: ObjectCodec,
    fieldsToExclude: Set<String> = SPECIAL_REQUEST_PROPERTIES,
): SequenceFilters =
    node.properties()
        .asSequence()
        .filter { !fieldsToExclude.contains(it.key) }
        .associate { it.key to getValuesList(it.value, it.key) }

fun butWas(jsonNode: JsonNode) = "but was $jsonNode (${jsonNode.nodeType})"

data class ParsedCommonFields(
    val nucleotideMutations: List<NucleotideMutation>,
    val aminoAcidMutations: List<AminoAcidMutation>,
    val nucleotideInsertions: List<NucleotideInsertion>,
    val aminoAcidInsertions: List<AminoAcidInsertion>,
    val sequenceFilters: SequenceFilters,
    val orderByFields: OrderBySpec,
    val limit: Int?,
    val offset: Int?,
)

data class ParsedMutationsAndInsertions(
    val nucleotideMutations: List<NucleotideMutation>,
    val aminoAcidMutations: List<AminoAcidMutation>,
    val nucleotideInsertions: List<NucleotideInsertion>,
    val aminoAcidInsertions: List<AminoAcidInsertion>,
)

fun getValuesList(
    value: JsonNode,
    key: String,
) = when {
    value.isValueNode -> listOf(getValueNode(value))

    value.nodeType == JsonNodeType.ARRAY -> value.map {
        when {
            it.isValueNode -> getValueNode(it)
            else -> throw BadRequestException(
                "Found unexpected array value $it of type ${it.nodeType} for $key, expected a primitive",
            )
        }
    }

    else -> throw BadRequestException(
        "Found unexpected value $value of type ${value.nodeType} for $key, expected primitive or array",
    )
}

fun getValueNode(value: JsonNode): String? {
    if (value.isNull) {
        return null
    }
    return value.asText()
}

fun parseFastaHeaderTemplateParameter(node: JsonNode) =
    when (val fastaHeaderTemplate = node.get(FASTA_HEADER_TEMPLATE_PROPERTY)) {
        is TextNode -> fastaHeaderTemplate.asText()
        is NullNode -> null
        null -> null
        else -> throw BadRequestException(
            "Fasta header template parameter must be a string or null, but was $node (${node.nodeType})",
        )
    }
