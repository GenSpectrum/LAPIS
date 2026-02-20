package org.genspectrum.lapis.response

import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.controller.QUERIES_OVER_TIME_ROUTE
import org.genspectrum.lapis.controller.SampleRoute
import org.genspectrum.lapis.controller.SampleRoute.AGGREGATED
import org.genspectrum.lapis.controller.SampleRoute.ALIGNED_AMINO_ACID_SEQUENCES
import org.genspectrum.lapis.controller.SampleRoute.ALIGNED_NUCLEOTIDE_SEQUENCES
import org.genspectrum.lapis.controller.SampleRoute.AMINO_ACID_INSERTIONS
import org.genspectrum.lapis.controller.SampleRoute.AMINO_ACID_MUTATIONS
import org.genspectrum.lapis.controller.SampleRoute.DETAILS
import org.genspectrum.lapis.controller.SampleRoute.MOST_RECENT_COMMON_ANCESTOR
import org.genspectrum.lapis.controller.SampleRoute.NUCLEOTIDE_INSERTIONS
import org.genspectrum.lapis.controller.SampleRoute.NUCLEOTIDE_MUTATIONS
import org.genspectrum.lapis.controller.SampleRoute.PHYLO_SUBTREE
import org.genspectrum.lapis.controller.SampleRoute.UNALIGNED_NUCLEOTIDE_SEQUENCES
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
        sections.add(generateApiEndpointsSection())
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

    private fun generateApiEndpointsSection(): String {
        val hasPhyloTreeField = databaseConfig.schema.metadata.any { it.isPhyloTreeField }

        val phylogeneticSection = if (hasPhyloTreeField) {
            """
            
### Phylogenetic Analysis

- ${markdownLink(getSampleUrl(MOST_RECENT_COMMON_ANCESTOR))}: Find most recent common ancestor for queried sequences
- ${markdownLink(getSampleUrl(PHYLO_SUBTREE))}: Get phylogenetic subtree in Newick format
            """.trimIndent()
        } else {
            ""
        }

        return """
## API Endpoints

### Data Retrieval

- ${markdownLink(getSampleUrl(AGGREGATED))}: Count and group sequences by metadata and mutations
- ${markdownLink(getSampleUrl(DETAILS))}: Retrieve detailed metadata for matching sequences
- ${markdownLink(getSampleUrl(ALIGNED_NUCLEOTIDE_SEQUENCES))}: Get aligned nucleotide sequences in FASTA format
- ${markdownLink(getSampleUrl(UNALIGNED_NUCLEOTIDE_SEQUENCES))}: Get unaligned nucleotide sequences
- ${markdownLink(getSampleUrl(ALIGNED_AMINO_ACID_SEQUENCES) + "/{gene}")}: Get aligned amino acid sequences for a specific gene

### Mutation Analysis

- ${markdownLink(getSampleUrl(NUCLEOTIDE_MUTATIONS))}: List nucleotide mutations with their proportions
- ${markdownLink(getSampleUrl(AMINO_ACID_MUTATIONS))}: List amino acid mutations with their proportions
- ${markdownLink(getSampleUrl(NUCLEOTIDE_INSERTIONS))}: List nucleotide insertions
- ${markdownLink(getSampleUrl(AMINO_ACID_INSERTIONS))}: List amino acid insertions

### Time Series

- ${markdownLink("/sample/$QUERIES_OVER_TIME_ROUTE")}: Query results aggregated over time
$phylogeneticSection

### Utility

- ${markdownLink("/sample/info")}: Get instance information and versions
- ${markdownLink("/sample/getDatabaseConfig")}: Retrieve the complete database configuration

All endpoints support both GET and POST methods. POST requests accept JSON or form-encoded data.
            """.trimIndent()
    }

    private fun markdownLink(href: String, name: String = href): String = """[$name]($href)"""

    private fun getSampleUrl(route: SampleRoute): String = "sample${route.pathSegment}"

    private fun getDocsUrl(path: String): String =
        if (lapisDocsUrl.isNotBlank()) {
            "$lapisDocsUrl/$path"
        } else {
            "https://github.com/GenSpectrum/LAPIS/blob/main/lapis-docs/src/content/docs/$path.md"
        }
}
