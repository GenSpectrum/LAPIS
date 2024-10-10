package org.genspectrum.lapis.config

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class DatabaseConfigValidatorTest {
    private val underTest = DatabaseConfigValidator()

    @Test
    fun `GIVEN valid config THEN accepts it`() {
        val validConfig = DatabaseConfig(
            DatabaseSchema(
                instanceName = "test",
                primaryKey = "primaryKey",
                opennessLevel = OpennessLevel.OPEN,
                metadata = emptyList(),
            ),
        )

        assertDoesNotThrow { underTest.validate(validConfig) }
    }

    @Test
    fun `GIVEN config with regex separator in metadata field THEN config is invalid`() {
        val invalidConfig = DatabaseConfig(
            DatabaseSchema(
                instanceName = "test",
                primaryKey = "primaryKey",
                opennessLevel = OpennessLevel.OPEN,
                metadata = listOf(
                    DatabaseMetadata(
                        name = "field.with.regex.separator",
                        type = MetadataType.STRING,
                    ),
                ),
            ),
        )

        val exception = assertThrows<IllegalArgumentException> { underTest.validate(invalidConfig) }
        assertThat(
            exception.message,
            `is`("Metadata field name 'field.with.regex.separator' contains the reserved character '.'"),
        )
    }

    @Test
    fun `GIVEN lapisAllowsRegexSearch on non-string field THEN config is invalid`() {
        val invalidConfig = DatabaseConfig(
            DatabaseSchema(
                instanceName = "test",
                primaryKey = "primaryKey",
                opennessLevel = OpennessLevel.OPEN,
                metadata = listOf(
                    DatabaseMetadata(
                        name = "test field",
                        type = MetadataType.INT,
                        lapisAllowsRegexSearch = true,
                    ),
                ),
            ),
        )

        val exception = assertThrows<IllegalArgumentException> { underTest.validate(invalidConfig) }
        assertThat(
            exception.message,
            `is`("Metadata field 'test field' has lapisAllowsRegexSearch set to true, but is not of type STRING."),
        )
    }
}
