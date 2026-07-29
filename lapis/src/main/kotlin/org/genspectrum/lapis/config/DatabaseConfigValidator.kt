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
            if (it.name.contains('[') || it.name.contains(']')) {
                throw IllegalArgumentException(
                    "Metadata field name '${it.name}' contains a reserved character, '[' or ']'",
                )
            }
        }

        return databaseConfig
    }
}
