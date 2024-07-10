package org.genspectrum.lapis.response

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.node.NullNode
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.controller.LapisHeaders.LAPIS_DATA_VERSION
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_X_FASTA
import org.genspectrum.lapis.silo.DataVersion
import org.springframework.boot.jackson.JsonComponent
import org.springframework.http.MediaType
import java.nio.charset.Charset
import java.util.stream.Stream

const val COUNT_PROPERTY = "count"

data class AggregationData(
    val count: Int,
    @Schema(hidden = true) val fields: Map<String, JsonNode>,
) : CsvRecord {
    override fun getValuesList() =
        fields.values
            .map { it.toCsvValue() }
            .plus(count.toString())

    override fun getHeader() = fields.keys.plus(COUNT_PROPERTY)
}

data class DetailsData(val map: Map<String, JsonNode>) : Map<String, JsonNode> by map, CsvRecord {
    override fun getValuesList() = values.map { it.toCsvValue() }

    override fun getHeader() = keys
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
    ): DetailsData {
        return DetailsData(p.readValueAs(object : TypeReference<Map<String, JsonNode>>() {}))
    }
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
    val sequence: String,
) {
    fun writeAsString() = ">$sequenceKey\n$sequence"
}

fun Stream<SequenceData>.writeFastaTo(
    response: HttpServletResponse,
    dataVersion: DataVersion,
) {
    response.setHeader(LAPIS_DATA_VERSION, dataVersion.dataVersion)
    if (response.contentType == null) {
        response.contentType = MediaType(TEXT_X_FASTA, Charset.defaultCharset()).toString()
    }
    response.outputStream.writer().use {
        for (sequenceData in this) {
            it.appendLine(sequenceData.writeAsString())
        }
    }
}

data class InfoData(
    val dataVersion: String,
)

@JsonComponent
class SequenceDataDeserializer(val databaseConfig: DatabaseConfig) : JsonDeserializer<SequenceData>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): SequenceData {
        val node = p.readValueAsTree<JsonNode>()
        val sequenceKey = node.get(databaseConfig.schema.primaryKey).asText()
        val sequence =
            node.fields().asSequence().first { it.key != databaseConfig.schema.primaryKey }.value.asText()
        return SequenceData(sequenceKey, sequence)
    }
}
