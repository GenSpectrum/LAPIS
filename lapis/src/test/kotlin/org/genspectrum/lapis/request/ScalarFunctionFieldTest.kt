package org.genspectrum.lapis.request

import org.genspectrum.lapis.config.DatabaseMetadata
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.databaseConfig
import org.genspectrum.lapis.request.converter.CaseInsensitiveFieldsCleaner
import org.genspectrum.lapis.request.converter.ScalarFunctionFieldConverter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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

private val underTest = ScalarFunctionFieldConverter(
    caseInsensitiveFieldsCleaner = CaseInsensitiveFieldsCleaner(testDatabaseConfig),
    databaseConfig = testDatabaseConfig,
)

class ScalarFunctionFieldTest {
    @Test
    fun `plain field name (no dot) returns null`() {
        assertNull(underTest.tryConvert("date"))
    }

    @Test
    fun `date_isoWeek returns ComputedField`() {
        val result = underTest.tryConvert("date.isoWeek")
        assertEquals(ComputedField("date", ScalarFunction.ISO_WEEK), result)
        assertEquals("date.isoWeek", result?.outputColumnName)
    }

    @Test
    fun `function name matching is case insensitive`() {
        val result = underTest.tryConvert("date.ISOWEEK")
        assertEquals(ComputedField("date", ScalarFunction.ISO_WEEK), result)
    }

    @Test
    fun `base field name in computed field is case insensitive`() {
        val result = underTest.tryConvert("DATE.isoWeek")
        assertEquals(ComputedField("date", ScalarFunction.ISO_WEEK), result)
    }

    @Test
    fun `unknown base field in computed syntax throws BadRequestException`() {
        assertThrows<BadRequestException> { underTest.tryConvert("unknown.isoWeek") }
    }

    @Test
    fun `unknown function name throws BadRequestException`() {
        assertThrows<BadRequestException> { underTest.tryConvert("date.unknownFunction") }
    }

    @Test
    fun `isoWeek on non-date field throws BadRequestException`() {
        val ex = assertThrows<BadRequestException> { underTest.tryConvert("country.isoWeek") }
        assertTrue(ex.message.orEmpty().contains("STRING"), "Expected error to mention type, got: ${ex.message}")
    }
}
