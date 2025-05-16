package org.genspectrum.lapis

import org.genspectrum.lapis.config.SequenceFilterField
import org.genspectrum.lapis.config.SequenceFilterFieldType
import org.genspectrum.lapis.config.SequenceFilterFields
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

const val PRIMARY_KEY_FIELD = "primaryKey"
const val PANGO_LINEAGE_FIELD = "pangoLineage"
const val DATE_FIELD = "date"

const val FIELD_WITH_UPPERCASE_LETTER = PANGO_LINEAGE_FIELD
const val FIELD_WITH_ONLY_LOWERCASE_LETTERS = DATE_FIELD

val dummySequenceFilterFields = SequenceFilterFields(
    mapOf(
        DATE_FIELD to SequenceFilterFieldType.Date,
        "dateTo" to SequenceFilterFieldType.DateTo(DATE_FIELD),
        "dateFrom" to SequenceFilterFieldType.DateFrom(DATE_FIELD),
        PANGO_LINEAGE_FIELD to SequenceFilterFieldType.Lineage,
        "some_metadata" to SequenceFilterFieldType.String,
        "some_metadata.regex" to SequenceFilterFieldType.StringSearch("some_metadata"),
        "other_metadata" to SequenceFilterFieldType.String,
        "variantQuery" to SequenceFilterFieldType.VariantQuery,
        "advancedQuery" to SequenceFilterFieldType.AdvancedQuery,
        "intField" to SequenceFilterFieldType.Int,
        "intFieldTo" to SequenceFilterFieldType.IntTo("intField"),
        "intFieldFrom" to SequenceFilterFieldType.IntFrom("intField"),
        "floatField" to SequenceFilterFieldType.Float,
        "floatFieldTo" to SequenceFilterFieldType.FloatTo("floatField"),
        "floatFieldFrom" to SequenceFilterFieldType.FloatFrom("floatField"),
        "test_boolean_column" to SequenceFilterFieldType.Boolean,
    )
        .map { (name, type) -> name.lowercase() to SequenceFilterField(name, type) }
        .toMap(),
)

class ConstantsFulfillAssumptionsThatTheirNameSuggestsTest {
    @Test
    fun `field with uppercase letter has indeed uppercase and lowercase letters`() {
        assertThat(FIELD_WITH_UPPERCASE_LETTER.uppercase(), `is`(not(equalTo(FIELD_WITH_UPPERCASE_LETTER))))
        assertThat(FIELD_WITH_UPPERCASE_LETTER.lowercase(), `is`(not(equalTo(FIELD_WITH_UPPERCASE_LETTER))))
    }

    @Test
    fun `field with only lowercase letters has indeed only lowercase letters`() {
        assertThat(FIELD_WITH_ONLY_LOWERCASE_LETTERS.lowercase(), `is`(equalTo(FIELD_WITH_ONLY_LOWERCASE_LETTERS)))
    }
}
