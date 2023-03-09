package org.genspectrum.lapis

import org.genspectrum.lapis.silo.SiloClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path

@Configuration
class LapisSpringConfig {
    @Bean
    fun siloClient(lapisConfig: LapisConfig): SiloClient {
        return SiloClient(lapisConfig.siloUrl)
    }

    @Bean
    fun lapisConfig(): LapisConfig {
        return readLapisConfig(Path.of("config.yml"))
    }
}