package org.genspectrum.lapis.util

import org.genspectrum.lapis.request.SPECIAL_REQUEST_PROPERTY_TYPES
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.BooleanNode
import tools.jackson.databind.node.DoubleNode
import tools.jackson.databind.node.JsonNodeFactory
import tools.jackson.databind.node.JsonNodeType.ARRAY
import tools.jackson.databind.node.JsonNodeType.BOOLEAN
import tools.jackson.databind.node.JsonNodeType.NULL
import tools.jackson.databind.node.JsonNodeType.NUMBER
import tools.jackson.databind.node.JsonNodeType.STRING
import tools.jackson.databind.node.LongNode
import tools.jackson.databind.node.StringNode

/**
 * Try conversion of special request properties to their correct types of form url encoded requests
 * If the type cannot be converted, it will always fall back to a string.
 */
fun tryToGuessTheType(entry: Map.Entry<String, List<String>>): JsonNode {
    val (key, values) = entry

    val jsonNodeType = SPECIAL_REQUEST_PROPERTY_TYPES[key]

    if (jsonNodeType == ARRAY || values.size != 1) {
        return JsonNodeFactory.instance.arrayNode()
            .addAll(values.map { StringNode(it) })
    }

    val value = values.first()

    return when (jsonNodeType) {
        null, NULL, STRING -> StringNode(value)
        BOOLEAN -> when (val booleanValue = value.toBooleanStrictOrNull()) {
            null -> StringNode(value)
            else -> BooleanNode.valueOf(booleanValue)
        }

        NUMBER -> value.toLongOrNull()?.let { LongNode(it) }
            ?: value.toDoubleOrNull()?.let { DoubleNode(it) }
            ?: StringNode(value)

        else -> StringNode(value)
    }
}
