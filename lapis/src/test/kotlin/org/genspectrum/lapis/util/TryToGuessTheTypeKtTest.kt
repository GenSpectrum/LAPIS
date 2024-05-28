package org.genspectrum.lapis.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.TextNode
import org.genspectrum.lapis.controller.DOWNLOAD_AS_FILE_PROPERTY
import org.genspectrum.lapis.controller.LIMIT_PROPERTY
import org.genspectrum.lapis.controller.NUCLEOTIDE_MUTATIONS_PROPERTY
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class TryToGuessTheTypeKtTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("getTestCases")
    fun `GIVEN some input THEN maps to the output of expected type`(
        description: String,
        input: Map.Entry<String, List<String>>,
        expected: JsonNode,
    ) {
        val actual = tryToGuessTheType(input)

        assertThat(actual, `is`(expected))
    }

    companion object {
        @JvmStatic
        val testCases = listOf(
            Arguments.of(
                "non-special field with single value should map to single text value",
                entry("someField", listOf("value")),
                TextNode("value"),
            ),
            Arguments.of(
                "non-special field with multiple values should map to multiple text values",
                entry("someFieldWithMultipleValues", listOf("value1", "value2")),
                JsonNodeFactory.instance.arrayNode().add("value1").add("value2"),
            ),
            Arguments.of(
                "special array-valued field should always map to array",
                entry(NUCLEOTIDE_MUTATIONS_PROPERTY, listOf("value1")),
                JsonNodeFactory.instance.arrayNode().add("value1"),
            ),
            Arguments.of(
                "special bool-valued field should map non-boolean value to text",
                entry(DOWNLOAD_AS_FILE_PROPERTY, listOf("not a boolean")),
                TextNode("not a boolean"),
            ),
            Arguments.of(
                "special bool-valued field should map true to boolean",
                entry(DOWNLOAD_AS_FILE_PROPERTY, listOf("true")),
                BooleanNode.TRUE,
            ),
            Arguments.of(
                "special bool-valued field should map false to boolean",
                entry(DOWNLOAD_AS_FILE_PROPERTY, listOf("false")),
                BooleanNode.FALSE,
            ),
            Arguments.of(
                "special number-valued field should map integer to long node",
                entry(LIMIT_PROPERTY, listOf("42")),
                LongNode(42),
            ),
            Arguments.of(
                "special number-valued field should map float to double node",
                entry(LIMIT_PROPERTY, listOf("42.0")),
                DoubleNode(42.0),
            ),
            Arguments.of(
                "special number-valued field should map non-number to text",
                entry(LIMIT_PROPERTY, listOf("not a number")),
                TextNode("not a number"),
            ),
        )

        private fun entry(
            key: String,
            value: List<String>,
        ) = DummyEntry(key, value)
    }

    private data class DummyEntry(
        override val key: String,
        override val value: List<String>,
    ) : Map.Entry<String, List<String>>
}
