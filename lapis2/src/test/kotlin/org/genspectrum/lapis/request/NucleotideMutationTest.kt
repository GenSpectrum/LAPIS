package org.genspectrum.lapis.request

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class NucleotideMutationTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @ParameterizedTest
    @MethodSource("getMutationWithValidSyntax")
    fun `NucleotideMutation is correctly deserialized from JSON`(
        underTest: String,
        expected: NucleotideMutation,
    ) {
        val result = objectMapper.readValue<NucleotideMutation>(underTest)

        assertThat(result, equalTo(expected))
    }

    @ParameterizedTest
    @MethodSource("getNucleotideMutationWithWrongSyntax")
    fun `Given invalid NucleotideMutation then should throw an error`(input: String) {
        assertThrows(IllegalArgumentException::class.java) {
            objectMapper.readValue<NucleotideMutation>(input)
        }
    }

    companion object {
        @JvmStatic
        fun getMutationWithValidSyntax() = listOf(
            Arguments.of(
                "\"G123A\"",
                NucleotideMutation(null, 123, "A"),
            ),
            Arguments.of(
                "\"123A\"",
                NucleotideMutation(null, 123, "A"),
            ),
            Arguments.of(
                "\"123.\"",
                NucleotideMutation(null, 123, "."),
            ),
            Arguments.of(
                "\"123-\"",
                NucleotideMutation(null, 123, "-"),
            ),
            Arguments.of(
                "\"123\"",
                NucleotideMutation(null, 123, null),
            ),
            Arguments.of(
                "\"A123\"",
                NucleotideMutation(null, 123, null),
            ),
            Arguments.of(
                "\"sequence-Name_1:123X\"",
                NucleotideMutation("sequence-Name_1", 123, "X"),
            ),
        )

        @JvmStatic
        fun getNucleotideMutationWithWrongSyntax() = listOf(
            Arguments.of("\"AA123\""),
            Arguments.of("\"123AA\""),
            Arguments.of("\"\""),
            Arguments.of("\"AA123A\""),
            Arguments.of("\"A\""),
            Arguments.of("\":123A\""),
            Arguments.of("\"sequence\$name&with/invalid)chars:G123A\""),
        )
    }
}
