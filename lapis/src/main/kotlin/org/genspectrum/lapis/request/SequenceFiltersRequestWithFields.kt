package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JsonComponent

data class SequenceFiltersRequestWithFields(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aminoAcidMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
    val fields: List<Field>,
    override val orderByFields: List<OrderByField> = emptyList(),
    override val limit: Int? = null,
    override val offset: Int? = null,
) : CommonSequenceFilters

data class PhyloTreeSequenceFiltersRequestWithFields(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aminoAcidMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
    val phyloTreeField: String,
    override val orderByFields: List<OrderByField> = emptyList(),
    override val limit: Int? = null,
    override val offset: Int? = null,
    val printNodesNotInTree: Boolean = false,
) : CommonSequenceFilters

@JsonComponent
class SequenceFiltersRequestWithFieldsDeserializer(
    private val caseInsensitiveFieldConverter: CaseInsensitiveFieldConverter,
) : JsonDeserializer<SequenceFiltersRequestWithFields>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): SequenceFiltersRequestWithFields {
        val node = jsonParser.readValueAsTree<JsonNode>()
        val codec = jsonParser.codec

        val fields = parseFieldsProperty(node, caseInsensitiveFieldConverter)
        val parsedCommonFields = parseCommonFields(node, codec)

        return SequenceFiltersRequestWithFields(
            parsedCommonFields.sequenceFilters,
            parsedCommonFields.nucleotideMutations,
            parsedCommonFields.aminoAcidMutations,
            parsedCommonFields.nucleotideInsertions,
            parsedCommonFields.aminoAcidInsertions,
            fields,
            parsedCommonFields.orderByFields,
            parsedCommonFields.limit,
            parsedCommonFields.offset,
        )
    }
}

@JsonComponent
class PhyloTreeSequenceFiltersRequestWithFieldsDeserializer(
    private val caseInsensitiveFieldConverter: CaseInsensitiveFieldConverter,
) : JsonDeserializer<PhyloTreeSequenceFiltersRequestWithFields>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): PhyloTreeSequenceFiltersRequestWithFields {
        val node = jsonParser.readValueAsTree<JsonNode>()
        val codec = jsonParser.codec

        val phyloTreeField = node.get(PHYLO_TREE_FIELD_PROPERTY)?.asText()
            ?: throw BadRequestException("$PHYLO_TREE_FIELD_PROPERTY is required")
        val parsedCommonFields = parseCommonFields(node, codec)

        return PhyloTreeSequenceFiltersRequestWithFields(
            parsedCommonFields.sequenceFilters,
            parsedCommonFields.nucleotideMutations,
            parsedCommonFields.aminoAcidMutations,
            parsedCommonFields.nucleotideInsertions,
            parsedCommonFields.aminoAcidInsertions,
            phyloTreeField,
            parsedCommonFields.orderByFields,
            parsedCommonFields.limit,
            parsedCommonFields.offset,
            printNodesNotInTree = node.get(PRINT_NODES_NOT_IN_TREE_FIELD_PROPERTY)?.asBoolean() ?: false,
        )
    }
}

fun <T> parseFieldsProperty(
    node: JsonNode,
    fieldConverter: FieldConverter<T>,
) = when (val fields = node.get(FIELDS_PROPERTY)) {
    null -> emptyList()
    is ArrayNode -> fields.asSequence().map { fieldConverter.convert(it.asText()) }.toList()
    else -> throw BadRequestException(
        "$FIELDS_PROPERTY must be an array or null",
    )
}
