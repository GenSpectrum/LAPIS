package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JsonComponent

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

@JsonComponent
class PhyloTreeSequenceFiltersRequestDeserializer(
    private val fieldConverter: CaseInsensitiveFieldConverter,
    private val databaseConfig: DatabaseConfig,
) : JsonDeserializer<PhyloTreeSequenceFiltersRequest>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): PhyloTreeSequenceFiltersRequest {
        val node = jsonParser.readValueAsTree<JsonNode>()
        val codec = jsonParser.codec

        val phyloTreeField = parsePhyloTreeProperty(node, fieldConverter, databaseConfig)
        val parsedCommonFields = parseCommonFields(node, codec)

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

@JsonComponent
class MRCASequenceFiltersRequestDeserializer(
    private val fieldConverter: CaseInsensitiveFieldConverter,
    private val databaseConfig: DatabaseConfig,
) : JsonDeserializer<MRCASequenceFiltersRequest>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): MRCASequenceFiltersRequest {
        val node = jsonParser.readValueAsTree<JsonNode>()
        val codec = jsonParser.codec

        val phyloTreeField = parsePhyloTreeProperty(node, fieldConverter, databaseConfig)
        val printNodesNotInTree = parsePrintNodesNotInTree(node)
        val parsedCommonFields = parseCommonFields(node, codec)

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
    fieldConverter: FieldConverter<Field>,
    databaseConfig: DatabaseConfig,
): Field {
    val phyloTreeField = node.get(PHYLO_TREE_FIELD_PROPERTY)
    if (phyloTreeField == null) {
        throw BadRequestException(
            "$PHYLO_TREE_FIELD_PROPERTY is required and must be a string representing a phylo tree field",
        )
    }
    if (!phyloTreeField.isTextual) {
        throw BadRequestException(
            "$PHYLO_TREE_FIELD_PROPERTY must be a string, but was ${phyloTreeField.nodeType}",
        )
    }
    return validatePhyloTreeField(phyloTreeField.textValue(), fieldConverter, databaseConfig)
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
