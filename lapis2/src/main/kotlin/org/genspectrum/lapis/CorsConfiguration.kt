package org.genspectrum.lapis

import org.genspectrum.lapis.openApi.REQUEST_ID_HEADER
import org.genspectrum.lapis.request.LAPIS_DATA_VERSION_HEADER
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
            .exposedHeaders(LAPIS_DATA_VERSION_HEADER, REQUEST_ID_HEADER, RETRY_AFTER)
            .maxAge(3600)
    }
}
