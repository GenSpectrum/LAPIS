package org.genspectrum.lapis.request

import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JacksonComponent
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.node.JsonNodeFactory

data class PhyloTreeSequenceFiltersRequest(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aminoAcidMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
    override val orderByFields: OrderBySpec = OrderBySpec.EMPTY,
    override val limit: Int? = null,
    override val offset: Int? = null,
    val phyloTreeField: String,
) : CommonSequenceFilters

data class MRCASequenceFiltersRequest(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aminoAcidMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
    override val orderByFields: OrderBySpec = OrderBySpec.EMPTY,
    override val limit: Int? = null,
    override val offset: Int? = null,
    val phyloTreeField: String,
    val printNodesNotInTree: Boolean = false,
) : CommonSequenceFilters

@JacksonComponent
class PhyloTreeSequenceFiltersRequestDeserializer(
    private val fieldConverter: CaseInsensitiveFieldConverter,
    private val databaseConfig: DatabaseConfig,
) : ValueDeserializer<PhyloTreeSequenceFiltersRequest>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): PhyloTreeSequenceFiltersRequest {
        val node = jsonParser.readValueAsTree<JsonNode>()

        val phyloTreeField = parsePhyloTreeProperty(node, fieldConverter, databaseConfig)
        val parsedCommonFields = parseCommonFields(node, ctxt)

        return PhyloTreeSequenceFiltersRequest(
            parsedCommonFields.sequenceFilters,
            parsedCommonFields.nucleotideMutations,
            parsedCommonFields.aminoAcidMutations,
            parsedCommonFields.nucleotideInsertions,
            parsedCommonFields.aminoAcidInsertions,
            parsedCommonFields.orderByFields,
            parsedCommonFields.limit,
            parsedCommonFields.offset,
            phyloTreeField.fieldName,
        )
    }
}

@JacksonComponent
class MRCASequenceFiltersRequestDeserializer(
    private val fieldConverter: CaseInsensitiveFieldConverter,
    private val databaseConfig: DatabaseConfig,
) : ValueDeserializer<MRCASequenceFiltersRequest>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): MRCASequenceFiltersRequest {
        val node = jsonParser.readValueAsTree<JsonNode>()

        val phyloTreeField = parsePhyloTreeProperty(node, fieldConverter, databaseConfig)
        val printNodesNotInTree = parsePrintNodesNotInTree(node)
        val parsedCommonFields = parseCommonFields(node, ctxt)

        return MRCASequenceFiltersRequest(
            parsedCommonFields.sequenceFilters,
            parsedCommonFields.nucleotideMutations,
            parsedCommonFields.aminoAcidMutations,
            parsedCommonFields.nucleotideInsertions,
            parsedCommonFields.aminoAcidInsertions,
            parsedCommonFields.orderByFields,
            parsedCommonFields.limit,
            parsedCommonFields.offset,
            phyloTreeField.fieldName,
            printNodesNotInTree = printNodesNotInTree,
        )
    }
}

fun parsePhyloTreeProperty(
    node: JsonNode,
    fieldConverter: FieldConverter<RequestField>,
    databaseConfig: DatabaseConfig,
): Field {
    val phyloTreeField = node.get(PHYLO_TREE_FIELD_PROPERTY)
    if (phyloTreeField == null) {
        throw BadRequestException(
            "$PHYLO_TREE_FIELD_PROPERTY is required and must be a string representing a phylo tree field",
        )
    }
    if (!phyloTreeField.isString) {
        throw BadRequestException(
            "$PHYLO_TREE_FIELD_PROPERTY must be a string, but was ${phyloTreeField.nodeType}",
        )
    }
    return validatePhyloTreeField(phyloTreeField.stringValue(), fieldConverter, databaseConfig)
}

fun parsePrintNodesNotInTree(node: JsonNode): Boolean {
    val printNodesNotInTreeField =
        node.get(PRINT_NODES_NOT_IN_TREE_FIELD_PROPERTY) ?: JsonNodeFactory.instance.booleanNode(false)
    if (!printNodesNotInTreeField.isBoolean) {
        throw BadRequestException(
            "$PRINT_NODES_NOT_IN_TREE_FIELD_PROPERTY must be a boolean, but was ${printNodesNotInTreeField.nodeType}",
        )
    }
    return printNodesNotInTreeField.booleanValue()
}
