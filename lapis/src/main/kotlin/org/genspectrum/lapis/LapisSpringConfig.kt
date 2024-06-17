package org.genspectrum.lapis

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.benmanes.caffeine.cache.Caffeine
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.HeaderParameter
import mu.KotlinLogging
import org.genspectrum.lapis.auth.DataOpennessAuthorizationFilterFactory
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.NO_REFERENCE_GENOME_FILENAME_ERROR_MESSAGE
import org.genspectrum.lapis.config.REFERENCE_GENOME_ENV_VARIABLE_NAME
import org.genspectrum.lapis.config.REFERENCE_GENOME_FILENAME_ARGS_NAME
import org.genspectrum.lapis.config.REFERENCE_GENOME_GENES_APPLICATION_ARG_PREFIX
import org.genspectrum.lapis.config.REFERENCE_GENOME_SEGMENTS_APPLICATION_ARG_PREFIX
import org.genspectrum.lapis.config.ReferenceGenome
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.config.ReferenceSequenceSchema
import org.genspectrum.lapis.config.SequenceFilterFields
import org.genspectrum.lapis.controller.LapisHeaders
import org.genspectrum.lapis.logging.RequestContext
import org.genspectrum.lapis.logging.RequestContextLogger
import org.genspectrum.lapis.logging.StatisticsLogObjectMapper
import org.genspectrum.lapis.openApi.REQUEST_ID_HEADER_DESCRIPTION
import org.genspectrum.lapis.openApi.buildOpenApiSchema
import org.genspectrum.lapis.util.TimeFactory
import org.genspectrum.lapis.util.YamlObjectMapper
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.filter.CommonsRequestLoggingFilter
import java.io.File
import java.util.concurrent.TimeUnit

@Configuration
@EnableScheduling
@EnableCaching
class LapisSpringConfig {
    @Bean
    fun openAPI(
        sequenceFilterFields: SequenceFilterFields,
        databaseConfig: DatabaseConfig,
        referenceGenomeSchema: ReferenceGenomeSchema,
    ) = buildOpenApiSchema(sequenceFilterFields, databaseConfig, referenceGenomeSchema)

    @Bean
    fun headerCustomizer() =
        OperationCustomizer { operation, _ ->
            val foundRequestIdHeaderParameter = operation.parameters?.any { it.name == LapisHeaders.REQUEST_ID }
            if (foundRequestIdHeaderParameter == false || foundRequestIdHeaderParameter == null) {
                operation.addParametersItem(
                    HeaderParameter().apply {
                        name = LapisHeaders.REQUEST_ID
                        required = false
                        description = REQUEST_ID_HEADER_DESCRIPTION
                        content = Content().addMediaType("text/plain", MediaType().schema(Schema<String>()))
                    },
                )
            }
            operation
        }

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
        filter.setIncludeHeaders(false)
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
    fun referenceGenomeSchema(
        @Value("\${$REFERENCE_GENOME_SEGMENTS_APPLICATION_ARG_PREFIX}") nucleotideSegments: List<String>,
        @Value("\${$REFERENCE_GENOME_GENES_APPLICATION_ARG_PREFIX}") genes: List<String>,
    ) = ReferenceGenomeSchema(
        nucleotideSegments.map { ReferenceSequenceSchema(it) },
        genes.map { ReferenceSequenceSchema(it) },
    )

    @Bean
    fun referenceGenome(
        @Value("\${$REFERENCE_GENOME_FILENAME_ARGS_NAME:#{null}}") referenceGenomeFilename: String?,
    ): ReferenceGenome {
        val filename = referenceGenomeFilename
            ?: System.getenv(REFERENCE_GENOME_ENV_VARIABLE_NAME)
            ?: throw IllegalArgumentException(NO_REFERENCE_GENOME_FILENAME_ERROR_MESSAGE)

        return ReferenceGenome.readFromFile(filename)
    }

    @Bean
    fun caffeineCacheManager(): CaffeineCacheManager {
        val cacheManager = CaffeineCacheManager()
        cacheManager.setCaffeine(caffeineCacheBuilder())
        return cacheManager
    }

    fun caffeineCacheBuilder(): Caffeine<Any, Any> {
        return Caffeine.newBuilder()
            .maximumWeight(50 * 1024 * 1024) // 50 MB
            .weigher { key, value ->
                // Define the weight function here
                // For example, if value is a String, you can use its length
                if (value is String) {
                    return@weigher (value as String).length
                }
                1
            }
    }
}
