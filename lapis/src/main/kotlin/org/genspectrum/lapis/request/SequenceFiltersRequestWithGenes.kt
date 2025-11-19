package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JsonComponent

data class SequenceFiltersRequestWithGenes(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aminoAcidMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
    val genes: List<String>,
    override val orderByFields: OrderBySpec = OrderBySpec.ByFields(emptyList()),
    override val limit: Int? = null,
    override val offset: Int? = null,
    val fastaHeaderTemplate: String? = null,
) : CommonSequenceFilters

@JsonComponent
class SequenceFiltersRequestWithGenesDeserializer(
    private val referenceGenomeSchema: ReferenceGenomeSchema,
) : JsonDeserializer<SequenceFiltersRequestWithGenes>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): SequenceFiltersRequestWithGenes {
        val node = jsonParser.readValueAsTree<JsonNode>()
        val codec = jsonParser.codec

        val genes = parseGenes(node)
        val fastaHeaderTemplate = parseFastaHeaderTemplateParameter(node)
        val parsedCommonFields = parseCommonFields(node, codec)

        return SequenceFiltersRequestWithGenes(
            sequenceFilters = parsedCommonFields.sequenceFilters,
            nucleotideMutations = parsedCommonFields.nucleotideMutations,
            aminoAcidMutations = parsedCommonFields.aminoAcidMutations,
            nucleotideInsertions = parsedCommonFields.nucleotideInsertions,
            aminoAcidInsertions = parsedCommonFields.aminoAcidInsertions,
            genes = genes,
            orderByFields = parsedCommonFields.orderByFields,
            limit = parsedCommonFields.limit,
            offset = parsedCommonFields.offset,
            fastaHeaderTemplate = fastaHeaderTemplate,
        )
    }

    private fun parseGenes(node: JsonNode) =
        when (val genes = node.get(GENES_PROPERTY)) {
            null -> referenceGenomeSchema.getGeneNames()
            is ArrayNode -> {
                genes.map {
                    if (!it.isTextual) {
                        throw BadRequestException(
                            "$GENES_PROPERTY items must be strings, but was ${it.nodeType}: ${it.asText()}",
                        )
                    }
                    referenceGenomeSchema.getGene(it.textValue())
                        ?.name
                        ?: throw BadRequestException(
                            "Unknown gene: ${it.asText()}, " +
                                "available genes: ${referenceGenomeSchema.getGeneNames()}",
                        )
                }
            }

            else -> throw BadRequestException(
                "$GENES_PROPERTY must be an array or null",
            )
        }
}
