package org.genspectrum.lapis.request

import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JacksonComponent
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.node.JsonNodeType

/**
 * A single 1-based position, or an inclusive range of 1-based positions, requested for a co-occurrence query.
 */
sealed class CoOccurrencePosition {
    data class Single(
        val position: Int,
    ) : CoOccurrencePosition()

    data class Range(
        val from: Int,
        val to: Int,
    ) : CoOccurrencePosition()
}

/**
 * Parses a single token from a GET request (or a form-urlencoded POST body) into a [CoOccurrencePosition].
 * A token must be a plain positive integer (e.g. "5"). Ranges are only accepted as JSON objects
 * (e.g. `{"from": 100, "to": 110}`), not as strings.
 */
fun parsePositionToken(token: String): CoOccurrencePosition {
    val trimmed = token.trim()

    val position = trimmed.toIntOrNull()
        ?: throw BadRequestException(
            "Invalid entry '$token' in $POSITIONS_PROPERTY: must be a number (e.g. '5')",
        )
    return CoOccurrencePosition.Single(position)
}

/**
 * Safety limit on the number of distinct positions a single co-occurrence request may expand to.
 * This protects against out-of-memory/denial-of-service from a single huge range (e.g. `1-2000000000`)
 * or many ranges/positions combined. No known pathogen genome comes close to this length.
 */
const val MAX_CO_OCCURRENCE_POSITIONS = 1_000_000

/**
 * Expands a list of [CoOccurrencePosition]s into a sorted list of distinct 1-based positions.
 * Validates that:
 * - the list is not empty
 * - all positions/bounds are >= 1
 * - for ranges, `from` <= `to`
 * - the total number of distinct expanded positions does not exceed [MAX_CO_OCCURRENCE_POSITIONS]
 */
fun List<CoOccurrencePosition>.expandAndValidatePositions(): List<Int> {
    if (this.isEmpty()) {
        throw BadRequestException("$POSITIONS_PROPERTY must not be empty")
    }

    val result = mutableSetOf<Int>()

    for (position in this) {
        when (position) {
            is CoOccurrencePosition.Single -> {
                if (position.position < 1) {
                    throw BadRequestException(
                        "Invalid position ${position.position} in $POSITIONS_PROPERTY: must be >= 1",
                    )
                }
                result.add(position.position)
            }

            is CoOccurrencePosition.Range -> {
                if (position.from < 1 || position.to < 1) {
                    throw BadRequestException(
                        "Invalid range ${position.from}-${position.to} in $POSITIONS_PROPERTY: " +
                            "bounds must be >= 1",
                    )
                }
                if (position.from > position.to) {
                    throw BadRequestException(
                        "Invalid range ${position.from}-${position.to} in $POSITIONS_PROPERTY: " +
                            "'from' must be <= 'to'",
                    )
                }

                // Compute the span before materializing it, so a huge range (e.g. 1-2000000000) can't
                // exhaust memory before we get a chance to reject it.
                val rangeSize = position.to.toLong() - position.from.toLong() + 1
                if (rangeSize > MAX_CO_OCCURRENCE_POSITIONS) {
                    throw BadRequestException(
                        "Invalid range ${position.from}-${position.to} in $POSITIONS_PROPERTY: " +
                            "spans $rangeSize positions, which exceeds the maximum of " +
                            "$MAX_CO_OCCURRENCE_POSITIONS positions per request",
                    )
                }

                (position.from..position.to).forEach { result.add(it) }
            }
        }

        if (result.size > MAX_CO_OCCURRENCE_POSITIONS) {
            throw BadRequestException(
                "$POSITIONS_PROPERTY must not expand to more than $MAX_CO_OCCURRENCE_POSITIONS " +
                    "distinct positions",
            )
        }
    }

    return result.sorted()
}

/**
 * Deserializes a single entry of the `positions` array of a co-occurrence request body.
 * An entry can be:
 * - a JSON number, e.g. `5`
 * - a JSON object with `from`/`to`, e.g. `{"from": 100, "to": 110}`
 * - a JSON string with a plain number (used for form-urlencoded requests), e.g. `"5"`
 */
@JacksonComponent
class CoOccurrencePositionDeserializer : ValueDeserializer<CoOccurrencePosition>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): CoOccurrencePosition {
        val node = jsonParser.readValueAsTree<JsonNode>()

        return when (node.nodeType) {
            JsonNodeType.NUMBER -> CoOccurrencePosition.Single(node.asInt())

            JsonNodeType.STRING -> parsePositionToken(node.asString())

            JsonNodeType.OBJECT -> {
                val fromNode = node.get("from")
                val toNode = node.get("to")
                if (fromNode == null ||
                    toNode == null ||
                    !fromNode.canConvertToInt() ||
                    !toNode.canConvertToInt()
                ) {
                    throw BadRequestException(
                        "Each object entry in $POSITIONS_PROPERTY must have integer 'from' and 'to' " +
                            "properties that fit in a 32-bit integer, but was $node",
                    )
                }
                CoOccurrencePosition.Range(fromNode.asInt(), toNode.asInt())
            }

            else -> throw BadRequestException(
                "Each entry in $POSITIONS_PROPERTY must be a number, a string, or an object with " +
                    "'from'/'to', ${butWas(node)}",
            )
        }
    }
}
