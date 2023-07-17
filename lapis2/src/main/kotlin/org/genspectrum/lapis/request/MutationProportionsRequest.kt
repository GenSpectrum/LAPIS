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
            -> {}
            else -> throw IllegalArgumentException("minProportion must be a number")
        }

        val (nucleotideMutations, aminoAcidMutations, primitiveFields) = parseCommonFields(node, codec)

        val (minProportions, sequenceFilters) = primitiveFields.entries.partition { it.key == MIN_PROPORTION_PROPERTY }

        val minProportion = if (minProportions.isEmpty()) {
            DEFAULT_MIN_PROPORTION
        } else {
            minProportions.single().value.toDouble()
        }

        return MutationProportionsRequest(
            sequenceFilters.associate { it.key to it.value },
            nucleotideMutations,
            aminoAcidMutations,
            minProportion,
        )
    }
}
