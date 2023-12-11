package org.genspectrum.lapis.request

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.genspectrum.lapis.controller.BadRequestException
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SequenceFiltersRequestWithFieldsTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @ParameterizedTest
    @MethodSource("getTestSequenceFiltersRequestWithFields")
    fun `SequenceFiltersRequestWithFields is correctly deserialized from JSON`(
        input: String,
        expected: SequenceFiltersRequestWithFields,
    ) {
        val result = objectMapper.readValue<SequenceFiltersRequestWithFields>(input)

        assertThat(result, equalTo(expected))
    }

    @ParameterizedTest
    @MethodSource("getInvalidRequests")
    fun `Given invalid SequenceFiltersRequestWithFields then should throw an error`(
        input: String,
        expectedErrorMessage: String,
    ) {
        val exception = assertThrows(BadRequestException::class.java) {
            objectMapper.readValue<SequenceFiltersRequestWithFields>(input)
        }

        assertThat(exception.message, equalTo(expectedErrorMessage))
    }

    companion object {
        @JvmStatic
        fun getTestSequenceFiltersRequestWithFields() =
            listOf(
                Arguments.of(
                    """
                {
                    "country": "Switzerland",
                    "fields": ["division", "country"]
                }
                """,
                    SequenceFiltersRequestWithFields(
                        mapOf("country" to "Switzerland"),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        listOf("division", "country"),
                    ),
                ),
                Arguments.of(
                    """
                {
                    "nucleotideMutations": ["T1-", "A23062T"],
                    "fields": ["division", "country"]
                }
                """,
                    SequenceFiltersRequestWithFields(
                        emptyMap(),
                        listOf(NucleotideMutation(null, 1, "-"), NucleotideMutation(null, 23062, "T")),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        listOf("division", "country"),
                    ),
                ),
                Arguments.of(
                    """
                {
                    "aminoAcidMutations": ["S:501Y", "ORF1b:12"],
                    "fields": ["division", "country"]
                }
                """,
                    SequenceFiltersRequestWithFields(
                        emptyMap(),
                        emptyList(),
                        listOf(AminoAcidMutation("S", 501, "Y"), AminoAcidMutation("ORF1b", 12, null)),
                        emptyList(),
                        emptyList(),
                        listOf("division", "country"),
                    ),
                ),
                Arguments.of(
                    """
                {
                    "nucleotideInsertions": ["ins_S:501:Y", "ins_12:ABCD"],
                    "fields": ["division", "country"]
                }
                """,
                    SequenceFiltersRequestWithFields(
                        emptyMap(),
                        emptyList(),
                        emptyList(),
                        listOf(
                            NucleotideInsertion(501, "Y", "S"),
                            NucleotideInsertion(12, "ABCD", null),
                        ),
                        emptyList(),
                        listOf("division", "country"),
                    ),
                ),
                Arguments.of(
                    """
                {
                    "aminoAcidInsertions": ["ins_S:501:Y", "ins_ORF1:12:ABCD"],
                    "fields": ["division", "country"]
                }
                """,
                    SequenceFiltersRequestWithFields(
                        emptyMap(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        listOf(
                            AminoAcidInsertion(501, "S", "Y"),
                            AminoAcidInsertion(12, "ORF1", "ABCD"),
                        ),
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
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                    ),
                ),
                Arguments.of(
                    """
                {
                    "accessKey": "some access key"                
                }
                """,
                    SequenceFiltersRequestWithFields(
                        emptyMap(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                    ),
                ),
                Arguments.of(
                    "{}",
                    SequenceFiltersRequestWithFields(
                        emptyMap(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                    ),
                ),
            )

        @JvmStatic
        fun getInvalidRequests() =
            listOf(
                Arguments.of(
                    """
                {
                    "fields": "not an array"
                }
                """,
                    "fields must be an array or null",
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
                Arguments.of(
                    """
                {
                    "nucleotideInsertions": "not an array"
                }
                """,
                    "nucleotideInsertions must be an array or null",
                ),
                Arguments.of(
                    """
                {
                    "aminoAcidInsertions": "not an array"
                }
                """,
                    "aminoAcidInsertions must be an array or null",
                ),
            )
    }
}
