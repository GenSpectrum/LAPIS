package org.genspectrum.lapis

import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.DatabaseFeature
import org.genspectrum.lapis.config.DatabaseMetadata
import org.genspectrum.lapis.config.DatabaseSchema
import org.genspectrum.lapis.config.GENERALIZED_ADVANCED_QUERY_FEATURE
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.config.OpennessLevel
import org.genspectrum.lapis.config.SARS_COV2_VARIANT_QUERY_FEATURE
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

val dummySequenceFilterFields = SequenceFilterFields.fromDatabaseConfig(
    DatabaseConfig(
        schema = DatabaseSchema(
            instanceName = "dummy",
            opennessLevel = OpennessLevel.OPEN,
            metadata = listOf(
                DatabaseMetadata(name = PRIMARY_KEY_FIELD, type = MetadataType.STRING, isPhyloTreeField = true),
                DatabaseMetadata(name = DATE_FIELD, type = MetadataType.DATE),
                DatabaseMetadata(
                    name = PANGO_LINEAGE_FIELD,
                    type = MetadataType.STRING,
                    generateLineageIndex = "lineageIndex",
                ),
                DatabaseMetadata(name = "some_metadata", type = MetadataType.STRING),
                DatabaseMetadata(name = "other_metadata", type = MetadataType.STRING),
                DatabaseMetadata(name = "intField", type = MetadataType.INT),
                DatabaseMetadata(name = "floatField", type = MetadataType.FLOAT),
                DatabaseMetadata(name = "test_boolean_column", type = MetadataType.BOOLEAN),
            ),
            primaryKey = PRIMARY_KEY_FIELD,
            features = listOf(
                DatabaseFeature(name = SARS_COV2_VARIANT_QUERY_FEATURE),
                DatabaseFeature(name = GENERALIZED_ADVANCED_QUERY_FEATURE),
            ),
        ),
    ),
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
