package org.genspectrum.lapis.silonew

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AdvancedQueryTest {
    private val table = siloSchema {
        table("default") {
            column("country", SiloColumnType.INDEXED_STRING)
            column("date", SiloColumnType.DATE32)
            column("age", SiloColumnType.INT32)
            column("depth", SiloColumnType.INT64)
            column("score", SiloColumnType.FLOAT)
            column("active", SiloColumnType.BOOL)
            column("lineage", SiloColumnType.INDEXED_STRING)
            column("tree", SiloColumnType.STRING)
            column("unaligned_main", SiloColumnType.STRING)
            column("S", SiloColumnType.STRING)
        }
    }.table("default")

    @Test
    fun `GIVEN string metadata and an unqualified mutation THEN parses against a table schema`() {
        val expression = parseAdvancedQuery("country='Germany' AND 23063G", table)

        assertThat(
            expression.render(),
            equalTo("\"country\" = 'Germany' && nucleotideEquals(position:=23063, symbol:='G')"),
        )
    }

    @Test
    fun `GIVEN typed fields and named sequences THEN renders values using schema-derived types`() {
        assertThat(
            parseAdvancedQuery("date>=2026-01-01", table).render(),
            equalTo("\"date\".between('2026-01-01'::date, null)"),
        )
        assertThat(parseAdvancedQuery("age=42", table).render(), equalTo("\"age\" = 42"))
        assertThat(parseAdvancedQuery("depth=1234567890123", table).render(), equalTo("\"depth\" = 1234567890123"))
        assertThat(parseAdvancedQuery("score<=1.5", table).render(), equalTo("\"score\".between(null, 1.5)"))
        assertThat(parseAdvancedQuery("active=true", table).render(), equalTo("\"active\" = true"))
        assertThat(parseAdvancedQuery("lineage=XBB.1*", table).render(), equalTo("\"lineage\" = 'XBB.1*'"))
        assertThat(
            parseAdvancedQuery("main:A23063G OR S:N501Y", table).render(),
            equalTo(
                "nucleotideEquals(position:=23063, symbol:='G', sequenceName:='main') || " +
                    "aminoAcidEquals(position:=501, symbol:='Y', sequenceName:='S')",
            ),
        )
    }

    @Test
    fun `GIVEN maybe n-of and insertion expressions THEN preserves advanced-query semantics`() {
        val expression = parseAdvancedQuery("[exactly-1-of: MAYBE(23063G), ins_S:214:E?E]", table)

        assertThat(
            expression.render(),
            equalTo(
                "nOf(1, {maybe(nucleotideEquals(position:=23063, symbol:='G')), " +
                    "aminoAcidInsertionContains(position:=214, value:='E.*E', sequenceName:='S')}, " +
                    "matchExactly:=true)",
            ),
        )
    }

    @Test
    fun `GIVEN null regex phylogenetic and not expressions THEN maps their SILO functions`() {
        val expression = parseAdvancedQuery(
            "NOT IsNull(country) AND (country.regex='Ger.*' OR tree.PhyloDescendantOf='node\\'1')",
            table,
        )

        assertThat(
            expression.render(),
            equalTo(
                "!(isNull(\"country\")) && " +
                    "(\"country\".like('Ger.*') || \"tree\".phyloDescendantOf('node''1'))",
            ),
        )
    }

    @Test
    fun `GIVEN a quoted hostile value THEN keeps it inside one escaped string literal`() {
        val expression = parseAdvancedQuery("country='x\\'); default.filter(true) --'", table)

        assertThat(expression.render(), equalTo("\"country\" = 'x''); default.filter(true) --'"))
    }

    @Test
    fun `GIVEN syntax outside a quoted value THEN rejects it`() {
        val exception = assertThrows<AdvancedQueryParseException> {
            parseAdvancedQuery("country=Germany);default.filter(true)", table)
        }

        assertThat(exception.message, containsString("Failed to parse advanced query"))
    }

    @Test
    fun `GIVEN an invalid lexer character THEN rejects the complete input`() {
        assertThrows<AdvancedQueryParseException> { parseAdvancedQuery("country=Ger\$many", table) }
    }

    @Test
    fun `GIVEN an unrecognized named sequence THEN defaults to amino-acid semantics`() {
        val expression = parseAdvancedQuery("orf9b:P10S", table)

        assertThat(
            expression.render(),
            equalTo("aminoAcidEquals(position:=10, symbol:='S', sequenceName:='orf9b')"),
        )
    }
}
