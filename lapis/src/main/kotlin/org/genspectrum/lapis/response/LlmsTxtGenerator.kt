package org.genspectrum.lapis.response

import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class LlmsTxtGenerator(
    private val databaseConfig: DatabaseConfig,
    private val referenceGenomeSchema: ReferenceGenomeSchema,
    @Value("\${lapis.docs.url:}") private val lapisDocsUrl: String,
) {
    fun generate(): String {
        // TODO: Implement dynamic llms.txt generation
        // This will be implemented in a separate step
        return """
            # LAPIS - ${databaseConfig.schema.instanceName}
            
            > LAPIS (Lightweight API for Sequences) instance for ${databaseConfig.schema.instanceName}.
            
            TODO: Add complete llms.txt content generation
        """.trimIndent()
    }
}
