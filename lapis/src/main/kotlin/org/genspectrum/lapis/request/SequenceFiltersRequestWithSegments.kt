package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JsonComponent

data class SequenceFiltersRequestWithSegments(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aminoAcidMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
    val segments: List<String>,
    override val orderByFields: List<OrderByField> = emptyList(),
    override val limit: Int? = null,
    override val offset: Int? = null,
) : CommonSequenceFilters

@JsonComponent
class SequenceFiltersRequestWithSegmentsDeserializer(
    private val referenceGenomeSchema: ReferenceGenomeSchema,
) : JsonDeserializer<SequenceFiltersRequestWithSegments>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): SequenceFiltersRequestWithSegments {
        val node = jsonParser.readValueAsTree<JsonNode>()
        val codec = jsonParser.codec

        val segments = parseSegments(node)
        val parsedCommonFields = parseCommonFields(node, codec)

        return SequenceFiltersRequestWithSegments(
            sequenceFilters = parsedCommonFields.sequenceFilters,
            nucleotideMutations = parsedCommonFields.nucleotideMutations,
            aminoAcidMutations = parsedCommonFields.aminoAcidMutations,
            nucleotideInsertions = parsedCommonFields.nucleotideInsertions,
            aminoAcidInsertions = parsedCommonFields.aminoAcidInsertions,
            segments = segments,
            orderByFields = parsedCommonFields.orderByFields,
            limit = parsedCommonFields.limit,
            offset = parsedCommonFields.offset,
        )
    }

    private fun parseSegments(node: JsonNode) =
        when (val segments = node.get(SEGMENTS_PROPERTY)) {
            null -> referenceGenomeSchema.getNucleotideSequenceNames()
            is ArrayNode -> {
                segments.map {
                    if (!it.isTextual) {
                        throw BadRequestException(
                            "$SEGMENTS_PROPERTY items must be strings, but was ${it.nodeType}: ${it.asText()}",
                        )
                    }
                    referenceGenomeSchema.getNucleotideSequence(it.textValue())
                        ?.name
                        ?: throw BadRequestException(
                            "Unknown segment: ${it.asText()}, " +
                                "available segments: ${referenceGenomeSchema.getNucleotideSequenceNames()}",
                        )
                }
            }

            else -> throw BadRequestException(
                "$SEGMENTS_PROPERTY must be an array or null",
            )
        }
}
