package org.genspectrum.lapis.request

import org.genspectrum.lapis.controller.BadRequestException
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@SpringBootTest
class CoOccurrenceRequestTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @ParameterizedTest
    @MethodSource("getTestCoOccurrenceRequests")
    fun `CoOccurrenceRequest is correctly deserialized from JSON`(
        input: String,
        expected: CoOccurrenceRequest,
    ) {
        val result = objectMapper.readValue<CoOccurrenceRequest>(input)

        assertThat(result, equalTo(expected))
    }

    @ParameterizedTest
    @MethodSource("getInvalidRequests")
    fun `Given invalid CoOccurrenceRequest then should throw an error`(
        input: String,
        expectedErrorMessage: String,
    ) {
        val exception = assertThrows(BadRequestException::class.java) {
            objectMapper.readValue<CoOccurrenceRequest>(input)
        }

        assertThat(exception.message, equalTo(expectedErrorMessage))
    }

    companion object {
        @JvmStatic
        fun getTestCoOccurrenceRequests() =
            listOf(
                Arguments.of(
                    """
                    {
                        "country": "Switzerland",
                        "positions": [1, 2, {"from": 100, "to": 102}]
                    }
                    """,
                    CoOccurrenceRequest(
                        sequenceFilters = mapOf("country" to listOf("Switzerland")),
                        nucleotideMutations = emptyList(),
                        aminoAcidMutations = emptyList(),
                        nucleotideInsertions = emptyList(),
                        aminoAcidInsertions = emptyList(),
                        positions = listOf(
                            CoOccurrencePosition.Single(1),
                            CoOccurrencePosition.Single(2),
                            CoOccurrencePosition.Range(100, 102),
                        ),
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "positions": ["5", 7]
                    }
                    """,
                    CoOccurrenceRequest(
                        sequenceFilters = emptyMap(),
                        nucleotideMutations = emptyList(),
                        aminoAcidMutations = emptyList(),
                        nucleotideInsertions = emptyList(),
                        aminoAcidInsertions = emptyList(),
                        positions = listOf(
                            CoOccurrencePosition.Single(5),
                            CoOccurrencePosition.Single(7),
                        ),
                    ),
                ),
            )

        @JvmStatic
        fun getInvalidRequests() =
            listOf(
                Arguments.of(
                    """{"positions": "not an array"}""",
                    "positions must be an array, but was \"not an array\" (STRING)",
                ),
                Arguments.of(
                    "{}",
                    "positions is required",
                ),
                Arguments.of(
                    """{"positions": ["100-110"]}""",
                    "Invalid entry '100-110' in positions: must be a number (e.g. '5')",
                ),
            )
    }
}
