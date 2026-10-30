package org.genspectrum.lapis.rhydb

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class RhyDbQueryDslTest {
    @Test
    fun `GIVEN receiver only THEN renders bare receiver`() {
        assertThat(pipeline(id("data")).render(), equalTo("\"data\""))
    }

    @Test
    fun `GIVEN filter THEN renders filter step`() {
        assertThat(pipeline(id("data")).filter(SaneQlBoolean(true)).render(), equalTo("\"data\".filter(true)"))
    }

    @Test
    fun `GIVEN group THEN renders by and aggs`() {
        val result = pipeline(id("data"))
            .group(by = listOf(id("country")), aggs = listOf("count" to count()))
            .render()

        assertThat(result, equalTo("\"data\".group(by:={\"country\"}, aggs:={\"count\":=count()})"))
    }

    @Test
    fun `GIVEN empty group by THEN renders empty record`() {
        val result = pipeline(id("data"))
            .group(by = emptyList(), aggs = listOf("count" to count()))
            .render()

        assertThat(result, equalTo("\"data\".group(by:={}, aggs:={\"count\":=count()})"))
    }

    @Test
    fun `GIVEN map THEN renders record`() {
        val result = pipeline(id("data"))
            .map(listOf("a" to id("x"), "b" to id("y")))
            .render()

        assertThat(result, equalTo("\"data\".map({\"a\":=\"x\", \"b\":=\"y\"})"))
    }

    @Test
    fun `GIVEN project THEN renders column list`() {
        val result = pipeline(id("data")).project(listOf(id("a"), id("b"))).render()

        assertThat(result, equalTo("\"data\".project({\"a\", \"b\"})"))
    }

    @Test
    fun `GIVEN join without type THEN renders relation and on`() {
        val result = pipeline(id("data"))
            .join(relation = id("other"), on = SaneQlEquals(id("a"), id("b")))
            .render()

        assertThat(result, equalTo("\"data\".join(\"other\", \"a\" = \"b\")"))
    }

    @Test
    fun `GIVEN left join THEN renders type`() {
        val result = pipeline(id("data"))
            .join(relation = id("other"), on = SaneQlEquals(id("a"), id("b")), type = JoinType.LEFT)
            .render()

        assertThat(result, equalTo("\"data\".join(\"other\", \"a\" = \"b\", type:=left)"))
    }

    @Test
    fun `GIVEN transitiveClosure THEN renders bare column keywords and includeVertices`() {
        val result = pipeline(id("tree"))
            .transitiveClosure(parent = "parent", child = "lineage", includeVertices = true)
            .render()

        assertThat(result, equalTo("\"tree\".transitiveClosure(parent, lineage, includeVertices:=true)"))
    }

    @Test
    fun `GIVEN order offset randomize limit THEN renders each suffix step`() {
        val result = pipeline(id("data"))
            .order(by = listOf(id("date")))
            .offset(5)
            .randomize()
            .limit(10)
            .render()

        assertThat(result, equalTo("\"data\".order(by:={\"date\"}).offset(5).randomize().limit(10)"))
    }

    @Test
    fun `GIVEN randomize with seed THEN renders seed`() {
        assertThat(pipeline(id("data")).randomize(seed = 42).render(), equalTo("\"data\".randomize(seed:=42)"))
    }

    @Test
    fun `GIVEN step escape hatch THEN renders raw step`() {
        val result = pipeline(id("data"))
            .step("mutations", namedArgs = listOf(SaneQlNamedArg("minProportion", SaneQlFloat(0.5))))
            .render()

        assertThat(result, equalTo("\"data\".mutations(minProportion:=0.5)"))
    }

    @Test
    fun `GIVEN nested pipeline receiver THEN renders composed relation`() {
        val inner = pipeline(id("pangoLineage")).filter(SaneQlBoolean(true))
        val result = pipeline(inner).project(listOf(id("a"))).render()

        assertThat(result, equalTo("\"pangoLineage\".filter(true).project({\"a\"})"))
    }

    @Test
    fun `GIVEN eq THEN renders equality`() {
        assertThat((id("a") eq id("b")).render(), equalTo("\"a\" = \"b\""))
    }

    @Test
    fun `GIVEN concat THEN renders concat with suffix`() {
        assertThat(concat(id("from"), "*").render(), equalTo("concat(\"from\", '*')"))
    }

    @Test
    fun `GIVEN unionAll THEN renders all relations`() {
        assertThat(unionAll(id("a"), id("b")).render(), equalTo("unionall(\"a\", \"b\")"))
    }
}
