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

/**
 * Order by can either be a list of fields or it can be random, with an integer seed
 * to get deterministic random ordering.
 */
sealed class OrderBySpec {
    data class ByFields(
        val fields: List<OrderByField>,
    ) : OrderBySpec()

    data class Random(
        val seed: Int?,
    ) : OrderBySpec()

    companion object {
        val EMPTY = ByFields(emptyList())
    }
}

/**
 * Deserialize an `OrderBySpec`. Supports a list of fields like:
 * `[{field: country}, {field: date}]`
 * as well as random:
 * `{random: true}`
 * or random with seed:
 * `{random: 123}`
 */
@JsonComponent
class OrderBySpecDeserializer : JsonDeserializer<OrderBySpec>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): OrderBySpec {
        val node = p.readValueAsTree<JsonNode>()

        return when {
            node.isArray -> {
                val fields =
                    node.map { fieldNode ->
                        p.codec.treeToValue(fieldNode, OrderByField::class.java)
                    }
                fields.toOrderBySpec()
            }
            node.isObject && node.has("random") -> {
                val randomValue = node.get("random")
                val seed =
                    when {
                        randomValue.isBoolean && randomValue.asBoolean() -> null
                        randomValue.isBoolean && !randomValue.asBoolean() ->
                            throw BadRequestException("random must be true or an integer seed")
                        randomValue.isInt -> randomValue.asInt()
                        else -> throw BadRequestException("random must be true or an integer seed")
                    }
                OrderBySpec.Random(seed)
            }
            else ->
                throw BadRequestException(
                    "orderBy must be an array of fields or {random: true|<seed>}",
                )
        }
    }
}

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
class OrderByFieldDeserializer(
    private val orderByFieldsCleaner: OrderByFieldsCleaner,
) : JsonDeserializer<OrderByField>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): OrderByField =
        when (val value = jsonParser.readValueAsTree<JsonNode>()) {
            // TODO - It seems like maybe here we can't deserialize 'random' fields? Is that an issue?
            is TextNode -> OrderByField(orderByFieldsCleaner.clean(value.asText()), Order.ASCENDING)
            is ObjectNode -> deserializeOrderByField(value)
            else -> throw BadRequestException("orderByField must be a string or an object")
        }

    private fun deserializeOrderByField(value: ObjectNode): OrderByField {
        val fieldNode = value.get("field")
        if (fieldNode == null || fieldNode !is TextNode) {
            throw BadRequestException("orderByField must have a string property \"field\", was $value")
        }

        val ascending = when (value.get("type")?.asText()) {
            "ascending", null -> Order.ASCENDING
            "descending" -> Order.DESCENDING
            else -> throw BadRequestException("orderByField type must be \"ascending\" or \"descending\"")
        }

        return OrderByField(orderByFieldsCleaner.clean(fieldNode.asText()), ascending)
    }
}

/**
 * The `OrderByFieldConverter` converts a list of strings into a list of fields to order by.
 * It checks that all the fields exist, or start with "random".
 * The `Converter` is used automatically by Spring in the GET requests.
 */
@Component
class OrderByFieldConverter(
    private val orderByFieldsCleaner: OrderByFieldsCleaner,
) : Converter<String, OrderByField> {
    override fun convert(source: String): OrderByField {
        val field =
            if (source.startsWith("random")) {
                // validation and conversion happens later on, in `toOrderBySpec`.
                source
            } else {
                orderByFieldsCleaner.clean(source)
            }

        return OrderByField(field = field, order = Order.ASCENDING)
    }
}

@Component
class OrderByFieldsCleaner(
    private val caseInsensitiveFieldsCleaner: CaseInsensitiveFieldsCleaner,
) {
    fun clean(fieldName: String): String = caseInsensitiveFieldsCleaner.clean(fieldName) ?: fieldName
}

/**
 * Converts a list of fields to order by or an OrderBySpec.
 * If the list has any element and the field name is `random(123)`,
 * it will convert it into the appropriate OrderBySpec for random ordering with a seed.
 * Likewise for just `random` (without seed).
 * Any other input starting with random will cause an error.
 */
fun List<OrderByField>?.toOrderBySpec(): OrderBySpec {
    val nonNullThis = this.orEmpty()

    val randomField = nonNullThis.find { it.field.startsWith("random") }

    return when {
        randomField == null -> OrderBySpec.ByFields(nonNullThis)
        randomField.field == "random" -> OrderBySpec.Random(seed = null)
        else -> {
            val seedPattern = Regex("""^random\((\d+)\)$""")
            val match = seedPattern.matchEntire(randomField.field)
            val seed = match?.groupValues?.get(1)?.toInt()
            if (seed == null) {
                throw BadRequestException(
                    "Invalid random orderBy format: '${randomField.field}'. " +
                        "Use 'random' or 'random(<seed>)' where seed is a positive integer.",
                )
            }
            OrderBySpec.Random(seed = seed)
        }
    }
}
