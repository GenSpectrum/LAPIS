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
                    "\"gene1:G123A\"",
                    AminoAcidMutation("gene1", 123, "A"),
                ),
                Arguments.of(
                    "\"gene1:123A\"",
                    AminoAcidMutation("gene1", 123, "A"),
                ),
                Arguments.of(
                    "\"gene1:123.\"",
                    AminoAcidMutation("gene1", 123, "."),
                ),
                Arguments.of(
                    "\"gene1:123-\"",
                    AminoAcidMutation("gene1", 123, "-"),
                ),
                Arguments.of(
                    "\"gene1:123\"",
                    AminoAcidMutation("gene1", 123, null),
                ),
                Arguments.of(
                    "\"gene1:A123\"",
                    AminoAcidMutation("gene1", 123, null),
                ),
                Arguments.of(
                    "\"gene1:123X\"",
                    AminoAcidMutation("gene1", 123, "X"),
                ),
                Arguments.of(
                    "\"gene1:123a\"",
                    AminoAcidMutation("gene1", 123, "A"),
                ),
                Arguments.of(
                    "\"gene1:123*\"",
                    AminoAcidMutation("gene1", 123, "*"),
                ),
                Arguments.of(
                    "\"gENe1:123A\"",
                    AminoAcidMutation("gene1", 123, "A"),
                ),
                Arguments.of(
                    "\"MAYBE(gene1:123A)\"",
                    AminoAcidMutation("gene1", 123, "A", maybe = true),
                ),
                Arguments.of(
                    "\"MAYBE(gene1:G123A)\"",
                    AminoAcidMutation("gene1", 123, "A", maybe = true),
                ),
                Arguments.of(
                    "\"maybe(gene1:G123A)\"",
                    AminoAcidMutation("gene1", 123, "A", maybe = true),
                ),
                Arguments.of(
                    "\"MayBe(gene1:G123A)\"",
                    AminoAcidMutation("gene1", 123, "A", maybe = true),
                ),
                Arguments.of(
                    "\"gene1:*123A\"",
                    AminoAcidMutation("gene1", 123, "A", maybe = false),
                ),
            )

        @JvmStatic
        fun getAminoAcidMutationWithWrongSyntax() =
            listOf(
                Arguments.of("\"A123G\""),
                Arguments.of("\"gene1:AA123\""),
                Arguments.of("\"gene1:123AA\""),
                Arguments.of("\"\""),
                Arguments.of("\"gene:\""),
                Arguments.of("\"gene:AA123A\""),
                Arguments.of("\"gene:A\""),
                Arguments.of("\":123A\""),
                Arguments.of("\"gene1\$name&with/invalid)chars:123A\""),
                Arguments.of("\"geneNotInReferenceGenome:123A\""),
                Arguments.of("\"MAYBE()\""),
                Arguments.of("\"MAYBE(notAMutation)\""),
                Arguments.of("\"MAYBE(gene1:G123A))\""),
                Arguments.of("\"MAYBE((gene1:G123A)\""),
            )
    }
}
