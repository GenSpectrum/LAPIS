package org.genspectrum.lapis

import org.genspectrum.lapis.config.DatabaseConfigValidator
import org.genspectrum.lapis.config.ViewRegistry
import org.genspectrum.lapis.util.YamlObjectMapper
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class LapisApplicationTest {
    @Test
    fun contextLoads() {
    }
}

class LapisApplicationFailsToLoadContextTest {
    @Test
    fun `GIVEN invalid database config THEN lapis crashes on startup`() {
        var cause = assertThrows<Throwable> {
            ViewRegistry(
                manifestPath = "src/test/resources/config/views-invalid-test.yaml",
                yamlObjectMapper = YamlObjectMapper,
                databaseConfigValidator = DatabaseConfigValidator(),
            )
        }

        while (cause.cause != null) {
            cause = cause.cause!!
        }

        assertThat(
            cause.message,
            containsString("key.with.reserved.character"),
        )
    }
}
