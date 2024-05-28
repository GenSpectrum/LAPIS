package org.genspectrum.lapis.controller

import org.genspectrum.lapis.util.YamlObjectMapper
import org.springframework.http.MediaType
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter
import org.springframework.stereotype.Component

@Component
class YamlHttpMessageConverter(yamlObjectMapper: YamlObjectMapper) :
    AbstractJackson2HttpMessageConverter(
        yamlObjectMapper.objectMapper,
        MediaType.parseMediaType(LapisMediaType.APPLICATION_YAML_VALUE),
    )
