package org.genspectrum.lapis.request

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JsonComponent
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

data class OrderByField(
    val field: String,
    val order: Order,
)

@JsonFormat
enum class Order {
    @JsonProperty("ascending")
    ASCENDING,

    @JsonProperty("descending")
    DESCENDING,
}

@JsonComponent
class OrderByFieldDeserializer : JsonDeserializer<OrderByField>() {
    override fun deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): OrderByField {
        return when (val value = jsonParser.readValueAsTree<JsonNode>()) {
            is TextNode -> OrderByField(value.asText(), Order.ASCENDING)
            is ObjectNode -> deserializeOrderByField(value)
            else -> throw BadRequestException("orderByField must be a string or an object")
        }
    }

    private fun deserializeOrderByField(value: ObjectNode): OrderByField {
        val fieldNode = value.get("field")
        if (fieldNode == null || fieldNode !is TextNode) {
            throw BadRequestException("orderByField must have a string property \"field\"")
        }

        val ascending = when (value.get("type")?.asText()) {
            "ascending", null -> Order.ASCENDING
            "descending" -> Order.DESCENDING
            else -> throw BadRequestException("orderByField type must be \"ascending\" or \"descending\"")
        }

        return OrderByField(fieldNode.asText(), ascending)
    }
}

@Component
class OrderByFieldConverter : Converter<String, OrderByField> {
    override fun convert(source: String) = OrderByField(source, Order.ASCENDING)
}
