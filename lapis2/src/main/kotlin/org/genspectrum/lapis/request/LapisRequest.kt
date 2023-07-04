package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import org.springframework.boot.jackson.JsonComponent

data class AggregationRequest(
    val sequenceFilters: Map<String, String>,
    val fields: List<String>,
)

@JsonComponent
class AggregationRequestDeserializer : JsonDeserializer<AggregationRequest>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): AggregationRequest {
        val node = p.readValueAsTree<JsonNode>()

        val fields = when (node.get("fields")) {
            null -> emptyList()
            is ArrayNode -> node.get("fields").asSequence().map { it.asText() }.toList()
            else -> throw IllegalArgumentException("Fields in AggregationRequest must be an array or null")
        }

        val sequenceFilters =
            node.fields().asSequence().filter { it.key != "fields" }.associate { it.key to it.value.asText() }

        return AggregationRequest(sequenceFilters, fields)
    }
}
