package org.genspectrum.lapis

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Path

class LapisConfigTest{
    @Test
    fun `given an invalid config file then throws exception`() {
        val url = javaClass.getResource("/config/invalidLapisConfig.yml")
        val path = Path.of(url!!.path)

        val exception = assertThrows<MissingKotlinParameterException> { readLapisConfig(path) }
        assertThat(exception.message, containsString("failed for JSON property siloUrl"))
    }

    @Test
    fun `given a valid config file then returns config`() {
        val url = javaClass.getResource("/config/validLapisConfig.yml")
        val path = Path.of(url!!.path)

        val lapisConfig = readLapisConfig(path)

        assertThat(lapisConfig.siloUrl, equalTo("url.to.silo"))
    }
}