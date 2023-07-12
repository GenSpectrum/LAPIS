package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import org.springframework.boot.jackson.JsonComponent

const val FIELDS_PROPERTY = "fields"

data class SequenceFiltersRequestWithFields(
    override val sequenceFilters: Map<String, String>,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aaMutations: List<AminoAcidMutation>,
    val fields: List<String>,
) : CommonSequenceFilters

@JsonComponent
class SequenceFiltersRequestWithFieldsDeserializer : JsonDeserializer<SequenceFiltersRequestWithFields>() {
    override fun deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): SequenceFiltersRequestWithFields {
        val node = jsonParser.readValueAsTree<JsonNode>()
        val codec = jsonParser.codec

        val fields = when (val fields = node.get(FIELDS_PROPERTY)) {
            null -> emptyList()
            is ArrayNode -> fields.asSequence().map { it.asText() }.toList()
            else -> throw IllegalArgumentException(
                "fields must be an array or null",
            )
        }

        val (nucleotideMutations, aminoAcidMutations, sequenceFilters) = parseCommonFields(node, codec)

        return SequenceFiltersRequestWithFields(sequenceFilters, nucleotideMutations, aminoAcidMutations, fields)
    }
}
