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
class AminoAcidInsertionTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @ParameterizedTest
    @MethodSource("getAminoAcidInsertionWithValidSyntax")
    fun `AminoAcidInsertion is correctly deserialized from JSON`(
        underTest: String,
        expected: AminoAcidInsertion,
    ) {
        val result = objectMapper.readValue<AminoAcidInsertion>(underTest)

        assertThat(result, equalTo(expected))
    }

    @ParameterizedTest
    @MethodSource("getAminoAcidInsertionWithWrongSyntax")
    fun `Given invalid AminoAcidInsertion then should throw an error`(input: String) {
        assertThrows(BadRequestException::class.java) {
            objectMapper.readValue<AminoAcidInsertion>(input)
        }
    }

    companion object {
        @JvmStatic
        fun getAminoAcidInsertionWithValidSyntax() =
            listOf(
                Arguments.of(
                    "\"ins_gene:123:ABCD\"",
                    AminoAcidInsertion(123, "gene", "ABCD"),
                ),
                Arguments.of(
                    "\"ins_gene:123:A\"",
                    AminoAcidInsertion(123, "gene", "A"),
                ),
                Arguments.of(
                    "\"ins_gene:123:AB?CD\"",
                    AminoAcidInsertion(123, "gene", "AB.*CD"),
                ),
                Arguments.of(
                    "\"ins_gene:123:???\"",
                    AminoAcidInsertion(123, "gene", ".*.*.*"),
                ),
                Arguments.of(
                    "\"ins_gene:123:?\"",
                    AminoAcidInsertion(123, "gene", ".*"),
                ),
                Arguments.of(
                    "\"ins_gene:123:.*CD\"",
                    AminoAcidInsertion(123, "gene", ".*CD"),
                ),
                Arguments.of(
                    "\"ins_gene:123:AB.*.*\"",
                    AminoAcidInsertion(123, "gene", "AB.*.*"),
                ),
                Arguments.of(
                    "\"ins_gene:123:?CD\"",
                    AminoAcidInsertion(123, "gene", ".*CD"),
                ),
                Arguments.of(
                    "\"ins_gene:123:AB??\"",
                    AminoAcidInsertion(123, "gene", "AB.*.*"),
                ),
                Arguments.of(
                    "\"ins_gene:123:AB.*?CD\"",
                    AminoAcidInsertion(123, "gene", "AB.*.*CD"),
                ),
                Arguments.of(
                    "\"ins_gene:123:abCd\"",
                    AminoAcidInsertion(123, "gene", "ABCD"),
                ),
            )

        @JvmStatic
        fun getAminoAcidInsertionWithWrongSyntax() =
            listOf(
                Arguments.of("\"ins_::123:G\""),
                Arguments.of("\"ins_:123:\""),
                Arguments.of("\"ins_gene:123:\""),
                Arguments.of("\"ins_gene:gene:123:ABC\""),
                Arguments.of("\"ins_123:ABCD\""),
                Arguments.of("\"ins_gene\$name&with/invalid)chars:123:A\""),
            )
    }
}
