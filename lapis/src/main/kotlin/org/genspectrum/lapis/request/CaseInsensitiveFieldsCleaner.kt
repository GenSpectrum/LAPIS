package org.genspectrum.lapis.request

import org.genspectrum.lapis.config.DatabaseConfig
import org.springframework.stereotype.Component

@Component
class CaseInsensitiveFieldsCleaner(
    databaseConfig: DatabaseConfig,
) {
    private val fieldsMap = databaseConfig.schema.metadata.map { it.name }.associateBy { it.lowercase() }

    private val phyloTreeFields = databaseConfig.schema.metadata.filter { it.isPhyloTreeField }.map { it.name }

    fun clean(fieldName: String) = fieldsMap[fieldName.lowercase()]

    fun getKnownFields() = fieldsMap.values

    fun getPhyloTreeFields() = phyloTreeFields
}
