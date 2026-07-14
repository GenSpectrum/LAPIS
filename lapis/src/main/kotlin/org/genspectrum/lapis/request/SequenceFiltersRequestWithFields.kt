package org.genspectrum.lapis.request

import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JacksonComponent
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.node.ArrayNode

data class SequenceFiltersRequestWithFields(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aminoAcidMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
    val fields: List<RequestField>,
    override val orderByFields: OrderBySpec = OrderBySpec.EMPTY,
    override val limit: Int? = null,
    override val offset: Int? = null,
) : CommonSequenceFilters

@JacksonComponent
class SequenceFiltersRequestWithFieldsDeserializer(
    private val caseInsensitiveFieldConverter: CaseInsensitiveFieldConverter,
) : ValueDeserializer<SequenceFiltersRequestWithFields>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): SequenceFiltersRequestWithFields {
        val node = jsonParser.readValueAsTree<JsonNode>()

        val fields = parseFieldsProperty(node, caseInsensitiveFieldConverter)
        val parsedCommonFields = parseCommonFields(node, ctxt)

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

fun <T> parseFieldsProperty(
    node: JsonNode,
    fieldConverter: FieldConverter<T>,
) = when (val fields = node.get(FIELDS_PROPERTY)) {
    null -> emptyList()
    is ArrayNode -> fields.asSequence().map { fieldConverter.convert(it.asString()) }.toList()
    else -> throw BadRequestException(
        "$FIELDS_PROPERTY must be an array or null",
    )
}
