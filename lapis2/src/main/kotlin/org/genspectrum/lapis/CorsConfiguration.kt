package org.genspectrum.lapis

import org.genspectrum.lapis.controller.LapisHeaders.LAPIS_DATA_VERSION
import org.genspectrum.lapis.controller.LapisHeaders.REQUEST_ID
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.RETRY_AFTER
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfiguration : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders(LAPIS_DATA_VERSION, REQUEST_ID, RETRY_AFTER)
            .maxAge(3600)
    }
}
