package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import org.springframework.boot.jackson.JsonComponent

const val MIN_PROPORTION_PROPERTY = "minProportion"
const val DEFAULT_MIN_PROPORTION = 0.05

data class MutationProportionsRequest(
    override val sequenceFilters: Map<String, String>,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aaMutations: List<AminoAcidMutation>,
    val minProportion: Double,
    override val orderByFields: List<OrderByField> = emptyList(),
    override val limit: Int? = null,
    override val offset: Int? = null,
) : CommonSequenceFilters

@JsonComponent
class MutationProportionsRequestDeserializer : JsonDeserializer<MutationProportionsRequest>() {
    override fun deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): MutationProportionsRequest {
        val node = jsonParser.readValueAsTree<JsonNode>()
        val codec = jsonParser.codec

        when (node.get(MIN_PROPORTION_PROPERTY)?.nodeType) {
            null,
            JsonNodeType.MISSING,
            JsonNodeType.NULL,
            JsonNodeType.NUMBER,
            -> {
            }

            else -> throw IllegalArgumentException("minProportion must be a number")
        }

        val parsedCommonFields = parseCommonFields(node, codec)

        val (minProportions, actualSequenceFilters) = parsedCommonFields.sequenceFilters.entries.partition {
            it.key == MIN_PROPORTION_PROPERTY
        }

        val minProportion = if (minProportions.isEmpty()) {
            DEFAULT_MIN_PROPORTION
        } else {
            minProportions.single().value.toDouble()
        }

        return MutationProportionsRequest(
            actualSequenceFilters.associate { it.key to it.value },
            parsedCommonFields.nucleotideMutations,
            parsedCommonFields.aminoAcidMutations,
            minProportion,
            parsedCommonFields.orderByFields,
            parsedCommonFields.limit,
            parsedCommonFields.offset,
        )
    }
}
