package org.genspectrum.lapis.request.converter

import org.genspectrum.lapis.config.DatabaseMetadata
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.databaseConfig
import org.genspectrum.lapis.request.LineageWithSublineagesField
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LineageFieldConverterTest {
    private val underTest = LineageFieldConverter(
        databaseConfig = databaseConfig(
            primaryKey = "primaryKey",
            metadata = listOf(
                DatabaseMetadata(name = "primaryKey", type = MetadataType.STRING),
                DatabaseMetadata(
                    name = "pangoLineage",
                    type = MetadataType.STRING,
                    generateLineageIndex = "lineage_definitions",
                ),
                DatabaseMetadata(name = "country", type = MetadataType.STRING),
            ),
        ),
    )

    @Test
    fun `GIVEN a lineage field with trailing star THEN returns a LineageWithSublineagesField`() {
        assertThat(underTest.tryConvert("pangoLineage*"), equalTo(LineageWithSublineagesField("pangoLineage")))
    }

    @Test
    fun `GIVEN a lineage field with trailing star in different casing THEN canonicalizes the field name`() {
        assertThat(underTest.tryConvert("PANGOLINEAGE*"), equalTo(LineageWithSublineagesField("pangoLineage")))
    }

    @Test
    fun `GIVEN a field without trailing star THEN returns null so plain handling can take over`() {
        assertThat(underTest.tryConvert("pangoLineage"), nullValue())
        assertThat(underTest.tryConvert("country"), nullValue())
    }

    @Test
    fun `GIVEN a non-lineage field with trailing star THEN throws`() {
        val exception = assertThrows<BadRequestException> { underTest.tryConvert("country*") }
        assertThat(exception.message!!.contains("only allowed for fields with a lineage index"), equalTo(true))
    }

    @Test
    fun `GIVEN an unknown field with trailing star THEN throws`() {
        assertThrows<BadRequestException> { underTest.tryConvert("doesNotExist*") }
    }
}
