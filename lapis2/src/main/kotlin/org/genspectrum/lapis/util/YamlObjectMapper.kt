package org.genspectrum.lapis.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.stereotype.Component

@Component
object YamlObjectMapper {
    val objectMapper: ObjectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
}
