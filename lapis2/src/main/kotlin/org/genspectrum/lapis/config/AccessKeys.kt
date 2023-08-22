package org.genspectrum.lapis.config

import com.fasterxml.jackson.module.kotlin.readValue
import org.genspectrum.lapis.util.YamlObjectMapper
import org.springframework.stereotype.Component
import java.io.File

@Component
class AccessKeysReader(
    private val applicationProperties: LapisApplicationProperties,
    private val yamlObjectMapper: YamlObjectMapper,
) {
    fun read(): AccessKeys {
        val path = applicationProperties.accessKeys.path
            ?: throw IllegalArgumentException("Cannot read LAPIS access keys, lapis.access-keys.path was not set.")

        return yamlObjectMapper.objectMapper.readValue(File(path))
    }
}

data class AccessKeys(val fullAccessKey: String, val aggregatedDataAccessKey: String)
