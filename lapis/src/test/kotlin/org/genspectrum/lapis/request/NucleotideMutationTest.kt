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

class NucleotideMutationTest {
    @ParameterizedTest
    @MethodSource("getBareMutationsWithValidSyntax")
    fun `GIVEN single segmented THEN bare nucleotide mutation is parsed`(
        underTest: String,
        expected: NucleotideMutation,
    ) {
        assertThat(NucleotideMutation.fromString(underTest, SINGLE_SEGMENTED), equalTo(expected))
    }

    @ParameterizedTest
    @MethodSource("getSegmentedMutationsWithValidSyntax")
    fun `GIVEN multi segmented THEN nucleotide mutation with segment is parsed`(
        underTest: String,
        expected: NucleotideMutation,
    ) {
        assertThat(NucleotideMutation.fromString(underTest, MULTI_SEGMENTED), equalTo(expected))
    }

    @ParameterizedTest
    @MethodSource("getBareMutationsWithValidSyntax")
    fun `GIVEN multi segmented WHEN nucleotide mutation without segment THEN throws`(
        underTest: String,
        @Suppress("UNUSED_PARAMETER") expected: NucleotideMutation,
    ) {
        assertThrows(BadRequestException::class.java) {
            NucleotideMutation.fromString(underTest, MULTI_SEGMENTED)
        }
    }

    @ParameterizedTest
    @MethodSource("getNucleotideMutationWithWrongSyntax")
    fun `Given invalid NucleotideMutation then should throw an error`(input: String) {
        assertThrows(BadRequestException::class.java) {
            NucleotideMutation.fromString(input, MULTI_SEGMENTED)
        }
    }

    companion object {
        private val SINGLE_SEGMENTED = ReferenceGenomeSchema(listOf(ReferenceSequenceSchema("main")), emptyList())
        private val MULTI_SEGMENTED = ReferenceGenomeSchema(
            listOf(ReferenceSequenceSchema("main"), ReferenceSequenceSchema("other_segment")),
            emptyList(),
        )

        @JvmStatic
        fun getBareMutationsWithValidSyntax() =
            listOf(
                Arguments.of("G123A", NucleotideMutation(null, 123, "A")),
                Arguments.of("123A", NucleotideMutation(null, 123, "A")),
                Arguments.of("123.", NucleotideMutation(null, 123, ".")),
                Arguments.of("123-", NucleotideMutation(null, 123, "-")),
                Arguments.of("123", NucleotideMutation(null, 123, null)),
                Arguments.of("A123", NucleotideMutation(null, 123, null)),
                Arguments.of("g123A", NucleotideMutation(null, 123, "A")),
                Arguments.of("G123a", NucleotideMutation(null, 123, "A")),
                Arguments.of("g123a", NucleotideMutation(null, 123, "A")),
                Arguments.of("MAYBE(123X)", NucleotideMutation(null, 123, "X", maybe = true)),
                Arguments.of("maybe(123X)", NucleotideMutation(null, 123, "X", maybe = true)),
                Arguments.of("maYbE(123X)", NucleotideMutation(null, 123, "X", maybe = true)),
            )

        @JvmStatic
        fun getSegmentedMutationsWithValidSyntax() =
            listOf(
                Arguments.of("other_segment:123X", NucleotideMutation("other_segment", 123, "X")),
                Arguments.of("othER_SegmENt:123X", NucleotideMutation("other_segment", 123, "X")),
                Arguments.of(
                    "MAYBE(other_segment:123X)",
                    NucleotideMutation("other_segment", 123, "X", maybe = true),
                ),
            )

        @JvmStatic
        fun getNucleotideMutationWithWrongSyntax() =
            listOf(
                Arguments.of("AA123"),
                Arguments.of("123AA"),
                Arguments.of(""),
                Arguments.of("AA123A"),
                Arguments.of("A"),
                Arguments.of(":123A"),
                Arguments.of("sequence\$name&with/invalid)chars:G123A"),
                Arguments.of("segmentNotInReferenceGenome:G123A"),
                Arguments.of("MAYBE()"),
                Arguments.of("MAYBE(notAMutation)"),
                Arguments.of("MAYBE(123A))"),
                Arguments.of("MAYBE((123A)"),
            )
    }
}
