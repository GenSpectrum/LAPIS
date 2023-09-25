package org.genspectrum.lapis

import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import org.genspectrum.lapis.auth.DataOpennessAuthorizationFilterFactory
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.NucleotideSequence
import org.genspectrum.lapis.config.REFERENCE_GENOME_APPLICATION_ARG_PREFIX
import org.genspectrum.lapis.config.ReferenceGenome
import org.genspectrum.lapis.config.SequenceFilterFields
import org.genspectrum.lapis.logging.RequestContext
import org.genspectrum.lapis.logging.RequestContextLogger
import org.genspectrum.lapis.logging.StatisticsLogObjectMapper
import org.genspectrum.lapis.util.TimeFactory
import org.genspectrum.lapis.util.YamlObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.CommonsRequestLoggingFilter
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.io.File

@Configuration
class LapisSpringConfig {
    @Bean
    fun openAPI(sequenceFilterFields: SequenceFilterFields, databaseConfig: DatabaseConfig) =
        buildOpenApiSchema(sequenceFilterFields, databaseConfig)

    @Bean
    fun databaseConfig(
        @Value("\${lapis.databaseConfig.path}") configPath: String,
        yamlObjectMapper: YamlObjectMapper,
    ): DatabaseConfig {
        return yamlObjectMapper.objectMapper.readValue(File(configPath))
    }

    @Bean
    fun sequenceFilterFields(databaseConfig: DatabaseConfig) = SequenceFilterFields.fromDatabaseConfig(databaseConfig)

    @Bean
    fun logFilter(): CommonsRequestLoggingFilter {
        val filter = CommonsRequestLoggingFilter()
        filter.setIncludeQueryString(true)
        filter.setIncludePayload(true)
        filter.setMaxPayloadLength(10000)
        filter.setIncludeHeaders(false)
        filter.setAfterMessagePrefix("REQUEST DATA: ")
        return filter
    }

    @Bean
    fun requestContextLogger(
        requestContext: RequestContext,
        statisticsLogObjectMapper: StatisticsLogObjectMapper,
        timeFactory: TimeFactory,
    ) = RequestContextLogger(
        requestContext,
        statisticsLogObjectMapper,
        KotlinLogging.logger("StatisticsLogger"),
        timeFactory,
    )

    @Bean
    fun dataOpennessAuthorizationFilter(
        dataOpennessAuthorizationFilterFactory: DataOpennessAuthorizationFilterFactory,
    ) = dataOpennessAuthorizationFilterFactory.create()

    @Bean
    fun referenceGenome(
        @Value("\${$REFERENCE_GENOME_APPLICATION_ARG_PREFIX}") nucleotideSegments: List<String>,
    ): ReferenceGenome {
        return ReferenceGenome(nucleotideSegments.map { NucleotideSequence(it) })
    }

    @Bean
    fun corsConfigurer(databaseConfig: DatabaseConfig): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                if (databaseConfig.schema.allowedCorsOrigins.isEmpty()) {
                    return
                }

                registry.addMapping("/*")
                    .allowedOrigins(*(databaseConfig.schema.allowedCorsOrigins.toTypedArray()))
                    .allowedMethods("GET", "POST")
            }
        }
    }
}
