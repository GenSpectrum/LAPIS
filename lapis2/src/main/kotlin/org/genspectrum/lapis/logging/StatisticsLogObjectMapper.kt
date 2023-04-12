package org.genspectrum.lapis.logging

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.stereotype.Component

@Component
class StatisticsLogObjectMapper(objectMapperBuilder: Jackson2ObjectMapperBuilder) {
    private val mapper: ObjectMapper

    init {
        mapper = objectMapperBuilder.build()
        mapper.registerModule(JavaTimeModule())
        mapper.registerModule(kotlinModule())
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    fun writeValueAsString(requestContext: RequestContext): String {
        return mapper.writeValueAsString(requestContext)
    }
}
