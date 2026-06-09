package org.genspectrum.lapis.logging

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.stereotype.Component
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

@Component
class StatisticsLogObjectMapper {
    private val mapper = JsonMapper.builder()
        .addModule(kotlinModule())
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .changeDefaultPropertyInclusion {
            JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.USE_DEFAULTS)
        }
        .build()

    fun writeValueAsString(requestContext: RequestContext): String =
        mapper.writerFor(RequestContext::class.java).writeValueAsString(requestContext)
}
