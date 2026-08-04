package org.genspectrum.lapis.request

import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.config.ReferenceSequenceSchema
import org.genspectrum.lapis.controller.BadRequestException
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class NucleotideInsertionTest {
    @ParameterizedTest
    @MethodSource("getBareInsertionsWithValidSyntax")
    fun `GIVEN single segmented THEN bare nucleotide insertion is parsed`(
        underTest: String,
        expected: NucleotideInsertion,
    ) {
        assertThat(NucleotideInsertion.fromString(underTest, SINGLE_SEGMENTED), equalTo(expected))
    }

    @ParameterizedTest
    @MethodSource("getSegmentedInsertionsWithValidSyntax")
    fun `GIVEN multi segmented THEN nucleotide insertion with segment is parsed`(
        underTest: String,
        expected: NucleotideInsertion,
    ) {
        assertThat(NucleotideInsertion.fromString(underTest, MULTI_SEGMENTED), equalTo(expected))
    }

    @ParameterizedTest
    @MethodSource("getBareInsertionsWithValidSyntax")
    fun `GIVEN multi segmented WHEN nucleotide insertion without segment THEN throws`(
        underTest: String,
        @Suppress("UNUSED_PARAMETER") expected: NucleotideInsertion,
    ) {
        assertThrows(BadRequestException::class.java) {
            NucleotideInsertion.fromString(underTest, MULTI_SEGMENTED)
        }
    }

    @ParameterizedTest
    @MethodSource("getNucleotideInsertionWithWrongSyntax")
    fun `Given invalid NucleotideInsertion then should throw an error`(input: String) {
        assertThrows(BadRequestException::class.java) {
            NucleotideInsertion.fromString(input, MULTI_SEGMENTED)
        }
    }

    companion object {
        private val SINGLE_SEGMENTED = ReferenceGenomeSchema(listOf(ReferenceSequenceSchema("main")), emptyList())
        private val MULTI_SEGMENTED = ReferenceGenomeSchema(
            listOf(ReferenceSequenceSchema("main"), ReferenceSequenceSchema("other_segment")),
            emptyList(),
        )

        @JvmStatic
        fun getBareInsertionsWithValidSyntax() =
            listOf(
                Arguments.of("ins_123:ABCD", NucleotideInsertion(123, "ABCD", null)),
                Arguments.of("ins_123:AB?CD", NucleotideInsertion(123, "AB.*CD", null)),
                Arguments.of("ins_123:???", NucleotideInsertion(123, ".*.*.*", null)),
                Arguments.of("INs_123:AcCD", NucleotideInsertion(123, "ACCD", null)),
            )

        @JvmStatic
        fun getSegmentedInsertionsWithValidSyntax() =
            listOf(
                Arguments.of("ins_other_segment:123:ABCD", NucleotideInsertion(123, "ABCD", "other_segment")),
                Arguments.of("ins_other_segment:123:A", NucleotideInsertion(123, "A", "other_segment")),
                Arguments.of("ins_other_segment:123:?", NucleotideInsertion(123, ".*", "other_segment")),
                Arguments.of("ins_other_segment:123:AB.*CD", NucleotideInsertion(123, "AB.*CD", "other_segment")),
                Arguments.of("ins_other_segment:123:.*CD", NucleotideInsertion(123, ".*CD", "other_segment")),
                Arguments.of("ins_other_segment:123:AB.*.*", NucleotideInsertion(123, "AB.*.*", "other_segment")),
                Arguments.of("ins_other_segment:123:?CD", NucleotideInsertion(123, ".*CD", "other_segment")),
                Arguments.of("ins_other_segment:123:AB??", NucleotideInsertion(123, "AB.*.*", "other_segment")),
                Arguments.of("ins_other_segment:123:AB.*?CD", NucleotideInsertion(123, "AB.*.*CD", "other_segment")),
                Arguments.of("ins_other_segment:123:abCd", NucleotideInsertion(123, "ABCD", "other_segment")),
                Arguments.of("ins_oTher_segmenT:123:ABCD", NucleotideInsertion(123, "ABCD", "other_segment")),
            )

        @JvmStatic
        fun getNucleotideInsertionWithWrongSyntax() =
            listOf(
                Arguments.of("ins_::123:G"),
                Arguments.of("ins_:123:"),
                Arguments.of("ins_other_segment:123:"),
                Arguments.of("ins_other_segment:other_segment:123:ABC"),
                Arguments.of("ins_other_segmentWithDotWithoutStar:123:AB.C"),
                Arguments.of("ins_segment\$name&with/invalid)chars:123:A"),
                Arguments.of("ins_segmentNotInReference:123:ABCD"),
            )
    }
}
