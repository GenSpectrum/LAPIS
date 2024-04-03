package org.genspectrum.lapis

import com.fasterxml.jackson.module.kotlin.readValue
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
import org.genspectrum.lapis.logging.RequestContext
import org.genspectrum.lapis.logging.RequestContextLogger
import org.genspectrum.lapis.logging.StatisticsLogObjectMapper
import org.genspectrum.lapis.openApi.buildOpenApiSchema
import org.genspectrum.lapis.util.TimeFactory
import org.genspectrum.lapis.util.YamlObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.filter.CommonsRequestLoggingFilter
import java.io.File

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

//    TODO(#627) reactivate this when the bug in Swagger UI is fixed
//    https://github.com/swagger-api/swagger-ui/issues/9550
//    @Bean
//    fun headerCustomizer() =
//        OperationCustomizer { operation, _ ->
//            val foundRequestIdHeaderParameter = operation.parameters?.any { it.name == REQUEST_ID_HEADER }
//            if (foundRequestIdHeaderParameter == false || foundRequestIdHeaderParameter == null) {
//                operation.addParametersItem(
//                    HeaderParameter().apply {
//                        name = REQUEST_ID_HEADER
//                        required = false
//                        description = REQUEST_ID_HEADER_DESCRIPTION
//                        content = Content().addMediaType("text/plain", MediaType().schema(Schema<String>()))
//                    },
//                )
//            }
//            operation
//        }

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
}
