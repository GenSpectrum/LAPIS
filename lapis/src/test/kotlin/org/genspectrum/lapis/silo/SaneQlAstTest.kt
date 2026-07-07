package org.genspectrum.lapis.silo

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SaneQlAstTest {
    @Test
    fun `GIVEN null literal THEN renders as null`() {
        assertThat(SaneQlNull.render(), equalTo("null"))
    }

    @Test
    fun `GIVEN boolean literal THEN renders as true or false`() {
        assertThat(SaneQlBoolean(true).render(), equalTo("true"))
        assertThat(SaneQlBoolean(false).render(), equalTo("false"))
    }

    @Test
    fun `GIVEN int literal THEN renders as plain number`() {
        assertThat(SaneQlInt(42).render(), equalTo("42"))
    }

    @Test
    fun `GIVEN float literal THEN renders as plain number`() {
        assertThat(SaneQlFloat(1.0).render(), equalTo("1.0"))
    }

    @Test
    fun `GIVEN string literal THEN wraps in single quotes`() {
        assertThat(SaneQlString("theValue").render(), equalTo("'theValue'"))
    }

    @Test
    fun `GIVEN string literal with embedded single quote THEN escapes it by doubling`() {
        assertThat(SaneQlString("Côte d'Ivoire").render(), equalTo("'Côte d''Ivoire'"))
    }

    @Test
    fun `GIVEN date literal THEN renders as date cast`() {
        assertThat(SaneQlDate(LocalDate.of(2021, 3, 31)).render(), equalTo("'2021-03-31'::date"))
    }

    @Test
    fun `GIVEN null date literal THEN renders as null`() {
        assertThat(SaneQlDate(null).render(), equalTo("null"))
    }

    @Test
    fun `GIVEN identifier THEN wraps in double quotes`() {
        assertThat(SaneQlIdentifier("theColumn").render(), equalTo("\"theColumn\""))
    }

    @Test
    fun `GIVEN identifier with embedded double quote THEN escapes it by doubling`() {
        assertThat(SaneQlIdentifier("the\"Column").render(), equalTo("\"the\"\"Column\""))
    }

    @Test
    fun `GIVEN list THEN renders as comma-separated items in braces`() {
        val list = SaneQlList(listOf(SaneQlIdentifier("a"), SaneQlIdentifier("b")))
        assertThat(list.render(), equalTo("""{"a", "b"}"""))
    }

    @Test
    fun `GIVEN assignment THEN renders as name colon-equals value`() {
        val assignment = SaneQlAssignment("count", SaneQlFunctionCall("count"))
        assertThat(assignment.render(), equalTo("count:=count()"))
    }

    @Test
    fun `GIVEN equals THEN renders column equals value`() {
        val equals = SaneQlEquals(SaneQlIdentifier("theColumn"), SaneQlString("theValue"))
        assertThat(equals.render(), equalTo(""""theColumn" = 'theValue'"""))
    }

    @Test
    fun `GIVEN not THEN wraps child in parentheses`() {
        val not = SaneQlNot(SaneQlEquals(SaneQlIdentifier("a"), SaneQlInt(1)))
        assertThat(not.render(), equalTo("""!("a" = 1)"""))
    }

    @Test
    fun `GIVEN and with nested and or THEN parenthesizes nested and or children`() {
        val and = SaneQlAnd(
            listOf(
                SaneQlOr(listOf(SaneQlBoolean(true), SaneQlBoolean(false))),
                SaneQlBoolean(true),
            ),
        )
        assertThat(and.render(), equalTo("(true || false) && true"))
    }

    @Test
    fun `GIVEN or with nested and or THEN parenthesizes nested and or children`() {
        val or = SaneQlOr(
            listOf(
                SaneQlAnd(listOf(SaneQlBoolean(true), SaneQlBoolean(false))),
                SaneQlBoolean(true),
            ),
        )
        assertThat(or.render(), equalTo("(true && false) || true"))
    }

    @Test
    fun `GIVEN and with flat non-and-or children THEN does not add parentheses`() {
        val and = SaneQlAnd(listOf(SaneQlBoolean(true), SaneQlBoolean(false)))
        assertThat(and.render(), equalTo("true && false"))
    }

    @Test
    fun `GIVEN function call with positional and named args THEN joins them with commas`() {
        val call = SaneQlFunctionCall(
            "nucleotideEquals",
            positionalArgs = listOf(SaneQlInt(1)),
            namedArgs = listOf(SaneQlNamedArg("symbol", SaneQlString("A"))),
        )
        assertThat(call.render(), equalTo("nucleotideEquals(1, symbol:='A')"))
    }

    @Test
    fun `GIVEN function call without args THEN renders empty parentheses`() {
        assertThat(SaneQlFunctionCall("count").render(), equalTo("count()"))
    }

    @Test
    fun `GIVEN method call THEN renders receiver dot name of args`() {
        val call = SaneQlMethodCall(
            receiver = SaneQlIdentifier("fieldName"),
            name = "between",
            positionalArgs = listOf(SaneQlInt(18), SaneQlInt(65)),
        )
        assertThat(call.render(), equalTo(""""fieldName".between(18, 65)"""))
    }

    @Test
    fun `GIVEN method call with positional and named args THEN joins them with commas`() {
        val call = SaneQlMethodCall(
            receiver = SaneQlIdentifier("fieldName"),
            name = "lineage",
            positionalArgs = listOf(SaneQlString("ABC")),
            namedArgs = listOf(SaneQlNamedArg("includeSublineages", SaneQlBoolean(true))),
        )
        assertThat(call.render(), equalTo(""""fieldName".lineage('ABC', includeSublineages:=true)"""))
    }

    @Test
    fun `GIVEN step with no args THEN renders empty parentheses`() {
        assertThat(SaneQlStep("insertions").render(), equalTo(".insertions()"))
    }

    @Test
    fun `GIVEN step with named args THEN renders them`() {
        val step = SaneQlStep("randomize", namedArgs = listOf(SaneQlNamedArg("seed", SaneQlInt(42))))
        assertThat(step.render(), equalTo(".randomize(seed:=42)"))
    }

    @Test
    fun `GIVEN pipeline with no steps THEN renders only the filter`() {
        val pipeline = SaneQlPipeline(SaneQlBoolean(true), emptyList())
        assertThat(pipeline.render(), equalTo("default.filter(true)"))
    }

    @Test
    fun `GIVEN pipeline with steps THEN chains them after the filter`() {
        val pipeline = SaneQlPipeline(
            SaneQlBoolean(true),
            listOf(SaneQlStep("insertions"), SaneQlStep("limit", positionalArgs = listOf(SaneQlInt(10)))),
        )
        assertThat(pipeline.render(), equalTo("default.filter(true).insertions().limit(10)"))
    }
}
