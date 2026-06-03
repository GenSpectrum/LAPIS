package org.genspectrum.lapis.request

import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JacksonComponent
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.node.ArrayNode

data class SequenceFiltersRequestWithGenes(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aminoAcidMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
    val genes: List<String>,
    override val orderByFields: OrderBySpec = OrderBySpec.EMPTY,
    override val limit: Int? = null,
    override val offset: Int? = null,
    val fastaHeaderTemplate: String? = null,
) : CommonSequenceFilters

@JacksonComponent
class SequenceFiltersRequestWithGenesDeserializer(
    private val referenceGenomeSchema: ReferenceGenomeSchema,
) : ValueDeserializer<SequenceFiltersRequestWithGenes>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): SequenceFiltersRequestWithGenes {
        val node = jsonParser.readValueAsTree<JsonNode>()

        val genes = parseGenes(node)
        val fastaHeaderTemplate = parseFastaHeaderTemplateParameter(node)
        val parsedCommonFields = parseCommonFields(node, ctxt)

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

    private fun parseGenes(node: JsonNode): List<String> =
        when (val genes = node.get(GENES_PROPERTY)) {
            null -> referenceGenomeSchema.getGeneNames()
            is ArrayNode -> {
                genes.toList().map {
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
