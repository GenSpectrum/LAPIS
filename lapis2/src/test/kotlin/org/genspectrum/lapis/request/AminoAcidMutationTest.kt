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
class AminoAcidMutationTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @ParameterizedTest
    @MethodSource("getAminoAcidMutationWithValidSyntax")
    fun `AminoAcidMutation is correctly deserialized from JSON`(
        underTest: String,
        expected: AminoAcidMutation,
    ) {
        val result = objectMapper.readValue<AminoAcidMutation>(underTest)

        assertThat(result, equalTo(expected))
    }

    @ParameterizedTest
    @MethodSource("getAminoAcidMutationWithWrongSyntax")
    fun `Given invalid AminoAcidMutation then should throw an error`(input: String) {
        assertThrows(BadRequestException::class.java) {
            objectMapper.readValue<AminoAcidMutation>(input)
        }
    }

    companion object {
        @JvmStatic
        fun getAminoAcidMutationWithValidSyntax() =
            listOf(
                Arguments.of(
                    "\"gene:G123A\"",
                    AminoAcidMutation("gene", 123, "A"),
                ),
                Arguments.of(
                    "\"gene:123A\"",
                    AminoAcidMutation("gene", 123, "A"),
                ),
                Arguments.of(
                    "\"gene:123.\"",
                    AminoAcidMutation("gene", 123, "."),
                ),
                Arguments.of(
                    "\"gene:123-\"",
                    AminoAcidMutation("gene", 123, "-"),
                ),
                Arguments.of(
                    "\"gene:123\"",
                    AminoAcidMutation("gene", 123, null),
                ),
                Arguments.of(
                    "\"gene:A123\"",
                    AminoAcidMutation("gene", 123, null),
                ),
                Arguments.of(
                    "\"ORF1b:123X\"",
                    AminoAcidMutation("ORF1b", 123, "X"),
                ),
                Arguments.of(
                    "\"gene:123a\"",
                    AminoAcidMutation("gene", 123, "A"),
                ),
            )

        @JvmStatic
        fun getAminoAcidMutationWithWrongSyntax() =
            listOf(
                Arguments.of("\"A123G\""),
                Arguments.of("\"gene:AA123\""),
                Arguments.of("\"gene:123AA\""),
                Arguments.of("\"\""),
                Arguments.of("\"gene:\""),
                Arguments.of("\"gene:AA123A\""),
                Arguments.of("\"gene:A\""),
                Arguments.of("\":123A\""),
                Arguments.of("\"gene\$name&with/invalid)chars:123A\""),
            )
    }
}
