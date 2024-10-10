package org.genspectrum.lapis

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.ComponentScan

@SpringBootTest
class LapisApplicationTest {
    @Test
    fun contextLoads() {
    }
}

class LapisApplicationFailsToLoadContextTest {
    @Test
    fun `GIVEN invalid database config THEN lapis crashes on startup`() {
        val contextRunner: ApplicationContextRunner = ApplicationContextRunner()
            .withUserConfiguration(LapisSpringConfig::class.java)
            .withUserConfiguration(ComponentScanConfig::class.java)
            .withPropertyValues("lapis.databaseConfig.path=src/test/resources/config/invalidTestDatabaseConfig.yaml")

        var cause = assertThrows<Throwable> {
            contextRunner.run { it!!.getBean("databaseConfig") }
        }

        while (cause.cause != null) {
            cause = cause.cause!!
        }

        assertThat(
            cause.message,
            containsString("key.with.reserved.character"),
        )
    }

    @TestConfiguration
    @ComponentScan("org.genspectrum.lapis")
    class ComponentScanConfig
}
