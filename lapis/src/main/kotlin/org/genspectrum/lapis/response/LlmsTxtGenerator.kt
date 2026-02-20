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
        val sections = mutableListOf<String>()

        sections.add(generateHeader())
        // TODO: Add more sections

        return sections.joinToString("\n\n")
    }

    private fun generateHeader(): String {
        val instanceName = databaseConfig.schema.instanceName
        val metadataCount = databaseConfig.schema.metadata.size
        val geneCount = referenceGenomeSchema.genes.size

        return buildString {
            appendLine("# LAPIS - $instanceName")
            appendLine()
            appendLine("> LAPIS (Lightweight API for Sequences) instance for $instanceName.")
            appendLine("> Query genomic sequence data with powerful mutation filters, metadata combinations, and Boolean logic.")
            appendLine()
            append("This instance contains data for $instanceName with $metadataCount metadata fields and $geneCount genes")
            if (!referenceGenomeSchema.isSingleSegmented()) {
                append(" across ${referenceGenomeSchema.nucleotideSequences.size} segments")
            }
            append(".")
        }.trimEnd()
    }

    private fun getDocsUrl(path: String): String =
        if (lapisDocsUrl.isNotBlank()) {
            "$lapisDocsUrl/$path"
        } else {
            "https://github.com/GenSpectrum/LAPIS/blob/main/lapis-docs/src/content/docs/$path.md"
        }
}
