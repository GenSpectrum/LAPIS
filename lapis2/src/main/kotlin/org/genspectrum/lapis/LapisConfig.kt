package org.genspectrum.lapis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.nio.file.Path

data class LapisConfig(val siloUrl: String)

fun readLapisConfig(configFile: Path): LapisConfig {
    val objectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    return objectMapper.readValue(configFile.toFile())
}
