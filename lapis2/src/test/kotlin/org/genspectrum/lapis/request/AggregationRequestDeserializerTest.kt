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
class AggregationRequestDeserializerTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @ParameterizedTest(name = "Test AggregationRequestDeserializer {1}")
    @MethodSource("getTestAggregationRequest")
    fun `AggregationRequest is correctly deserialized from JSON`(underTest: String, expected: AggregationRequest) {
        val result = objectMapper.readValue(underTest, AggregationRequest::class.java)

        MatcherAssert.assertThat(result, Matchers.equalTo(expected))
    }

    companion object {
        @JvmStatic
        fun getTestAggregationRequest() = listOf(
            Arguments.of(
                """
                {
                "country": "Switzerland",
                 "fields": ["division", "country"]
                }
                """,
                AggregationRequest(
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
                AggregationRequest(
                    mapOf("country" to "Switzerland"),
                    emptyList(),
                ),
            ),

        )
    }

    @Test
    fun `Given an AggregationRequest with fields not null or ArrayList it should return an error`() {
        val underTest = """
                {
                "country": "Switzerland",
                 "fields": "notAnArrayNode"
                }
                """

        assertThrows(IllegalArgumentException::class.java) {
            objectMapper.readValue(underTest, AggregationRequest::class.java)
        }
    }
}
