package org.genspectrum.lapis.config

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.aMapWithSize
import org.hamcrest.Matchers.hasEntry
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SequenceFilterFieldsTest {
    @Test
    fun `given database config without fields then is empty`() {
        val input = databaseConfigWithFields(emptyList(), emptyList())

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, aMapWithSize(1))
        assertThat(underTest.fields, hasEntry("nucleotideMutations", SequenceFilterFieldType.MutationsList))
    }

    @Test
    fun `given database config with a string field then contains a string field`() {
        val input = databaseConfigWithFields(
            listOf(DatabaseMetadata("fieldName", "string")),
            emptyList(),
        )

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, aMapWithSize(2))
        assertThat(underTest.fields, hasEntry("fieldName", SequenceFilterFieldType.String))
    }

    @Test
    fun `given database config with a pango_lineage field then contains a pango_lineage field`() {
        val input = databaseConfigWithFields(
            listOf(DatabaseMetadata("pango lineage", "pango_lineage")),
            emptyList(),
        )

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, aMapWithSize(2))
        assertThat(underTest.fields, hasEntry("pango lineage", SequenceFilterFieldType.PangoLineage))
    }

    @Test
    fun `given database config with a date field then contains date, dateFrom and dateTo fields`() {
        val input = databaseConfigWithFields(listOf(DatabaseMetadata("dateField", "date")), emptyList())

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, aMapWithSize(4))
        assertThat(underTest.fields, hasEntry("dateField", SequenceFilterFieldType.Date))
        assertThat(underTest.fields, hasEntry("dateFieldFrom", SequenceFilterFieldType.DateFrom("dateField")))
        assertThat(underTest.fields, hasEntry("dateFieldTo", SequenceFilterFieldType.DateTo("dateField")))
    }

    @Test
    fun `given database config with an unknown field type then throws exception`() {
        val input = databaseConfigWithFields(
            listOf(DatabaseMetadata("fieldName", "unknown type")),
            emptyList(),
        )

        val exception = assertThrows<IllegalArgumentException> { SequenceFilterFields.fromDatabaseConfig(input) }
        assertThat(exception.message, `is`("Unknown field type 'unknown type' for field 'fieldName'"))
    }

    @Test
    fun `given database config with a feature of 'sarsCoV2VariantQuery' then contains variationQuery`() {
        val input = databaseConfigWithFields(emptyList(), listOf(DatabaseFeature("sarsCoV2VariantQuery")))

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, aMapWithSize(2))
        assertThat(underTest.fields, hasEntry("nucleotideMutations", SequenceFilterFieldType.MutationsList))
        assertThat(underTest.fields, hasEntry("variantQuery", SequenceFilterFieldType.VariantQuery))
    }

    private fun databaseConfigWithFields(
        databaseMetadata: List<DatabaseMetadata>,
        databaseFeatures: List<DatabaseFeature>,
    ) = DatabaseConfig(
        DatabaseSchema(
            "test config",
            databaseMetadata,
            "test primary key",
            databaseFeatures,
        ),
    )
}
