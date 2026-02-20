package org.genspectrum.lapis.response

import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.controller.AMINO_ACID_MUTATIONS_OVER_TIME_ROUTE
import org.genspectrum.lapis.controller.DATABASE_CONFIG_ROUTE
import org.genspectrum.lapis.controller.INFO_ROUTE
import org.genspectrum.lapis.controller.LINEAGE_DEFINITION_ROUTE
import org.genspectrum.lapis.controller.NUCLEOTIDE_MUTATIONS_OVER_TIME_ROUTE
import org.genspectrum.lapis.controller.QUERIES_OVER_TIME_ROUTE
import org.genspectrum.lapis.controller.REFERENCE_GENOME_ROUTE
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

- ${mdLink(getSampleLink(MOST_RECENT_COMMON_ANCESTOR))}: Find most recent common ancestor for queried sequences.
  Identifies the MRCA node in the phylogenetic tree that contains all sequences matching your filters. Useful for understanding evolutionary relationships.
- ${mdLink(getSampleLink(PHYLO_SUBTREE))}: Get phylogenetic subtree in Newick format.
  Returns a subtree containing only the sequences matching your filters. The subtree is in Newick format and can be visualized in phylogenetic tree viewers.
            """.trimIndent()
        } else {
            ""
        }

        return """
## API Endpoints

The OpenAPI spec is available at ${mdLink("api-docs")}.
Refer to that if you need more details on an endpoints.

### Data Retrieval and Mutation Analysis 

These endpoints are available as GET or POST.
Prefer POST since it allows more flexible requests.
Use GET when you want to have links that are easy to share since all their parameters can be passed as query parameters.

Those are the primary entrypoints for analyzing the data in this LAPIS instance.

- ${mdLink(getSampleLink(AGGREGATED))}: Count and group sequences by metadata and mutations.
  This is similar to a "select count(*) from ... group by <fields> where <filters>" SQL query.
- ${mdLink(getSampleLink(DETAILS))}:
  Returns the actual metadata values for sequences that match your filters. Use this to get individual sequence records.
  Similar to a "select <fields ?? *> from ... where <filters>" SQL query.
- ${mdLink(getSampleLink(ALIGNED_NUCLEOTIDE_SEQUENCES))}:
  Returns nucleotide sequences aligned to the reference genome in FASTA format.
  Usually used by users who want to download the sequences for offline analysis. Not recommended for large result sets.
- ${mdLink(getSampleLink(UNALIGNED_NUCLEOTIDE_SEQUENCES))}:
  Returns raw nucleotide sequences without alignment.
  Usually used by users who want to download the sequences for offline analysis. Not recommended for large result sets.
- ${mdLink(getSampleLink(ALIGNED_AMINO_ACID_SEQUENCES))}:
  Returns translated protein sequences for multiple genes at once.
  Usually used by users who want to download the sequences for offline analysis. Not recommended for large result sets.
- ${mdLink(getSampleLink(ALIGNED_AMINO_ACID_SEQUENCES) + "/{gene}")}:
  Returns translated protein sequences for a single gene. Specify the gene name in the URL path.
  Usually used by users who want to download the sequences for offline analysis. Not recommended for large result sets.
- ${mdLink(getSampleLink(NUCLEOTIDE_MUTATIONS))}: List nucleotide mutations with their proportions.
  Shows which nucleotide mutations appear in your filtered sequences and how frequently.
  Example: "C123T appears in 45% of sequences that match <filters>".
- ${mdLink(getSampleLink(AMINO_ACID_MUTATIONS))}: List amino acid mutations with their proportions.
  Shows which amino acid mutations appear in your filtered sequences and how frequently.
  Example: "S:484K appears in 30% of sequences that match <filters>".
- ${mdLink(getSampleLink(NUCLEOTIDE_INSERTIONS))}: List nucleotide insertions.
  Shows how often which insertion of nucleotides occurred in the nucleotide sequence(s) for the given filters.
- ${mdLink(getSampleLink(AMINO_ACID_INSERTIONS))}: List amino acid insertions.
  Shows how often which insertion of amino acids occurred in the amino acid sequences for the given filters.

### Time Series

These endpoints are mainly built for specialized display components that show time series data in a tabular form.
Useful for tracking trends over time.
These endpoints only accept POST.

- ${mdLink("sample/$QUERIES_OVER_TIME_ROUTE")}: Query results aggregated over time.
  Shows how many sequences match your filters for each time period (e.g., daily, weekly).
- ${mdLink("sample/$NUCLEOTIDE_MUTATIONS_OVER_TIME_ROUTE")}: Query nucleotide mutations aggregated over time.
  Shows how mutation frequencies change over time. Useful for tracking the emergence and spread of specific mutations.
- ${mdLink("sample/$AMINO_ACID_MUTATIONS_OVER_TIME_ROUTE")}: Query amino acid mutations aggregated over time.
  Shows how amino acid mutation frequencies change over time.
$phylogeneticSection

### Info

- ${mdLink("info$INFO_ROUTE")}: Get instance information and versions.
  Useful for debugging or confirming you're connected to the right instance.
- ${mdLink("info$DATABASE_CONFIG_ROUTE")}: Retrieve the complete database configuration.
  Contains the complete metadata schema and configuration. Use this to discover what fields are available for filtering.
- ${mdLink("info$REFERENCE_GENOME_ROUTE")}: Retrieve the complete reference genome.
  Returns the full reference genome sequences. Warning: Large response.
  Only use when you need the actual reference sequences.
- ${mdLink("info$LINEAGE_DEFINITION_ROUTE/{column}")}: 
  Retrieve the lineage definition file for a specific metadata column.
  Returns lineage hierarchy and parent-child relationships. Useful for understanding lineage classifications.
  Warning: Usually quite large.

All endpoints support both GET and POST methods. POST requests accept JSON or form-encoded data.
            """.trimIndent()
    }

    private fun mdLink(
        href: String,
        name: String = href,
    ): String = """[$name]($href)"""

    private fun getSampleLink(route: SampleRoute): String = "sample${route.pathSegment}"

    private fun getDocsUrl(path: String): String =
        if (lapisDocsUrl.isNotBlank()) {
            "$lapisDocsUrl/$path"
        } else {
            "https://github.com/GenSpectrum/LAPIS/blob/main/lapis-docs/src/content/docs/$path.md"
        }
}
