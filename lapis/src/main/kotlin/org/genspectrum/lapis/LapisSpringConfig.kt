package org.genspectrum.lapis

import com.fasterxml.jackson.core.StreamReadConstraints
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.HeaderParameter
import io.swagger.v3.oas.models.security.SecurityRequirement
import mu.KotlinLogging
import org.genspectrum.lapis.auth.DataOpennessAuthorizationFilterFactory
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.DatabaseConfigValidator
import org.genspectrum.lapis.config.LapisVersion
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
import org.genspectrum.lapis.openApi.SECURITY_SCHEMA_NAME
import org.genspectrum.lapis.openApi.buildOpenApiSchema
import org.genspectrum.lapis.util.TimeFactory
import org.genspectrum.lapis.util.YamlObjectMapper
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.CommonsRequestLoggingFilter
import java.io.File

private const val VERSION_FILE = "version.txt"

@Configuration
@EnableScheduling
@EnableCaching
class LapisSpringConfig {
    @Bean
    fun openAPI(
        sequenceFilterFields: SequenceFilterFields,
        databaseConfig: DatabaseConfig,
        referenceGenomeSchema: ReferenceGenomeSchema,
        resourceServerProperties: OAuth2ResourceServerProperties,
    ) = buildOpenApiSchema(sequenceFilterFields, databaseConfig, referenceGenomeSchema, resourceServerProperties)

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

    /**
     * Adds authentication to data access API endpoints, if this LAPIS instance is configured to use authentication.
     */
    @Bean
    fun securityCustomizer(resourceServerProperties: OAuth2ResourceServerProperties) =
        OpenApiCustomizer { openApi ->
            if (resourceServerProperties.isConfiguredToUseAuth()) {
                val pathMatcher = AntPathMatcher()

                openApi.paths
                    .filterKeys { path ->
                        PUBLIC_ROUTES.none { pattern -> pathMatcher.match(pattern, path) }
                    }
                    .forEach { (_, pathItem) ->
                        pathItem.readOperations()?.forEach { operation ->
                            operation.addSecurityItem(SecurityRequirement().addList(SECURITY_SCHEMA_NAME))
                        }
                    }
            }
        }

    @Bean
    fun databaseConfig(
        @Value("\${lapis.databaseConfig.path}") configPath: String,
        yamlObjectMapper: YamlObjectMapper,
        databaseConfigValidator: DatabaseConfigValidator,
    ): DatabaseConfig =
        yamlObjectMapper.objectMapper.readValue<DatabaseConfig>(File(configPath))
            .let { databaseConfigValidator.validate(it) }

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
    fun lapisVersion() =
        try {
            LapisVersion(File(VERSION_FILE).readText().trim())
        } catch (e: Exception) {
            log.info { "Failed to read $VERSION_FILE, assuming local version: ${e.message}" }
            LapisVersion("local")
        }

    @Bean
    fun streamConstraintsCustomizer(): Jackson2ObjectMapperBuilderCustomizer =
        Jackson2ObjectMapperBuilderCustomizer { builder: Jackson2ObjectMapperBuilder ->
            builder.postConfigurer { objectMapper: ObjectMapper ->
                objectMapper.factory.setStreamReadConstraints(
                    StreamReadConstraints.builder()
                        .maxStringLength(200_000_000)
                        .build(),
                )
            }
        }
}
