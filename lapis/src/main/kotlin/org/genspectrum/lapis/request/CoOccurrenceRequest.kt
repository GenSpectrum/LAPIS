package org.genspectrum.lapis.request

import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JacksonComponent
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.node.ArrayNode

data class CoOccurrenceRequest(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aminoAcidMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
    val positions: List<CoOccurrencePosition>,
    override val orderByFields: OrderBySpec = OrderBySpec.EMPTY,
    override val limit: Int? = null,
    override val offset: Int? = null,
) : CommonSequenceFilters

@JacksonComponent
class CoOccurrenceRequestDeserializer : ValueDeserializer<CoOccurrenceRequest>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): CoOccurrenceRequest {
        val node = jsonParser.readValueAsTree<JsonNode>()

        val positions = parsePositionsProperty(node, ctxt)
        val parsedCommonFields = parseCommonFields(node, ctxt)

        return CoOccurrenceRequest(
            sequenceFilters = parsedCommonFields.sequenceFilters,
            nucleotideMutations = parsedCommonFields.nucleotideMutations,
            aminoAcidMutations = parsedCommonFields.aminoAcidMutations,
            nucleotideInsertions = parsedCommonFields.nucleotideInsertions,
            aminoAcidInsertions = parsedCommonFields.aminoAcidInsertions,
            positions = positions,
            orderByFields = parsedCommonFields.orderByFields,
            limit = parsedCommonFields.limit,
            offset = parsedCommonFields.offset,
        )
    }
}

private fun parsePositionsProperty(
    node: JsonNode,
    ctxt: DeserializationContext,
): List<CoOccurrencePosition> =
    when (val positionsNode = node.get(POSITIONS_PROPERTY)) {
        null -> throw BadRequestException("$POSITIONS_PROPERTY is required")
        is ArrayNode -> positionsNode.toList().map {
            ctxt.readTreeAsValue(it, CoOccurrencePosition::class.java)
        }
        else -> throw BadRequestException(
            "$POSITIONS_PROPERTY must be an array, ${butWas(positionsNode)}",
        )
    }
