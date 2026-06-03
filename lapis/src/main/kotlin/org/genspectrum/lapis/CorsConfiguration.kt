package org.genspectrum.lapis

import org.genspectrum.lapis.controller.LapisHeaders.LAPIS_DATA_VERSION
import org.genspectrum.lapis.controller.LapisHeaders.REQUEST_ID
import org.genspectrum.lapis.controller.middleware.Compression
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.RETRY_AFTER
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
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

    override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        // Register compression media types with the Jackson JSON converter so that
        // error responses can be serialized even when the Content-Type is forced to
        // application/gzip or application/zstd by the CompressionFilter.
        val compressionMediaTypes = Compression.entries.map { it.contentType }
        converters.filterIsInstance<JacksonJsonHttpMessageConverter>().forEach { converter ->
            val supportedTypes = converter.supportedMediaTypes.toMutableList()
            supportedTypes.addAll(compressionMediaTypes)
            converter.supportedMediaTypes = supportedTypes
        }
    }
}
