package org.genspectrum.lapis.response

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.boot.jackson.JsonComponent

data class AggregationData(val count: Int, @Schema(hidden = true) val fields: Map<String, JsonNode>)

@JsonComponent
class AggregationDataSerializer : JsonSerializer<AggregationData>() {
    override fun serialize(value: AggregationData, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeNumberField("count", value.count)
        value.fields.forEach { (key, value) -> gen.writeObjectField(key, value) }
        gen.writeEndObject()
    }
}

@JsonComponent
class AggregationDataDeserializer : JsonDeserializer<AggregationData>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): AggregationData {
        val node = p.readValueAsTree<JsonNode>()
        val count = node.get("count").asInt()
        val fields = node.fields().asSequence().filter { it.key != "count" }.associate { it.key to it.value }
        return AggregationData(count, fields)
    }
}

data class MutationData(
    @Schema(
        example = "G29741T",
        description = "(nucleotide symbol in reference genome)(position in genome)" +
            "(mutation's target nucleotide symbol)",
    ) val mutation: String,
    @Schema(
        example = "42",
        description = "Total number of sequences with this mutation matching the given sequence filter criteria",
    ) val count: Int,
    @Schema(
        example = "0.54321",
        description = "Number of sequences with this mutation divided by the total number sequences matching the " +
            "given filter criteria",
    ) val proportion: Double,
)
