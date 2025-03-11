package org.genspectrum.lapis.response

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.NullNode
import io.swagger.v3.oas.annotations.media.Schema
import org.genspectrum.lapis.config.DatabaseConfig
import org.springframework.boot.jackson.JsonComponent

const val COUNT_PROPERTY = "count"

data class AggregationData(
    val count: Int,
    @Schema(hidden = true) val fields: Map<String, JsonNode>,
) : CsvRecord {
    override fun getValuesList(comparator: Comparator<String>) =
        fields.entries
            .sortedWith { a, b -> comparator.compare(a.key, b.key) }
            .map { (_, value) -> value.toCsvValue() }
            .plus(count.toString())

    override fun getHeader(comparator: Comparator<String>) = fields.keys.sortedWith(comparator) + COUNT_PROPERTY
}

data class DetailsData(
    val map: Map<String, JsonNode>,
) : Map<String, JsonNode> by map,
    CsvRecord {
    override fun getValuesList(comparator: Comparator<String>) =
        entries
            .sortedWith { a, b -> comparator.compare(a.key, b.key) }
            .map { (_, value) -> value.toCsvValue() }

    override fun getHeader(comparator: Comparator<String>) = keys.sortedWith(comparator)
}

private fun JsonNode.toCsvValue() =
    when (this) {
        is NullNode -> null
        else -> asText()
    }

@JsonComponent
class DetailsDataDeserializer : JsonDeserializer<DetailsData>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): DetailsData = DetailsData(p.readValueAs(object : TypeReference<Map<String, JsonNode>>() {}))
}

@JsonComponent
class AggregationDataSerializer : JsonSerializer<AggregationData>() {
    override fun serialize(
        value: AggregationData,
        gen: JsonGenerator,
        serializers: SerializerProvider,
    ) {
        gen.writeStartObject()
        gen.writeNumberField(COUNT_PROPERTY, value.count)
        value.fields.forEach { (key, value) -> gen.writeObjectField(key, value) }
        gen.writeEndObject()
    }
}

@JsonComponent
class AggregationDataDeserializer : JsonDeserializer<AggregationData>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): AggregationData {
        val node = p.readValueAsTree<JsonNode>()
        val count = node.get(COUNT_PROPERTY).asInt()
        val fields = node.fields().asSequence().filter { it.key != COUNT_PROPERTY }.associate { it.key to it.value }
        return AggregationData(count, fields)
    }
}

data class MutationData(
    val mutation: String,
    val count: Int,
    val proportion: Double,
    val sequenceName: String,
    val mutationFrom: String,
    val mutationTo: String,
    val position: Int,
    val coverage: Int,
)

data class InsertionData(
    val count: Int,
    val insertion: String,
    val insertedSymbols: String,
    val position: Int,
    val sequenceName: String,
)

data class SequenceData(
    val sequenceKey: String,
    val sequence: String?,
    @JsonIgnore
    val sequenceName: String,
) {
    fun writeAsFasta() = ">$sequenceKey\n$sequence"
}

data class InfoData(
    val dataVersion: String,
    val siloVersion: String?,
)

@JsonComponent
class SequenceDataDeserializer(
    val databaseConfig: DatabaseConfig,
) : JsonDeserializer<SequenceData>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): SequenceData {
        val node = p.readValueAsTree<JsonNode>()
        val sequenceKey = node.get(databaseConfig.schema.primaryKey).asText()

        val (key, value) = node.fields().asSequence().first { it.key != databaseConfig.schema.primaryKey }
        val sequence = when (value.nodeType) {
            JsonNodeType.NULL -> null
            JsonNodeType.STRING -> value.asText()
            else -> throw RuntimeException(
                "Error deserializing sequence data: got unexpected value $value of type ${value.nodeType}",
            )
        }

        return SequenceData(
            sequenceKey = sequenceKey,
            sequence = sequence,
            sequenceName = key,
        )
    }
}

@JsonComponent
class SequenceDataSerializer(
    val databaseConfig: DatabaseConfig,
) : JsonSerializer<SequenceData>() {
    override fun serialize(
        value: SequenceData,
        gen: JsonGenerator,
        serializers: SerializerProvider,
    ) {
        gen.writeStartObject()
        gen.writeStringField(databaseConfig.schema.primaryKey, value.sequenceKey)
        gen.writeStringField(value.sequenceName, value.sequence)
        gen.writeEndObject()
    }
}
