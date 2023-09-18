package org.genspectrum.lapis.config

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.aMapWithSize
import org.hamcrest.Matchers.hasEntry
import org.junit.jupiter.api.Test

class SequenceFilterFieldsTest {
    @Test
    fun `given database config without fields then is empty`() {
        val input = databaseConfigWithFields(emptyList())

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, aMapWithSize(0))
    }

    @Test
    fun `given database config with a string field then contains a string field`() {
        val input = databaseConfigWithFields(listOf(DatabaseMetadata("fieldName", MetadataType.STRING)))

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, aMapWithSize(1))
        assertThat(underTest.fields, hasEntry("fieldName", SequenceFilterFieldType.String))
    }

    @Test
    fun `given database config with a pango_lineage field then contains a pango_lineage field`() {
        val input = databaseConfigWithFields(listOf(DatabaseMetadata("pango lineage", MetadataType.PANGO_LINEAGE)))

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, aMapWithSize(1))
        assertThat(underTest.fields, hasEntry("pango lineage", SequenceFilterFieldType.PangoLineage))
    }

    @Test
    fun `given database config with a date field then contains date, dateFrom and dateTo fields`() {
        val input = databaseConfigWithFields(listOf(DatabaseMetadata("dateField", MetadataType.DATE)))

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, aMapWithSize(3))
        assertThat(underTest.fields, hasEntry("dateField", SequenceFilterFieldType.Date))
        assertThat(underTest.fields, hasEntry("dateFieldFrom", SequenceFilterFieldType.DateFrom("dateField")))
        assertThat(underTest.fields, hasEntry("dateFieldTo", SequenceFilterFieldType.DateTo("dateField")))
    }

    @Test
    fun `given database config with an int field then contains int, intFrom and intTo fields`() {
        val input = databaseConfigWithFields(listOf(DatabaseMetadata("intField", MetadataType.INT)))

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, aMapWithSize(3))
        assertThat(underTest.fields, hasEntry("intField", SequenceFilterFieldType.Int))
        assertThat(underTest.fields, hasEntry("intFieldFrom", SequenceFilterFieldType.IntFrom("intField")))
        assertThat(underTest.fields, hasEntry("intFieldTo", SequenceFilterFieldType.IntTo("intField")))
    }

    @Test
    fun `given database config with a float field then contains float, floatFrom and floatTo fields`() {
        val input = databaseConfigWithFields(listOf(DatabaseMetadata("floatField", MetadataType.FLOAT)))

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, aMapWithSize(3))
        assertThat(underTest.fields, hasEntry("floatField", SequenceFilterFieldType.Float))
        assertThat(underTest.fields, hasEntry("floatFieldFrom", SequenceFilterFieldType.FloatFrom("floatField")))
        assertThat(underTest.fields, hasEntry("floatFieldTo", SequenceFilterFieldType.FloatTo("floatField")))
    }

    @Test
    fun `given database config with a feature of 'sarsCoV2VariantQuery' then contains variantQuery`() {
        val input = databaseConfigWithFields(emptyList(), listOf(DatabaseFeature(SARS_COV2_VARIANT_QUERY_FEATURE)))

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, aMapWithSize(1))
        assertThat(underTest.fields, hasEntry("variantQuery", SequenceFilterFieldType.VariantQuery))
    }

    private fun databaseConfigWithFields(
        databaseMetadata: List<DatabaseMetadata>,
        databaseFeatures: List<DatabaseFeature> = emptyList(),
    ) = DatabaseConfig(
        DatabaseSchema(
            "test config",
            OpennessLevel.OPEN,
            databaseMetadata,
            "test primary key",
            databaseFeatures,
        ),
    )
}
