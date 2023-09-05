package org.genspectrum.lapis.request

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class MutationProportionsRequestTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @ParameterizedTest
    @MethodSource("getTestMutationProportionsRequests")
    fun `MutationProportionsRequest is correctly deserialized from JSON`(
        input: String,
        expected: MutationProportionsRequest,
    ) {
        val result = objectMapper.readValue<MutationProportionsRequest>(input)

        assertThat(result, equalTo(expected))
    }

    @ParameterizedTest
    @MethodSource("getInvalidRequests")
    fun `Given invalid MutationProportionsRequest then should throw an error`(
        input: String,
        expectedErrorMessage: String,
    ) {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            objectMapper.readValue<MutationProportionsRequest>(input)
        }

        assertThat(exception.message, equalTo(expectedErrorMessage))
    }

    companion object {
        @JvmStatic
        fun getTestMutationProportionsRequests() = listOf(
            Arguments.of(
                """
                {
                    "country": "Switzerland"
                }
                """,
                MutationProportionsRequest(mapOf("country" to "Switzerland"), emptyList(), emptyList()),
            ),
            Arguments.of(
                """
                {
                    "nucleotideMutations": ["T1-", "A23062T"]
                }
                """,
                MutationProportionsRequest(
                    emptyMap(),
                    listOf(NucleotideMutation(null, 1, "-"), NucleotideMutation(null, 23062, "T")),
                    emptyList(),
                ),
            ),
            Arguments.of(
                """
                {
                    "aminoAcidMutations": ["S:501Y", "ORF1b:12"]
                }
                """,
                MutationProportionsRequest(
                    emptyMap(),
                    emptyList(),
                    listOf(AminoAcidMutation("S", 501, "Y"), AminoAcidMutation("ORF1b", 12, null)),
                ),
            ),
            Arguments.of(
                """
                {
                    "minProportion": 0.7
                }
                """,
                MutationProportionsRequest(emptyMap(), emptyList(), emptyList(), 0.7),
            ),
            Arguments.of(
                """
                {
                    "accessKey": "some access key"                
                }
                """,
                MutationProportionsRequest(emptyMap(), emptyList(), emptyList()),
            ),
            Arguments.of(
                """
                {
                }
                """,
                MutationProportionsRequest(emptyMap(), emptyList(), emptyList()),
            ),
        )

        @JvmStatic
        fun getInvalidRequests() = listOf(
            Arguments.of(
                """
                {
                    "minProportion": "not a number"
                }
                """,
                "minProportion must be a number",
            ),
            Arguments.of(
                """
                {
                    "minProportion": ["not a number"]
                }
                """,
                "minProportion must be a number",
            ),
            Arguments.of(
                """
                {
                    "nucleotideMutations": "not an array"
                }
                """,
                "nucleotideMutations must be an array or null",
            ),
            Arguments.of(
                """
                {
                    "aminoAcidMutations": "not an array"
                }
                """,
                "aminoAcidMutations must be an array or null",
            ),
        )
    }
}
