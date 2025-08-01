package org.genspectrum.lapis.request

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.genspectrum.lapis.controller.BadRequestException
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.startsWith
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
        val exception = assertThrows(BadRequestException::class.java) {
            objectMapper.readValue<MutationProportionsRequest>(input)
        }

        assertThat(exception.message, startsWith(expectedErrorMessage))
    }

    companion object {
        @JvmStatic
        fun getTestMutationProportionsRequests() =
            listOf(
                Arguments.of(
                    """
                    {
                        "country": "Switzerland"
                    }
                    """,
                    MutationProportionsRequest(
                        sequenceFilters = mapOf("country" to listOf("Switzerland")),
                        nucleotideMutations = emptyList(),
                        aminoAcidMutations = emptyList(),
                        nucleotideInsertions = emptyList(),
                        aminoAcidInsertions = emptyList(),
                        fields = emptyList(),
                        minProportion = DEFAULT_MIN_PROPORTION,
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "country": ["Switzerland", "Germany"]
                    }
                    """,
                    MutationProportionsRequest(
                        sequenceFilters = mapOf("country" to listOf("Switzerland", "Germany")),
                        nucleotideMutations = emptyList(),
                        aminoAcidMutations = emptyList(),
                        nucleotideInsertions = emptyList(),
                        aminoAcidInsertions = emptyList(),
                        fields = emptyList(),
                        minProportion = DEFAULT_MIN_PROPORTION,
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "nucleotideMutations": ["T1-", "A23062T"]
                    }
                    """,
                    MutationProportionsRequest(
                        sequenceFilters = emptyMap(),
                        nucleotideMutations = listOf(
                            NucleotideMutation(null, 1, "-"),
                            NucleotideMutation(null, 23062, "T"),
                        ),
                        aminoAcidMutations = emptyList(),
                        nucleotideInsertions = emptyList(),
                        aminoAcidInsertions = emptyList(),
                        fields = emptyList(),
                        minProportion = DEFAULT_MIN_PROPORTION,
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "aminoAcidMutations": ["gene1:501Y", "gene2:12"]
                    }
                    """,
                    MutationProportionsRequest(
                        sequenceFilters = emptyMap(),
                        nucleotideMutations = emptyList(),
                        aminoAcidMutations = listOf(
                            AminoAcidMutation("gene1", 501, "Y"),
                            AminoAcidMutation("gene2", 12, null),
                        ),
                        nucleotideInsertions = emptyList(),
                        aminoAcidInsertions = emptyList(),
                        fields = emptyList(),
                        minProportion = DEFAULT_MIN_PROPORTION,
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "nucleotideInsertions": ["ins_other_segment:501:Y", "ins_12:ABCD"]
                    }
                    """,
                    MutationProportionsRequest(
                        sequenceFilters = emptyMap(),
                        nucleotideMutations = emptyList(),
                        aminoAcidMutations = emptyList(),
                        nucleotideInsertions = listOf(
                            NucleotideInsertion(501, "Y", "other_segment"),
                            NucleotideInsertion(12, "ABCD", null),
                        ),
                        aminoAcidInsertions = emptyList(),
                        fields = emptyList(),
                        minProportion = DEFAULT_MIN_PROPORTION,
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "aminoAcidInsertions": ["ins_gene1:501:Y", "ins_gene2:12:ABCD"]
                    }
                    """,
                    MutationProportionsRequest(
                        sequenceFilters = emptyMap(),
                        nucleotideMutations = emptyList(),
                        aminoAcidMutations = emptyList(),
                        nucleotideInsertions = emptyList(),
                        aminoAcidInsertions = listOf(
                            AminoAcidInsertion(501, "gene1", "Y"),
                            AminoAcidInsertion(12, "gene2", "ABCD"),
                        ),
                        fields = emptyList(),
                        minProportion = DEFAULT_MIN_PROPORTION,
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "minProportion": 0.7
                    }
                    """,
                    MutationProportionsRequest(
                        sequenceFilters = emptyMap(),
                        nucleotideMutations = emptyList(),
                        aminoAcidMutations = emptyList(),
                        nucleotideInsertions = emptyList(),
                        aminoAcidInsertions = emptyList(),
                        fields = emptyList(),
                        minProportion = 0.7,
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "accessKey": "some access key"                
                    }
                    """,
                    MutationProportionsRequest(
                        sequenceFilters = emptyMap(),
                        nucleotideMutations = emptyList(),
                        aminoAcidMutations = emptyList(),
                        nucleotideInsertions = emptyList(),
                        aminoAcidInsertions = emptyList(),
                        fields = emptyList(),
                        minProportion = DEFAULT_MIN_PROPORTION,
                    ),
                ),
                Arguments.of(
                    "{}",
                    MutationProportionsRequest(
                        sequenceFilters = emptyMap(),
                        nucleotideMutations = emptyList(),
                        aminoAcidMutations = emptyList(),
                        nucleotideInsertions = emptyList(),
                        aminoAcidInsertions = emptyList(),
                        fields = emptyList(),
                        minProportion = DEFAULT_MIN_PROPORTION,
                    ),
                ),
            )

        @JvmStatic
        fun getInvalidRequests() =
            listOf(
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
