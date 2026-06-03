package org.genspectrum.lapis.request

import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JacksonComponent
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.node.ArrayNode

data class SequenceFiltersRequestWithSegments(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aminoAcidMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
    val segments: List<String>,
    override val orderByFields: OrderBySpec = OrderBySpec.EMPTY,
    override val limit: Int? = null,
    override val offset: Int? = null,
    val fastaHeaderTemplate: String? = null,
) : CommonSequenceFilters

@JacksonComponent
class SequenceFiltersRequestWithSegmentsDeserializer(
    private val referenceGenomeSchema: ReferenceGenomeSchema,
) : ValueDeserializer<SequenceFiltersRequestWithSegments>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): SequenceFiltersRequestWithSegments {
        val node = jsonParser.readValueAsTree<JsonNode>()

        val segments = parseSegments(node)
        val fastaHeaderTemplate = parseFastaHeaderTemplateParameter(node)
        val parsedCommonFields = parseCommonFields(node, ctxt)

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
            fastaHeaderTemplate = fastaHeaderTemplate,
        )
    }

    private fun parseSegments(node: JsonNode): List<String> =
        when (val segments = node.get(SEGMENTS_PROPERTY)) {
            null -> referenceGenomeSchema.getNucleotideSequenceNames()
            is ArrayNode -> {
                segments.toList().map {
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
