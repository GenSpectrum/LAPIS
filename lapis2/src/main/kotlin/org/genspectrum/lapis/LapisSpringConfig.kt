package org.genspectrum.lapis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.SequenceFilterFields
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
class LapisSpringConfig {
    @Bean
    fun openAPI(sequenceFilterFields: SequenceFilterFields) = buildOpenApiSchema(sequenceFilterFields)

    @Bean
    fun databaseConfig(@Value("\${lapis.databaseConfig.path}") configPath: String): DatabaseConfig {
        return ObjectMapper(YAMLFactory()).registerKotlinModule().readValue(File(configPath))
    }

    @Bean
    fun sequenceFilterFields(databaseConfig: DatabaseConfig) = SequenceFilterFields.fromDatabaseConfig(databaseConfig)
}
