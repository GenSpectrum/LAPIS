package org.genspectrum.lapis.request

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SequenceFiltersRequestWithFieldsDeserializerTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @ParameterizedTest(name = "Test SequenceFiltersRequestWithFieldsDeserializer {1}")
    @MethodSource("getTestSequenceFiltersRequestWithFields")
    fun `AggregationRequest is correctly deserialized from JSON`(
        underTest: String,
        expected: SequenceFiltersRequestWithFields,
    ) {
        val result = objectMapper.readValue(underTest, SequenceFiltersRequestWithFields::class.java)

        MatcherAssert.assertThat(result, Matchers.equalTo(expected))
    }

    companion object {
        @JvmStatic
        fun getTestSequenceFiltersRequestWithFields() = listOf(
            Arguments.of(
                """
                {
                    "country": "Switzerland",
                    "fields": ["division", "country"]
                }
                """,
                SequenceFiltersRequestWithFields(
                    mapOf("country" to "Switzerland"),
                    listOf("division", "country"),
                ),
            ),
            Arguments.of(
                """
                {
                    "country": "Switzerland"
                }
                """,
                SequenceFiltersRequestWithFields(
                    mapOf("country" to "Switzerland"),
                    emptyList(),
                ),
            ),

        )
    }

    @Test
    fun `Given a SequenceFiltersRequestWithFields with fields not null or ArrayList it should return an error`() {
        val underTest = """
            {
                "country": "Switzerland",
                "fields": "notAnArrayNode"
            }
        """

        assertThrows(IllegalArgumentException::class.java) {
            objectMapper.readValue(underTest, SequenceFiltersRequestWithFields::class.java)
        }
    }
}
