package org.genspectrum.lapis.config

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SequenceFilterFieldsTest {
    @Test
    fun `given database config without fields then is empty`() {
        val input = databaseConfigWithFields()

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, `is`(empty()))
    }

    @Test
    fun `given database config with a string field then contains a string field`() {
        val input = databaseConfigWithFields(DatabaseMetadata("fieldName", "string"))

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(
            underTest.fields,
            containsInAnyOrder(SequenceFilterField("fieldName", SequenceFilterField.Type.String)),
        )
    }

    @Test
    fun `given database config with a pango_lineage field then contains a pango_lineage field`() {
        val input = databaseConfigWithFields(DatabaseMetadata("pango lineage", "pango_lineage"))

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(
            underTest.fields,
            containsInAnyOrder(SequenceFilterField("pango lineage", SequenceFilterField.Type.PangoLineage)),
        )
    }

    @Test
    fun `given database config with a date field then contains date, dateFrom and dateTo fields`() {
        val input = databaseConfigWithFields(DatabaseMetadata("dateField", "date"))

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(
            underTest.fields,
            containsInAnyOrder(
                SequenceFilterField("dateField", SequenceFilterField.Type.Date),
                SequenceFilterField("dateFieldFrom", SequenceFilterField.Type.DateFrom("dateField")),
                SequenceFilterField("dateFieldTo", SequenceFilterField.Type.DateTo("dateField")),
            ),
        )
    }

    @Test
    fun `given database config with an unknown field type then throws exception`() {
        val input = databaseConfigWithFields(DatabaseMetadata("fieldName", "unknown type"))

        val exception = assertThrows<IllegalArgumentException> { SequenceFilterFields.fromDatabaseConfig(input) }
        assertThat(exception.message, `is`("Unknown field type 'unknown type' for field 'fieldName'"))
    }

    private fun databaseConfigWithFields(vararg databaseMetadata: DatabaseMetadata) = DatabaseConfig(
        DatabaseSchema(
            "test config",
            databaseMetadata.asList(),
            "test primary key",
        ),
    )
}
