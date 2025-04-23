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
    fun `given database config with a string field allowing regex then contains a string and string search field`() {
        val input = databaseConfigWithFields(
            listOf(
                DatabaseMetadata(
                    name = "fieldName",
                    type = MetadataType.STRING,
                    lapisAllowsRegexSearch = true,
                ),
            ),
        )

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, aMapWithSize(2))
        assertThat(
            underTest.fields,
            hasEntry("fieldname", SequenceFilterField("fieldName", SequenceFilterFieldType.String)),
        )
        assertThat(
            underTest.fields,
            hasEntry(
                "fieldname.regex",
                SequenceFilterField("fieldName.regex", SequenceFilterFieldType.StringSearch("fieldName")),
            ),
        )
    }

    @Test
    fun `given database config with a string field forbidding regex then only contains a string field`() {
        val input = databaseConfigWithFields(listOf(DatabaseMetadata("fieldName", MetadataType.STRING)))

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, aMapWithSize(1))
        assertThat(
            underTest.fields,
            hasEntry("fieldname", SequenceFilterField("fieldName", SequenceFilterFieldType.String)),
        )
    }

    @Test
    fun `GIVEN database config with a field with lineage index THEN contains a lineage field`() {
        val input = databaseConfigWithFields(
            listOf(
                DatabaseMetadata(
                    name = "pangoLineage",
                    type = MetadataType.STRING,
                    generateLineageIndex = true,
                ),
            ),
        )

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, aMapWithSize(1))
        assertThat(
            underTest.fields,
            hasEntry("pangolineage", SequenceFilterField("pangoLineage", SequenceFilterFieldType.Lineage)),
        )
    }

    @Test
    fun `GIVEN config with a field with lineage index and regex search THEN contains a lineage and regex field`() {
        val input = databaseConfigWithFields(
            listOf(
                DatabaseMetadata(
                    name = "pangoLineage",
                    type = MetadataType.STRING,
                    generateLineageIndex = true,
                    lapisAllowsRegexSearch = true,
                ),
            ),
        )

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, aMapWithSize(2))
        assertThat(
            underTest.fields,
            hasEntry("pangolineage", SequenceFilterField("pangoLineage", SequenceFilterFieldType.Lineage)),
        )
        assertThat(
            underTest.fields,
            hasEntry(
                "pangolineage.regex",
                SequenceFilterField("pangoLineage.regex", SequenceFilterFieldType.StringSearch("pangoLineage")),
            ),
        )
    }

    @Test
    fun `given database config with a date field then contains date, dateFrom and dateTo fields`() {
        val input = databaseConfigWithFields(listOf(DatabaseMetadata("dateField", MetadataType.DATE)))

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, aMapWithSize(3))
        assertThat(
            underTest.fields,
            hasEntry("datefield", SequenceFilterField("dateField", SequenceFilterFieldType.Date)),
        )
        assertThat(
            underTest.fields,
            hasEntry(
                "datefieldfrom",
                SequenceFilterField("dateFieldFrom", SequenceFilterFieldType.DateFrom("dateField")),
            ),
        )
        assertThat(
            underTest.fields,
            hasEntry("datefieldto", SequenceFilterField("dateFieldTo", SequenceFilterFieldType.DateTo("dateField"))),
        )
    }

    @Test
    fun `given database config with an int field then contains int, intFrom and intTo fields`() {
        val input = databaseConfigWithFields(listOf(DatabaseMetadata("intField", MetadataType.INT)))

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, aMapWithSize(3))
        assertThat(underTest.fields, hasEntry("intfield", SequenceFilterField("intField", SequenceFilterFieldType.Int)))
        assertThat(
            underTest.fields,
            hasEntry("intfieldfrom", SequenceFilterField("intFieldFrom", SequenceFilterFieldType.IntFrom("intField"))),
        )
        assertThat(
            underTest.fields,
            hasEntry("intfieldto", SequenceFilterField("intFieldTo", SequenceFilterFieldType.IntTo("intField"))),
        )
    }

    @Test
    fun `given database config with a float field then contains float, floatFrom and floatTo fields`() {
        val input = databaseConfigWithFields(listOf(DatabaseMetadata("floatField", MetadataType.FLOAT)))

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, aMapWithSize(3))
        assertThat(
            underTest.fields,
            hasEntry("floatfield", SequenceFilterField("floatField", SequenceFilterFieldType.Float)),
        )
        assertThat(
            underTest.fields,
            hasEntry(
                "floatfieldfrom",
                SequenceFilterField("floatFieldFrom", SequenceFilterFieldType.FloatFrom("floatField")),
            ),
        )
        assertThat(
            underTest.fields,
            hasEntry(
                "floatfieldto",
                SequenceFilterField("floatFieldTo", SequenceFilterFieldType.FloatTo("floatField")),
            ),
        )
    }

    @Test
    fun `given database config with a feature of 'sarsCoV2VariantQuery' then contains variantQuery`() {
        val input = databaseConfigWithFields(emptyList(), listOf(DatabaseFeature(SARS_COV2_VARIANT_QUERY_FEATURE)))

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, aMapWithSize(1))
        assertThat(
            underTest.fields,
            hasEntry("variantquery", SequenceFilterField("variantQuery", SequenceFilterFieldType.VariantQuery)),
        )
    }

    @Test
    fun `given database config with a feature of 'generalizedAdvancedQuery' then contains advancedQuery`() {
        val input = databaseConfigWithFields(emptyList(), listOf(DatabaseFeature(GENERALIZED_ADVANCED_QUERY_FEATURE)))

        val underTest = SequenceFilterFields.fromDatabaseConfig(input)

        assertThat(underTest.fields, aMapWithSize(1))
        assertThat(
            underTest.fields,
            hasEntry("advancedquery", SequenceFilterField("advancedQuery", SequenceFilterFieldType.AdvancedQuery)),
        )
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
