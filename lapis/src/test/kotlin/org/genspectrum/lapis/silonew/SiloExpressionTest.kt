package org.genspectrum.lapis.silonew

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class SiloExpressionTest {
    @Test
    fun `GIVEN supported literal values THEN renders SaneQL literals`() {
        assertThat(nullLiteral().render(), equalTo("null"))
        assertThat(literal(true).render(), equalTo("true"))
        assertThat(literal(42).render(), equalTo("42"))
        assertThat(literal(Long.MIN_VALUE).render(), equalTo("-9223372036854775808"))
        assertThat(literal(1.5f).render(), equalTo("1.5"))
        assertThat(literal(2.25).render(), equalTo("2.25"))
        assertThat(literal("Switzerland").render(), equalTo("'Switzerland'"))
        assertThat(literal(LocalDate.of(2021, 3, 15)).render(), equalTo("'2021-03-15'::date"))
    }

    @Test
    fun `GIVEN non-finite number THEN rejects an unrenderable SaneQL literal`() {
        assertThrows<IllegalArgumentException> { literal(Double.NaN) }
        assertThrows<IllegalArgumentException> { literal(Double.POSITIVE_INFINITY) }
    }

    @Test
    fun `GIVEN hostile literal and identifier values THEN escapes them as single tokens`() {
        val hostileString = "x'); default.filter(true) --"
        val hostileIdentifier = "x\"}).filter(true).limit(1) --"

        assertThat(
            literal(hostileString).render(),
            equalTo("'x''); default.filter(true) --'"),
        )
        assertThat(
            field(hostileIdentifier).render(),
            equalTo("\"x\"\"}).filter(true).limit(1) --\""),
        )
    }

    @Test
    fun `GIVEN nested boolean expressions THEN preserves precedence`() {
        val a = field("a") eq literal(1)
        val b = field("b") gt literal(2)
        val c = field("c") neq nullLiteral()

        assertThat((a and (b or c)).render(), equalTo("\"a\" = 1 && (\"b\" > 2 || \"c\" <> null)"))
        assertThat(((a and b) or c).render(), equalTo("\"a\" = 1 && \"b\" > 2 || \"c\" <> null"))
        assertThat((!a).render(), equalTo("!(\"a\" = 1)"))
    }

    @Test
    fun `GIVEN comparison operators THEN renders each supported operator`() {
        val left = field("value")
        val right = literal(10)

        assertThat((left eq right).render(), equalTo("\"value\" = 10"))
        assertThat((left neq right).render(), equalTo("\"value\" <> 10"))
        assertThat((left lt right).render(), equalTo("\"value\" < 10"))
        assertThat((left lte right).render(), equalTo("\"value\" <= 10"))
        assertThat((left gt right).render(), equalTo("\"value\" > 10"))
        assertThat((left gte right).render(), equalTo("\"value\" >= 10"))
    }

    @Test
    fun `GIVEN records sets and named arguments THEN renders structured values`() {
        val result = call(
            name = "aggregate",
            positionalArguments = listOf(
                record(assignment("count", count())),
                set(field("country"), literal("CH")),
            ),
            namedArguments = listOf(namedArgument("enabled", literal(true))),
        )

        assertThat(
            result.render(),
            equalTo("aggregate({\"count\":=count()}, {\"country\", 'CH'}, enabled:=true)"),
        )
    }

    @Test
    fun `GIVEN hostile generic callable names THEN renders them as quoted identifiers`() {
        val hostileName = "filter).limit(1"

        assertThat(call(hostileName).render(), equalTo("\"filter).limit(1\"()"))
        assertThat(
            field("country").call(hostileName, listOf(literal("CH"))).render(),
            equalTo("\"country\".\"filter).limit(1\"('CH')"),
        )
    }
}
