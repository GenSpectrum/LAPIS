package org.genspectrum.lapis.util

import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.module.kotlin.kotlinModule

@Component
object YamlObjectMapper {
    val objectMapper: ObjectMapper = YAMLMapper.builder()
        .addModule(kotlinModule())
        .build()
}
