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
    @param:Value("\${lapis.docs.url:}") private val lapisDocsUrl: String,
) {
    fun generate(): String {
        val sections = mutableListOf<String>()

        sections.add(generateHeader())
        sections.add(generateInstanceConfigurationSection())
        sections.add(generateApiEndpointsSection())
        sections.add(generateQueryExamplesSection())

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

        val docsLink = if (lapisDocsUrl.isEmpty()) {
            ""
        } else {
            """
            If you need more detailed information than mentioned in this file, 
            refer to the ${mdLink(lapisDocsUrl, "LAPIS docs")}.

            """.trimIndent()
        }

        return """
# LAPIS - $instanceName

> LAPIS (Lightweight API for Sequences) instance for $instanceName.
> Query genomic sequence data with powerful mutation filters, metadata combinations, and Boolean logic.

This instance contains data for $instanceName with $metadataCount metadata fields and $geneCount genes$segmentInfo.

The LAPIS code is open source and available at https://github.com/GenSpectrum/LAPIS.
LAPIS is a convenience API around SILO, a high-performance query engine for genomic sequences.
The code is available at https://github.com/GenSpectrum/LAPIS-SILO.

$docsLink
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

        // TODO: advanced queries, mutation queries

        val geneNames = referenceGenomeSchema.genes.joinToString(", ") { it.name }

        val filterExamples = buildString {
            val stringFields = databaseConfig.schema.metadata.filter { it.type == MetadataType.STRING }
            if (stringFields.isNotEmpty()) {
                val field = stringFields.first().name
                appendLine("- **String fields**: Use exact or regex match.")
                appendLine("  Examples: `\"$field\": \"someValue\"`, `\"$field.regex\": \"^startsWithThis*\"`")
            }

            val dateFields = databaseConfig.schema.metadata.filter { it.type == MetadataType.DATE }
            if (dateFields.isNotEmpty()) {
                val field = dateFields.first().name
                appendLine(
                    "- **Date fields**: Use `From` and `To` suffixes for ranges. Example: `\"${field}From\": \"2023-01-01\", \"${field}To\": \"2023-12-31\"`",
                )
            }

            val intFields = databaseConfig.schema.metadata.filter { it.type == MetadataType.INT }
            if (intFields.isNotEmpty()) {
                val field = intFields.first().name
                appendLine(
                    "- **Integer fields**: Use exact match or `From`/`To` for ranges. Example: `\"$field\": 42` or `\"${field}From\": 10, \"${field}To\": 50`",
                )
            }

            val floatFields = databaseConfig.schema.metadata.filter { it.type == MetadataType.FLOAT }
            if (floatFields.isNotEmpty()) {
                val field = floatFields.first().name
                appendLine(
                    "- **Float fields**: Use exact match or `From`/`To` for ranges. Example: `\"$field\": 0.95` or `\"${field}From\": 0.8, \"${field}To\": 1.0`",
                )
            }

            val boolFields = databaseConfig.schema.metadata.filter { it.type == MetadataType.BOOLEAN }
            if (boolFields.isNotEmpty()) {
                val field = boolFields.first().name
                appendLine("- **Boolean fields**: Use `true` or `false`. Example: `\"$field\": true`")
            }
        }

        return """
## Instance Configuration

### Metadata Fields

The following metadata fields are available for filtering on this instance:

$metadataFields

### How to Filter by Metadata Fields

You can use metadata fields as filter parameters in your queries. The filter syntax depends on the field type:

$filterExamples
You can combine multiple filters in a single query. All filters are combined with AND logic.
All exact filters also support filtering for `null`.

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
Refer to that if you need more details on an endpoint.

### Data Retrieval and Mutation Analysis 

These are the primary entrypoints for analyzing the data in this LAPIS instance.

These endpoints are available as GET or POST.
Prefer POST since it allows more flexible requests.
Use GET when you want to have links that are easy to share since all their parameters can be passed as query parameters.

Every endpoint accepts filters on metadata fields and mutations.
Use these to narrow down the sequences that are included in the results.

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

    private fun generateQueryExamplesSection(): String {
        // Find useful fields for examples
        val primaryKey = databaseConfig.schema.primaryKey
        val dateField = databaseConfig.schema.metadata.find { it.type == MetadataType.DATE }?.name
        val stringField = databaseConfig.schema.metadata
            .find { it.type == MetadataType.STRING && it.name != primaryKey }
            ?.name
        val firstGene = referenceGenomeSchema.genes.firstOrNull()?.name

        return """
## Query Examples

These examples demonstrate common query patterns using POST requests with JSON payloads.
All sample endpoints also support GET requests with query parameters.

### Example 1: Count sequences by a metadata field

```
POST ${getSampleLink(AGGREGATED)}
Content-Type: application/json

{
  "fields": ["$stringField"]
}
```

Returns count of sequences grouped by $stringField values.

### Example 2: Filter by date range

```
POST ${getSampleLink(AGGREGATED)}
Content-Type: application/json

{
  "${dateField}From": "2023-01-01",
  "${dateField}To": "2023-12-31",
  "fields": ["$dateField"]
}
```

Returns sequences within the specified date range, grouped by date.

### Example 3: Find sequences with specific mutation

```
POST ${getSampleLink(DETAILS)}
Content-Type: application/json

{
  "nucleotideMutations": ["C123T"],
  "limit": 10
}
```

Returns up to 10 sequences with the C123T nucleotide mutation.

### Example 4: Complex mutation filter with Boolean logic

```
POST ${getSampleLink(AGGREGATED)}
Content-Type: application/json

{
  "nucleotideMutations": ["C123T"],
  "aminoAcidMutations": ["S:484K", "S:484E"],
  "fields": ["$stringField"]
}
```

Returns sequences with C123T mutation AND either S:484K OR S:484E amino acid mutation, grouped by $stringField.

### Example 5: Get sequences for a specific gene

```
POST ${getSampleLink(ALIGNED_AMINO_ACID_SEQUENCES)}
Content-Type: application/json

{
  "genes": ["$firstGene"],
  "$stringField": "someValue",
  "limit": 5
}
```

Returns up to 5 aligned amino acid sequences for the $firstGene gene.

### Example 6: Analyze mutation proportions

```
POST ${getSampleLink(NUCLEOTIDE_MUTATIONS)}
Content-Type: application/json

{
  "$stringField": "someValue",
  "minProportion": 0.05
}
```

Returns all nucleotide mutations appearing in at least 5% of sequences matching the filter.
            """.trimIndent()
    }

    private fun mdLink(
        href: String,
        name: String = href,
    ): String = """[$name]($href)"""

    private fun getSampleLink(route: SampleRoute): String = "sample${route.pathSegment}"
}
