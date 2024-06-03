package org.genspectrum.lapis.request

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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

        assertThat(exception.message, startsWith(expectedErrorMessage))
    }

    companion object {
        @JvmStatic
        fun getTestSequenceFiltersRequestWithFields() =
            listOf(
                Arguments.of(
                    """
                    {
                        "country": "Switzerland",
                        "fields": ["date", "country"]
                    }
                    """,
                    SequenceFiltersRequestWithFields(
                        mapOf("country" to listOf("Switzerland")),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        listOf(Field("date"), Field("country")),
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "country": ["Switzerland", "Germany"]
                    }
                    """,
                    SequenceFiltersRequestWithFields(
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
                    SequenceFiltersRequestWithFields(
                        emptyMap(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        listOf(Field(FIELD_WITH_UPPERCASE_LETTER)),
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "fields": ["${FIELD_WITH_ONLY_LOWERCASE_LETTERS.uppercase()}"]
                    }
                    """,
                    SequenceFiltersRequestWithFields(
                        emptyMap(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        listOf(Field(FIELD_WITH_ONLY_LOWERCASE_LETTERS)),
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "nucleotideMutations": ["T1-", "A23062T"],
                        "fields": ["date", "country"]
                    }
                    """,
                    SequenceFiltersRequestWithFields(
                        emptyMap(),
                        listOf(NucleotideMutation(null, 1, "-"), NucleotideMutation(null, 23062, "T")),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        listOf(Field("date"), Field("country")),
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "aminoAcidMutations": ["gene1:501Y", "gene2:12"],
                        "fields": ["date", "country"]
                    }
                    """,
                    SequenceFiltersRequestWithFields(
                        emptyMap(),
                        emptyList(),
                        listOf(AminoAcidMutation("gene1", 501, "Y"), AminoAcidMutation("gene2", 12, null)),
                        emptyList(),
                        emptyList(),
                        listOf(Field("date"), Field("country")),
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "nucleotideInsertions": ["ins_other_segment:501:Y", "ins_12:ABCD"],
                        "fields": ["date", "country"]
                    }
                    """,
                    SequenceFiltersRequestWithFields(
                        emptyMap(),
                        emptyList(),
                        emptyList(),
                        listOf(
                            NucleotideInsertion(501, "Y", "other_segment"),
                            NucleotideInsertion(12, "ABCD", null),
                        ),
                        emptyList(),
                        listOf(Field("date"), Field("country")),
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "aminoAcidInsertions": ["ins_gene1:501:Y", "ins_gene2:12:ABCD"],
                        "fields": ["date", "country"]
                    }
                    """,
                    SequenceFiltersRequestWithFields(
                        emptyMap(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        listOf(
                            AminoAcidInsertion(501, "gene1", "Y"),
                            AminoAcidInsertion(12, "gene2", "ABCD"),
                        ),
                        listOf(Field("date"), Field("country")),
                    ),
                ),
                Arguments.of(
                    """
                    {
                        "country": "Switzerland"
                    }
                    """,
                    SequenceFiltersRequestWithFields(
                        mapOf("country" to listOf("Switzerland")),
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
                Arguments.of(
                    """
                    {
                        "country": null
                    }
                    """,
                    SequenceFiltersRequestWithFields(
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
