package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JsonComponent

data class PhyloTreeSequenceFiltersRequest(
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
class PhyloTreeSequenceFiltersRequestDeserializer(
    private val caseInsensitiveFieldConverter: CaseInsensitiveFieldConverter,
) : JsonDeserializer<PhyloTreeSequenceFiltersRequest>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): PhyloTreeSequenceFiltersRequest {
        val node = jsonParser.readValueAsTree<JsonNode>()
        val codec = jsonParser.codec

        val phyloTreeField = node.get(PHYLO_TREE_FIELD_PROPERTY)?.asText()
            ?: throw BadRequestException("$PHYLO_TREE_FIELD_PROPERTY is required")
        val parsedCommonFields = parseCommonFields(node, codec)

        return PhyloTreeSequenceFiltersRequest(
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
