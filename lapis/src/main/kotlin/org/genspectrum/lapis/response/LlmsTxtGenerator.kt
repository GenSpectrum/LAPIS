package org.genspectrum.lapis.response

import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.MetadataType
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
        val sections = mutableListOf<String>()

        sections.add(generateHeader())
        sections.add(generateInstanceConfigurationSection())
        // TODO: Add more sections

        return sections.joinToString("\n\n")
    }

    private fun generateHeader(): String {
        val instanceName = databaseConfig.schema.instanceName
        val metadataCount = databaseConfig.schema.metadata.size
        val geneCount = referenceGenomeSchema.genes.size

        val segmentInfo = if (!referenceGenomeSchema.isSingleSegmented()) {
            " across ${referenceGenomeSchema.nucleotideSequences.size} segments"
        } else {
            ""
        }

        return """
                # LAPIS - $instanceName
                
                > LAPIS (Lightweight API for Sequences) instance for $instanceName.
                > Query genomic sequence data with powerful mutation filters, metadata combinations, and Boolean logic.
                
                This instance contains data for $instanceName with $metadataCount metadata fields and $geneCount genes$segmentInfo.
            """.trimIndent()
    }

    private fun generateInstanceConfigurationSection(): String {
        val metadataFields = databaseConfig.schema.metadata.joinToString("\n") { field ->
            val typeDescription = when (field.type) {
                MetadataType.STRING -> "string"
                MetadataType.DATE -> "date"
                MetadataType.INT -> "integer"
                MetadataType.FLOAT -> "float"
                MetadataType.BOOLEAN -> "boolean"
            }
            "- **${field.name}** ($typeDescription)"
        }

        val segmentInfo = if (referenceGenomeSchema.isSingleSegmented()) {
            ""
        } else {
            val segmentNames = referenceGenomeSchema.nucleotideSequences.joinToString(", ") { it.name }
            """
                This instance uses a multi-segmented genome with ${referenceGenomeSchema.nucleotideSequences.size} segments:
                
                Segments: $segmentNames
                
            """.trimIndent()
        }

        val geneNames = referenceGenomeSchema.genes.joinToString(", ") { it.name }

        return """
                ## Instance Configuration
                
                ### Metadata Fields
                
                The following metadata fields are available for filtering on this instance:
                
                $metadataFields
                
                ### Genes and Segments
                
                $segmentInfo
                Available genes for amino acid queries:
                
                Genes: $geneNames
            """.trimIndent()
    }

    private fun getDocsUrl(path: String): String =
        if (lapisDocsUrl.isNotBlank()) {
            "$lapisDocsUrl/$path"
        } else {
            "https://github.com/GenSpectrum/LAPIS/blob/main/lapis-docs/src/content/docs/$path.md"
        }
}
