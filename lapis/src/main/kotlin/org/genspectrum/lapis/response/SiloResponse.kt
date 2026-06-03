package org.genspectrum.lapis.response

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.boot.jackson.JacksonComponent
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.JsonNode
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer

const val COUNT_PROPERTY = "count"

data class AggregationData(
    val count: Long,
    @param:Schema(hidden = true) val fields: Map<String, JsonNode>,
)

data class DetailsData(
    val map: Map<String, JsonNode>,
) : Map<String, JsonNode> by map

@JacksonComponent
class AggregationDataSerializer : ValueSerializer<AggregationData>() {
    override fun serialize(
        value: AggregationData,
        gen: JsonGenerator,
        serializers: SerializationContext,
    ) {
        gen.writeStartObject()
        gen.writeNumberProperty(COUNT_PROPERTY, value.count)
        value.fields.forEach { (key, value) -> gen.writePOJOProperty(key, value) }
        gen.writeEndObject()
    }
}

data class MutationData(
    val mutation: String?,
    val count: Int?,
    val proportion: Double?,
    val sequenceName: String?,
    val mutationFrom: String?,
    val mutationTo: String?,
    val position: Int?,
    val coverage: Int?,
)

data class InsertionData(
    val count: Int,
    val insertion: String,
    val insertedSymbols: String,
    val position: Int,
    val sequenceName: String,
)

data class MostCommonAncestorData(
    val mrcaNode: String?,
    val missingNodeCount: Int,
    val missingFromTree: String?,
)

data class PhyloSubtreeData(
    val subtreeNewick: String,
    val missingNodeCount: Int,
    val missingFromTree: String?,
)

data class SequenceData(
    val map: Map<String, JsonNode>,
) : Map<String, JsonNode> by map

data class InfoData(
    val dataVersion: String,
    val siloVersion: String?,
)
