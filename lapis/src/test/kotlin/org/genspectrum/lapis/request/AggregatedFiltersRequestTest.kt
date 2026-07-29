package org.genspectrum.lapis.request

import org.genspectrum.lapis.FIELD_WITH_ONLY_LOWERCASE_LETTERS
import org.genspectrum.lapis.FIELD_WITH_UPPERCASE_LETTER
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
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@SpringBootTest
class AggregatedFiltersRequestTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @ParameterizedTest
    @MethodSource("getTestAggregatedFiltersRequest")
    fun `AggregatedFiltersRequest is correctly deserialized from JSON`(
        input: String,
        expected: AggregatedFiltersRequest,
    ) {
        val result = objectMapper.readValue<AggregatedFiltersRequest>(input)

        assertThat(result, equalTo(expected))
    }

    @ParameterizedTest
    @MethodSource("getSequencePositionFieldTestCases")
    fun `AggregatedFiltersRequest correctly parses sequence position fields`(
        input: String,
        expected: AggregatedFiltersRequest,
    ) {
        val result = objectMapper.readValue<AggregatedFiltersRequest>(input)

        assertThat(result, equalTo(expected))
    }

    @ParameterizedTest
    @MethodSource("getInvalidRequests")
    fun `Given invalid AggregatedFiltersRequest then should throw an error`(
        input: String,
        expectedErrorMessage: String,
    ) {
        val exception = assertThrows(BadRequestException::class.java) {
            objectMapper.readValue<AggregatedFiltersRequest>(input)
        }

        assertThat(exception.message, startsWith(expectedErrorMessage))
    }

    companion object {
        @JvmStatic
        fun getTestAggregatedFiltersRequest() =
            listOf(
                Arguments.of(
                    """
                    {
                        "country": "Switzerland",
                        "fields": ["date", "country"]
                    }
                    """,
                    AggregatedFiltersRequest(
                        mapOf("country" to listOf("Switzerland")),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        listOf(PlainField("date"), PlainField("country")),
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "country": ["Switzerland", "Germany"]
                    }
                    """,
                    AggregatedFiltersRequest(
                        mapOf("country" to listOf("Switzerland", "Germany")),
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
                        "fields": ["${FIELD_WITH_UPPERCASE_LETTER.lowercase()}"]
                    }
                    """,
                    AggregatedFiltersRequest(
                        emptyMap(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        listOf(PlainField(FIELD_WITH_UPPERCASE_LETTER)),
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "fields": ["${FIELD_WITH_ONLY_LOWERCASE_LETTERS.uppercase()}"]
                    }
                    """,
                    AggregatedFiltersRequest(
                        emptyMap(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        listOf(PlainField(FIELD_WITH_ONLY_LOWERCASE_LETTERS)),
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "nucleotideMutations": ["T1-", "A23062T"],
                        "fields": ["date", "country"]
                    }
                    """,
                    AggregatedFiltersRequest(
                        emptyMap(),
                        listOf(NucleotideMutation(null, 1, "-"), NucleotideMutation(null, 23062, "T")),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        listOf(PlainField("date"), PlainField("country")),
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "aminoAcidMutations": ["gene1:501Y", "gene2:12"],
                        "fields": ["date", "country"]
                    }
                    """,
                    AggregatedFiltersRequest(
                        emptyMap(),
                        emptyList(),
                        listOf(AminoAcidMutation("gene1", 501, "Y"), AminoAcidMutation("gene2", 12, null)),
                        emptyList(),
                        emptyList(),
                        listOf(PlainField("date"), PlainField("country")),
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "nucleotideInsertions": ["ins_other_segment:501:Y", "ins_12:ABCD"],
                        "fields": ["date", "country"]
                    }
                    """,
                    AggregatedFiltersRequest(
                        emptyMap(),
                        emptyList(),
                        emptyList(),
                        listOf(
                            NucleotideInsertion(501, "Y", "other_segment"),
                            NucleotideInsertion(12, "ABCD", null),
                        ),
                        emptyList(),
                        listOf(PlainField("date"), PlainField("country")),
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "aminoAcidInsertions": ["ins_gene1:501:Y", "ins_gene2:12:ABCD"],
                        "fields": ["date", "country"]
                    }
                    """,
                    AggregatedFiltersRequest(
                        emptyMap(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        listOf(
                            AminoAcidInsertion(501, "gene1", "Y"),
                            AminoAcidInsertion(12, "gene2", "ABCD"),
                        ),
                        listOf(PlainField("date"), PlainField("country")),
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "country": "Switzerland"
                    }
                    """,
                    AggregatedFiltersRequest(
                        mapOf("country" to listOf("Switzerland")),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                    ),
                ),
                Arguments.of(
                    "{}",
                    AggregatedFiltersRequest(
                        emptyMap(),
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
                        "country": null
                    }
                    """,
                    AggregatedFiltersRequest(
                        mapOf("country" to listOf(null)),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                    ),
                ),
            )

        @JvmStatic
        fun getSequencePositionFieldTestCases() =
            listOf(
                Arguments.of(
                    """{"fields": ["gene1[123]"]}""",
                    AggregatedFiltersRequest(
                        emptyMap(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        listOf(SequencePositionField("gene1", 123)),
                    ),
                ),
                Arguments.of(
                    """{"fields": ["GENE1[1]"]}""",
                    AggregatedFiltersRequest(
                        emptyMap(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        listOf(SequencePositionField("gene1", 1)),
                    ),
                ),
                Arguments.of(
                    """{"fields": ["gene1[7]", "country", "gene2[42]"]}""",
                    AggregatedFiltersRequest(
                        emptyMap(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        listOf(
                            SequencePositionField("gene1", 7),
                            PlainField("country"),
                            SequencePositionField("gene2", 42),
                        ),
                    ),
                ),
                Arguments.of(
                    """{"fields": ["gene1[7]", "GENE1[7]", "country", "country"]}""",
                    AggregatedFiltersRequest(
                        emptyMap(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        listOf(
                            SequencePositionField("gene1", 7),
                            PlainField("country"),
                        ),
                    ),
                ),
            )

        @JvmStatic
        fun getInvalidRequests() =
            listOf(
                Arguments.of(
                    """{"fields": ["[456]"]}""",
                    "Shorthand position syntax '[N]' can only be used for single-segmented genomes",
                ),
                Arguments.of(
                    """{"fields": ["unknownSequence[1]"]}""",
                    "Unknown sequence 'unknownSequence'",
                ),
                Arguments.of(
                    """{"fields": ["gene1[0]"]}""",
                    "Invalid position in 'gene1[0]': must be a positive integer",
                ),
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
