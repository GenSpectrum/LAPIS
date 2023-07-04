package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import org.genspectrum.lapis.controller.FIELDS_PROPERTY
import org.springframework.boot.jackson.JsonComponent

data class SequenceFiltersRequestWithFields(
    val sequenceFilters: Map<String, String>,
    val fields: List<String>,
)

@JsonComponent
class SequenceFiltersRequestWithFieldsDeserializer : JsonDeserializer<SequenceFiltersRequestWithFields>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): SequenceFiltersRequestWithFields {
        val node = p.readValueAsTree<JsonNode>()

        val fields = when (node.get(FIELDS_PROPERTY)) {
            null -> emptyList()
            is ArrayNode -> node.get(FIELDS_PROPERTY).asSequence().map { it.asText() }.toList()
            else -> throw IllegalArgumentException(
                "Fields in SequenceFiltersRequestWithFields must be an array or null",
            )
        }

        val sequenceFilters =
            node.fields().asSequence().filter { it.key != FIELDS_PROPERTY }.associate { it.key to it.value.asText() }

        return SequenceFiltersRequestWithFields(sequenceFilters, fields)
    }
}
