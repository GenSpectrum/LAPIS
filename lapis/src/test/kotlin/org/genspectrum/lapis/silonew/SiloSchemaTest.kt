package org.genspectrum.lapis.silonew

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasProperty
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SiloSchemaTest {
    @Test
    fun `GIVEN multiple declared tables THEN exposes their columns in declaration order`() {
        val schema = siloSchema {
            table("sequences") {
                column("country", SiloColumnType.INDEXED_STRING)
                column("date", SiloColumnType.DATE32)
            }
            table("samples") {
                column("sample_id", SiloColumnType.STRING)
            }
        }

        assertThat(
            schema.tables,
            contains(hasProperty("name", equalTo("sequences")), hasProperty("name", equalTo("samples"))),
        )
        assertThat(schema["sequences"].columns.map(SiloColumn::name), equalTo(listOf("country", "date")))
        assertThat(
            schema["sequences"].columns.map(SiloColumn::type),
            equalTo(listOf(SiloColumnType.INDEXED_STRING, SiloColumnType.DATE32)),
        )
        assertThat(schema["samples"]["sample_id"].render(), equalTo("\"sample_id\""))
    }

    @Test
    fun `GIVEN an unknown table THEN lookup fails locally`() {
        val schema = siloSchema {
            table("default")
        }

        val exception = assertThrows<IllegalArgumentException> { schema.table("missing") }

        assertThat(exception.message, equalTo("Table 'missing' is not declared in this SILO schema"))
    }

    @Test
    fun `GIVEN an unknown source column THEN lookup fails locally`() {
        val schema = siloSchema {
            table("default") {
                column("country", SiloColumnType.STRING)
            }
        }

        val exception = assertThrows<IllegalArgumentException> { schema["default"].column("date") }

        assertThat(exception.message, equalTo("Column 'date' is not declared in SILO table 'default'"))
    }

    @Test
    fun `GIVEN duplicate declarations THEN schema construction fails`() {
        val tableException = assertThrows<IllegalArgumentException> {
            siloSchema {
                table("default")
                table("default")
            }
        }
        val columnException = assertThrows<IllegalArgumentException> {
            siloSchema {
                table("default") {
                    column("country", SiloColumnType.STRING)
                    column("country", SiloColumnType.STRING)
                }
            }
        }

        assertThat(tableException.message, equalTo("Table 'default' is declared more than once"))
        assertThat(columnException.message, equalTo("Column 'country' is declared more than once in table 'default'"))
    }

    @Test
    fun `GIVEN a hostile table name THEN renders it as one quoted identifier`() {
        val schema = siloSchema {
            table("default.filter(true)") {
                column("country", SiloColumnType.STRING)
            }
        }

        assertThat(schema.tables.single().render(), equalTo("\"default.filter(true)\""))
    }

    @Test
    fun `GIVEN equal schemas THEN equality and hash code depend on ordered typed columns`() {
        val first = siloSchema {
            table("default") {
                column("country", SiloColumnType.INDEXED_STRING)
                column("date", SiloColumnType.DATE32)
            }
        }
        val second = siloSchema {
            table("default") {
                column("country", SiloColumnType.INDEXED_STRING)
                column("date", SiloColumnType.DATE32)
            }
        }
        val differentType = siloSchema {
            table("default") {
                column("country", SiloColumnType.STRING)
                column("date", SiloColumnType.DATE32)
            }
        }
        val differentOrder = siloSchema {
            table("default") {
                column("date", SiloColumnType.DATE32)
                column("country", SiloColumnType.INDEXED_STRING)
            }
        }

        assertThat(first, equalTo(second))
        assertThat(first.hashCode(), equalTo(second.hashCode()))
        assertThat(first == differentType, equalTo(false))
        assertThat(first == differentOrder, equalTo(false))
    }

    @Test
    fun `GIVEN typed and derived fields with the same name THEN equality is symmetric`() {
        val column = siloSchema {
            table("default") {
                column("country", SiloColumnType.STRING)
            }
        }["default"]["country"]
        val derivedField = field("country")

        assertThat(column == derivedField, equalTo(false))
        assertThat(derivedField == column, equalTo(false))
        assertThat(setOf<SiloField>(column, derivedField).size, equalTo(2))
    }
}
