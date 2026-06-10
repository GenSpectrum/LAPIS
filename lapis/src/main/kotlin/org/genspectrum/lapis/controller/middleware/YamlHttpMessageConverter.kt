package org.genspectrum.lapis.controller.middleware

import org.genspectrum.lapis.controller.LapisMediaType
import org.genspectrum.lapis.util.YamlObjectMapper
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class YamlHttpMessageConverter(
    yamlObjectMapper: YamlObjectMapper,
) : AbstractJacksonHttpMessageConverter<ObjectMapper>(
        yamlObjectMapper.objectMapper,
        MediaType.parseMediaType(LapisMediaType.APPLICATION_YAML_VALUE),
    )
