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
class NucleotideInsertionTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @ParameterizedTest
    @MethodSource("getNucleotideInsertionWithValidSyntax")
    fun `NucleotideInsertion is correctly deserialized from JSON`(
        underTest: String,
        expected: NucleotideInsertion,
    ) {
        val result = objectMapper.readValue<NucleotideInsertion>(underTest)

        assertThat(result, equalTo(expected))
    }

    @ParameterizedTest
    @MethodSource("getNucleotideInsertionWithWrongSyntax")
    fun `Given invalid NucleotideInsertion then should throw an error`(input: String) {
        assertThrows(BadRequestException::class.java) {
            objectMapper.readValue<NucleotideInsertion>(input)
        }
    }

    companion object {
        @JvmStatic
        fun getNucleotideInsertionWithValidSyntax() =
            listOf(
                Arguments.of(
                    "\"ins_other_segment:123:ABCD\"",
                    NucleotideInsertion(123, "ABCD", "other_segment"),
                ),
                Arguments.of(
                    "\"ins_other_segment:123:A\"",
                    NucleotideInsertion(123, "A", "other_segment"),
                ),
                Arguments.of(
                    "\"ins_123:ABCD\"",
                    NucleotideInsertion(123, "ABCD", null),
                ),
                Arguments.of(
                    "\"ins_123:AB?CD\"",
                    NucleotideInsertion(123, "AB.*CD", null),
                ),
                Arguments.of(
                    "\"ins_123:???\"",
                    NucleotideInsertion(123, ".*.*.*", null),
                ),
                Arguments.of(
                    "\"ins_other_segment:123:?\"",
                    NucleotideInsertion(123, ".*", "other_segment"),
                ),
                Arguments.of(
                    "\"ins_other_segment:123:AB.*CD\"",
                    NucleotideInsertion(123, "AB.*CD", "other_segment"),
                ),
                Arguments.of(
                    "\"ins_other_segment:123:.*CD\"",
                    NucleotideInsertion(123, ".*CD", "other_segment"),
                ),
                Arguments.of(
                    "\"ins_other_segment:123:AB.*.*\"",
                    NucleotideInsertion(123, "AB.*.*", "other_segment"),
                ),
                Arguments.of(
                    "\"ins_other_segment:123:?CD\"",
                    NucleotideInsertion(123, ".*CD", "other_segment"),
                ),
                Arguments.of(
                    "\"ins_other_segment:123:AB??\"",
                    NucleotideInsertion(123, "AB.*.*", "other_segment"),
                ),
                Arguments.of(
                    "\"ins_other_segment:123:AB.*?CD\"",
                    NucleotideInsertion(123, "AB.*.*CD", "other_segment"),
                ),
                Arguments.of(
                    "\"ins_other_segment:123:abCd\"",
                    NucleotideInsertion(123, "ABCD", "other_segment"),
                ),
                Arguments.of(
                    "\"ins_oTher_segmenT:123:ABCD\"",
                    NucleotideInsertion(123, "ABCD", "other_segment"),
                ),
            )

        @JvmStatic
        fun getNucleotideInsertionWithWrongSyntax() =
            listOf(
                Arguments.of("\"ins_::123:G\""),
                Arguments.of("\"ins_:123:\""),
                Arguments.of("\"ins_other_segment:123:\""),
                Arguments.of("\"ins_other_segment:other_segment:123:ABC\""),
                Arguments.of("\"ins_other_segmentWithDotWithoutStar:123:AB.C\""),
                Arguments.of("\"ins_segment\$name&with/invalid)chars:123:A\""),
                Arguments.of(
                    "\"ins_segmentNotInReference:123:ABCD\"",
                ),
            )
    }
}
