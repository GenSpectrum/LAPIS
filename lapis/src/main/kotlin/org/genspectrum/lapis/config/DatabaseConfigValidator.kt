package org.genspectrum.lapis.config

import org.springframework.stereotype.Component

@Component
class DatabaseConfigValidator {
    fun validate(databaseConfig: DatabaseConfig): DatabaseConfig {
        databaseConfig.schema.metadata.forEach {
            if (it.name.contains('.')) {
                throw IllegalArgumentException(
                    "Metadata field name '${it.name}' contains the reserved character '.'",
                )
            }

            if (it.lapisAllowsRegexSearch && it.type != MetadataType.STRING) {
                throw IllegalArgumentException(
                    "Metadata field '${it.name}' has lapisAllowsRegexSearch set to true, but is not of type STRING.",
                )
            }
        }

        return databaseConfig
    }
}
