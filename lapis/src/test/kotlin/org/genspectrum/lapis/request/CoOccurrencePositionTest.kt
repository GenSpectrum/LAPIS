package org.genspectrum.lapis.request

import org.genspectrum.lapis.controller.BadRequestException
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tools.jackson.databind.ObjectMapper

class CoOccurrencePositionTest {
    @Test
    fun `GIVEN a plain number string THEN parses to a Single position`() {
        val result = parsePositionToken("5")

        assertThat(result, equalTo(CoOccurrencePosition.Single(5)))
    }

    @Test
    fun `GIVEN a range string THEN throws BadRequestException`() {
        val exception = assertThrows<BadRequestException> { parsePositionToken("100-110") }

        assertThat(
            exception.message,
            equalTo("Invalid entry '100-110' in positions: must be a number (e.g. '5')"),
        )
    }

    @Test
    fun `GIVEN an invalid token THEN throws BadRequestException`() {
        val exception = assertThrows<BadRequestException> { parsePositionToken("abc") }

        assertThat(
            exception.message,
            equalTo("Invalid entry 'abc' in positions: must be a number (e.g. '5')"),
        )
    }

    @Test
    fun `GIVEN a token that overflows Int THEN throws BadRequestException instead of crashing`() {
        val exception = assertThrows<BadRequestException> { parsePositionToken("99999999999") }

        assertThat(
            exception.message,
            equalTo("Invalid entry '99999999999' in positions: must be a number (e.g. '5')"),
        )
    }

    @ParameterizedTest
    @MethodSource("getExpandTestCases")
    fun `expandAndValidatePositions expands and sorts positions`(
        input: List<CoOccurrencePosition>,
        expected: List<Int>,
    ) {
        assertThat(input.expandAndValidatePositions(), equalTo(expected))
    }

    @Test
    fun `GIVEN empty positions THEN throws BadRequestException`() {
        val exception =
            assertThrows<BadRequestException> { emptyList<CoOccurrencePosition>().expandAndValidatePositions() }

        assertThat(exception.message, equalTo("positions must not be empty"))
    }

    @Test
    fun `GIVEN a single position of 0 THEN throws BadRequestException`() {
        val exception = assertThrows<BadRequestException> {
            listOf(CoOccurrencePosition.Single(0)).expandAndValidatePositions()
        }

        assertThat(exception.message, equalTo("Invalid position 0 in positions: must be >= 1"))
    }

    @Test
    fun `GIVEN a range with from greater than to THEN throws BadRequestException`() {
        val exception = assertThrows<BadRequestException> {
            listOf(CoOccurrencePosition.Range(10, 5)).expandAndValidatePositions()
        }

        assertThat(exception.message, equalTo("Invalid range 10-5 in positions: 'from' must be <= 'to'"))
    }

    @Test
    fun `GIVEN a range with from less than 1 THEN throws BadRequestException`() {
        val exception = assertThrows<BadRequestException> {
            listOf(CoOccurrencePosition.Range(0, 5)).expandAndValidatePositions()
        }

        assertThat(exception.message, equalTo("Invalid range 0-5 in positions: bounds must be >= 1"))
    }

    @Test
    fun `GIVEN a huge range THEN throws BadRequestException instead of exhausting memory`() {
        val exception = assertThrows<BadRequestException> {
            listOf(CoOccurrencePosition.Range(1, 2_000_000_000)).expandAndValidatePositions()
        }

        assertThat(
            exception.message,
            equalTo(
                "Invalid range 1-2000000000 in positions: spans 2000000000 positions, which exceeds " +
                    "the maximum of $MAX_CO_OCCURRENCE_POSITIONS positions per request",
            ),
        )
    }

    @Test
    fun `GIVEN a range at exactly the maximum THEN does not throw`() {
        val result = listOf(
            CoOccurrencePosition.Range(1, MAX_CO_OCCURRENCE_POSITIONS),
        ).expandAndValidatePositions()

        assertThat(result.size, equalTo(MAX_CO_OCCURRENCE_POSITIONS))
    }

    @Test
    fun `GIVEN many single positions exceeding the maximum THEN throws BadRequestException`() {
        val positions = (1..(MAX_CO_OCCURRENCE_POSITIONS + 1)).map { CoOccurrencePosition.Single(it) }

        val exception = assertThrows<BadRequestException> { positions.expandAndValidatePositions() }

        assertThat(
            exception.message,
            equalTo("positions must not expand to more than $MAX_CO_OCCURRENCE_POSITIONS distinct positions"),
        )
    }

    companion object {
        @JvmStatic
        fun getExpandTestCases() =
            listOf(
                Arguments.of(listOf(CoOccurrencePosition.Single(5)), listOf(5)),
                Arguments.of(
                    listOf(CoOccurrencePosition.Single(5), CoOccurrencePosition.Single(1)),
                    listOf(1, 5),
                ),
                Arguments.of(
                    listOf(CoOccurrencePosition.Range(100, 103)),
                    listOf(100, 101, 102, 103),
                ),
                Arguments.of(
                    listOf(
                        CoOccurrencePosition.Single(1),
                        CoOccurrencePosition.Range(1, 3),
                        CoOccurrencePosition.Single(2),
                    ),
                    listOf(1, 2, 3),
                ),
            )
    }
}

@SpringBootTest
class CoOccurrencePositionDeserializerTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `GIVEN a JSON number THEN deserializes to Single`() {
        val result = objectMapper.readValue("5", CoOccurrencePosition::class.java)

        assertThat(result, equalTo(CoOccurrencePosition.Single(5)))
    }

    @Test
    fun `GIVEN a JSON string number THEN deserializes to Single`() {
        val result = objectMapper.readValue(""""5"""", CoOccurrencePosition::class.java)

        assertThat(result, equalTo(CoOccurrencePosition.Single(5)))
    }

    @Test
    fun `GIVEN a JSON string range THEN throws BadRequestException`() {
        val exception = assertThrows<BadRequestException> {
            objectMapper.readValue(""""100-110"""", CoOccurrencePosition::class.java)
        }

        assertThat(
            exception.message,
            equalTo("Invalid entry '100-110' in positions: must be a number (e.g. '5')"),
        )
    }

    @Test
    fun `GIVEN a JSON object with from and to THEN deserializes to Range`() {
        val result = objectMapper.readValue("""{"from": 100, "to": 110}""", CoOccurrencePosition::class.java)

        assertThat(result, equalTo(CoOccurrencePosition.Range(100, 110)))
    }

    @Test
    fun `GIVEN a JSON object with a from that overflows Int THEN throws BadRequestException`() {
        val exception = assertThrows<BadRequestException> {
            objectMapper.readValue("""{"from": 99999999999, "to": 110}""", CoOccurrencePosition::class.java)
        }

        assertThat(
            exception.message,
            equalTo(
                "Each object entry in positions must have integer 'from' and 'to' properties " +
                    "that fit in a 32-bit integer, but was {\"from\":99999999999,\"to\":110}",
            ),
        )
    }

    @Test
    fun `GIVEN a boolean THEN throws BadRequestException`() {
        val exception = assertThrows<BadRequestException> {
            objectMapper.readValue("true", CoOccurrencePosition::class.java)
        }

        assertThat(
            exception.message,
            equalTo(
                "Each entry in positions must be a number, a string, or an object with " +
                    "'from'/'to', but was true (BOOLEAN)",
            ),
        )
    }
}
