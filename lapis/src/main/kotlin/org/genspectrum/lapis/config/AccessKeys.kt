package org.genspectrum.lapis.config

import com.fasterxml.jackson.module.kotlin.readValue
import org.genspectrum.lapis.util.YamlObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File

@Component
class AccessKeysReader(
    @Value("\${lapis.accessKeys.path:#{null}}") private val accessKeysFile: String?,
    private val yamlObjectMapper: YamlObjectMapper,
) {
    fun read(): AccessKeys {
        if (accessKeysFile == null) {
            throw IllegalArgumentException("Cannot read LAPIS access keys, lapis.accessKeys.path was not set.")
        }

        return yamlObjectMapper.objectMapper.readValue(File(accessKeysFile))
    }
}

data class AccessKeys(
    val fullAccessKeys: List<String>,
    val aggregatedDataAccessKeys: List<String>,
)
