package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.NumericNode
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.controller.MIN_PROPORTION_PROPERTY
import org.springframework.boot.jackson.JsonComponent

const val DEFAULT_MIN_PROPORTION = 0.05

data class MutationProportionsRequest(
    override val sequenceFilters: Map<String, String>,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aaMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
    val minProportion: Double? = null,
    override val orderByFields: List<OrderByField> = emptyList(),
    override val limit: Int? = null,
    override val offset: Int? = null,
) : CommonSequenceFilters

@JsonComponent
class MutationProportionsRequestDeserializer : JsonDeserializer<MutationProportionsRequest>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): MutationProportionsRequest {
        val node = jsonParser.readValueAsTree<JsonNode>()
        val codec = jsonParser.codec

        val minProportion = when (val minProportionNode = node.get(MIN_PROPORTION_PROPERTY)) {
            null, is NullNode -> DEFAULT_MIN_PROPORTION
            is NumericNode -> minProportionNode.doubleValue()

            else -> throw BadRequestException("minProportion must be a number")
        }

        val parsedCommonFields = parseCommonFields(node, codec)

        return MutationProportionsRequest(
            parsedCommonFields.sequenceFilters,
            parsedCommonFields.nucleotideMutations,
            parsedCommonFields.aminoAcidMutations,
            parsedCommonFields.nucleotideInsertions,
            parsedCommonFields.aminoAcidInsertions,
            minProportion,
            parsedCommonFields.orderByFields,
            parsedCommonFields.limit,
            parsedCommonFields.offset,
        )
    }
}
