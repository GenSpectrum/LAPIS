package org.genspectrum.lapis.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.JsonNodeType.ARRAY
import com.fasterxml.jackson.databind.node.JsonNodeType.BOOLEAN
import com.fasterxml.jackson.databind.node.JsonNodeType.NULL
import com.fasterxml.jackson.databind.node.JsonNodeType.NUMBER
import com.fasterxml.jackson.databind.node.JsonNodeType.STRING
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.TextNode
import org.genspectrum.lapis.controller.SPECIAL_REQUEST_PROPERTY_TYPES

/**
 * Try conversion of special request properties to their correct types of form url encoded requests
 * If the type cannot be converted, it will always fall back to a string.
 */
fun tryToGuessTheType(entry: Map.Entry<String, List<String>>): JsonNode {
    val (key, values) = entry

    val jsonNodeType = SPECIAL_REQUEST_PROPERTY_TYPES[key]

    if (jsonNodeType == ARRAY || values.size != 1) {
        return JsonNodeFactory.instance.arrayNode()
            .addAll(values.map { TextNode(it) })
    }

    val value = values.first()

    return when (jsonNodeType) {
        null, NULL, STRING -> TextNode(value)
        BOOLEAN -> when (val booleanValue = value.toBooleanStrictOrNull()) {
            null -> TextNode(value)
            else -> BooleanNode.valueOf(booleanValue)
        }

        NUMBER -> value.toLongOrNull()?.let { LongNode(it) }
            ?: value.toDoubleOrNull()?.let { DoubleNode(it) }
            ?: TextNode(value)

        else -> TextNode(value)
    }
}
