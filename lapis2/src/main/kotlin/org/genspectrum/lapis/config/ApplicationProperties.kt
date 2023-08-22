package org.genspectrum.lapis.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("lapis")
data class LapisApplicationProperties(
    var baseUrl: String = "",
    var databaseConfig: DatabaseApplicationConfig = DatabaseApplicationConfig(),
    var accessKeys: AccessKeysApplicationConfig = AccessKeysApplicationConfig(),
)

@Component
@ConfigurationProperties("database-config")
data class DatabaseApplicationConfig(var path: String? = null)

@Component
@ConfigurationProperties("access-keys")
data class AccessKeysApplicationConfig(var path: String? = null)
