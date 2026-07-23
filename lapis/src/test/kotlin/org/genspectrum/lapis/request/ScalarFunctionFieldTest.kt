package org.genspectrum.lapis.request

import org.genspectrum.lapis.config.DatabaseMetadata
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.databaseConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private val testDatabaseConfig = databaseConfig(
    primaryKey = "accession",
    metadata = listOf(
        DatabaseMetadata("accession", MetadataType.STRING),
        DatabaseMetadata("date", MetadataType.DATE),
        DatabaseMetadata("country", MetadataType.STRING),
    ),
)

private val underTest = CaseInsensitiveFieldConverter(
    caseInsensitiveFieldsCleaner = CaseInsensitiveFieldsCleaner(testDatabaseConfig),
    databaseConfig = testDatabaseConfig,
)

class ScalarFunctionFieldTest {
    @Test
    fun `plain field name returns Field_Plain`() {
        assertEquals(Field.Plain("date"), underTest.convert("date"))
    }

    @Test
    fun `plain field name is case insensitive`() {
        assertEquals(Field.Plain("date"), underTest.convert("DATE"))
    }

    @Test
    fun `date_isoWeek returns Field_Computed with correct alias`() {
        val result = underTest.convert("date.isoWeek")
        assertEquals(Field.Computed("date", ScalarFunction.ISO_WEEK), result)
        assertEquals("date.isoWeek", result.fieldName)
        assertEquals("__scalar_isoWeek_date", (result as Field.Computed).alias)
    }

    @Test
    fun `function name matching is case insensitive`() {
        val result = underTest.convert("date.ISOWEEK")
        assertEquals(Field.Computed("date", ScalarFunction.ISO_WEEK), result)
    }

    @Test
    fun `base field name in computed field is case insensitive`() {
        val result = underTest.convert("DATE.isoWeek")
        assertEquals(Field.Computed("date", ScalarFunction.ISO_WEEK), result)
    }

    @Test
    fun `unknown base field in computed syntax throws BadRequestException`() {
        assertThrows<BadRequestException> { underTest.convert("unknown.isoWeek") }
    }

    @Test
    fun `unknown function name throws BadRequestException`() {
        assertThrows<BadRequestException> { underTest.convert("date.unknownFunction") }
    }

    @Test
    fun `isoWeek on non-date field throws BadRequestException`() {
        val ex = assertThrows<BadRequestException> { underTest.convert("country.isoWeek") }
        assert(ex.message.orEmpty().contains("STRING")) { "Expected error to mention type, got: ${ex.message}" }
    }

    @Test
    fun `completely unknown field throws BadRequestException`() {
        assertThrows<BadRequestException> { underTest.convert("nonexistent") }
    }
}
