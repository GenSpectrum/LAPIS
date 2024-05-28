package org.genspectrum.lapis.request

import org.genspectrum.lapis.config.DatabaseConfig
import org.springframework.stereotype.Component

@Component
class CaseInsensitiveFieldsCleaner(databaseConfig: DatabaseConfig) {
    private val fieldsMap = databaseConfig.schema.metadata.map { it.name }.associateBy { it.lowercase() }

    fun clean(fieldName: String) = fieldsMap[fieldName.lowercase()]

    fun getKnownFields() = fieldsMap.values
}
