package org.genspectrum.lapis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.genspectrum.lapis.config.DatabaseConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
class LapisSpringConfig {
    @Bean
    fun openAPI(databaseConfig: DatabaseConfig) = buildOpenApiSchema(databaseConfig)

    @Bean
    fun databaseConfig(@Value("\${lapis.databaseConfig.path}") configPath: String): DatabaseConfig {
        return ObjectMapper(YAMLFactory()).registerKotlinModule().readValue(File(configPath))
    }
}
