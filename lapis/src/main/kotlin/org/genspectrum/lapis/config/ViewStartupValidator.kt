package org.genspectrum.lapis.config

import org.genspectrum.lapis.silo.SiloUris
import org.genspectrum.lapis.silo.applyRequestFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Component
class ViewStartupValidator(
    private val viewRegistry: ViewRegistry,
    private val siloUris: SiloUris,
    private val objectMapper: ObjectMapper,
    @Value("\${lapis.validateViewsOnStartup:true}") private val enabled: Boolean,
) : ApplicationRunner {
    private val httpClient = HttpClient.newHttpClient()

    override fun run(args: ApplicationArguments) {
        if (!enabled) {
            return
        }
        viewRegistry.views.values.forEach(::validate)
    }

    private fun validate(view: ViewConfig) {
        val schema = querySchema("${applyRequestFilter(view.baseQuery, "true")}.schema()", view.viewName)
        validateDeclaredSchema(view, schema)
        val tableScanBaseQuery = view.tableScanQuery ?: view.baseQuery
        if (view.tableScanQuery != null) {
            validateTableScanSchema(
                view = view,
                actualFields = querySchema("${applyRequestFilter(tableScanBaseQuery, "true")}.schema()", view.viewName),
            )
        }

        if (view.supports(ViewCapability.METADATA)) {
            probe("${applyRequestFilter(view.baseQuery, "false")}.groupBy({count:=count()}).schema()", view)
        }
        if (view.supports(ViewCapability.MUTATIONS)) {
            view.referenceGenomeSchema.getNucleotideSequenceNames().takeIf { it.isNotEmpty() }?.let {
                probe(
                    applyRequestFilter(tableScanBaseQuery, "false") +
                        ".mutations(minProportion:=0.1, sequenceNames:=${set(it)}).schema()",
                    view,
                )
            }
            view.referenceGenomeSchema.getGeneNames().takeIf { it.isNotEmpty() }?.let {
                probe(
                    applyRequestFilter(tableScanBaseQuery, "false") +
                        ".aminoAcidMutations(minProportion:=0.1, sequenceNames:=${set(it)}).schema()",
                    view,
                )
            }
        }
        if (view.supports(ViewCapability.INSERTIONS)) {
            view.referenceGenomeSchema.getNucleotideSequenceNames().takeIf { it.isNotEmpty() }?.let {
                probe(
                    "${applyRequestFilter(tableScanBaseQuery, "false")}" +
                        ".insertions(sequenceNames:=${set(it)}).schema()",
                    view,
                )
            }
            view.referenceGenomeSchema.getGeneNames().takeIf { it.isNotEmpty() }?.let {
                probe(
                    "${applyRequestFilter(tableScanBaseQuery, "false")}" +
                        ".aminoAcidInsertions(sequenceNames:=${set(it)}).schema()",
                    view,
                )
            }
        }
        if (view.supports(ViewCapability.SEQUENCES)) {
            val sequenceColumns = buildList {
                view.referenceGenomeSchema.getNucleotideSequenceNames().forEach {
                    add(it)
                    add("unaligned_$it")
                }
                addAll(view.referenceGenomeSchema.getGeneNames())
            }
            probe("${applyRequestFilter(view.baseQuery, "false")}.project(${set(sequenceColumns)}).schema()", view)
        }
        if (view.supports(ViewCapability.PHYLO_TREE)) {
            val treeFields = view.databaseConfig.schema.metadata.filter { it.isPhyloTreeField }.map { it.name }
            require(treeFields.isNotEmpty()) {
                "View '${view.viewName}' enables phyloTree but defines no phylogenetic tree metadata field"
            }
            treeFields.forEach {
                val filteredTableScanQuery = applyRequestFilter(tableScanBaseQuery, "false")
                probe("$filteredTableScanQuery.mostRecentCommonAncestor('$it').schema()", view)
                probe("$filteredTableScanQuery.phyloSubtree('$it').schema()", view)
            }
        }
    }

    private fun validateDeclaredSchema(
        view: ViewConfig,
        actualFields: List<SiloSchemaField>,
    ) {
        val actualByName = actualFields.associateBy { it.fieldName }
        view.databaseConfig.schema.metadata.forEach { metadata ->
            val actual = requireNotNull(actualByName[metadata.name]) {
                "View '${view.viewName}' declares metadata field '${metadata.name}' that is absent from its baseQuery"
            }
            require(actual.type in compatibleSiloTypes(metadata.type)) {
                "View '${view.viewName}' declares '${metadata.name}' as ${metadata.type}, but SILO reports ${actual.type}"
            }
            require(!metadata.generateIndex || actual.type == "INDEXED_STRING") {
                "View '${view.viewName}' declares '${metadata.name}' as indexed, but SILO reports ${actual.type}"
            }
        }
        require(view.databaseConfig.schema.primaryKey in actualByName) {
            "View '${view.viewName}' primary key '${view.databaseConfig.schema.primaryKey}' is absent from its baseQuery"
        }
    }

    private fun validateTableScanSchema(
        view: ViewConfig,
        actualFields: List<SiloSchemaField>,
    ) {
        val actualByName = actualFields.associateBy { it.fieldName }
        view.databaseConfig.schema.metadata.forEach { metadata ->
            val tableScanField = view.fieldAliases[metadata.name] ?: metadata.name
            val actual = requireNotNull(actualByName[tableScanField]) {
                "View '${view.viewName}' maps metadata field '${metadata.name}' to table-scan field " +
                    "'$tableScanField', which is absent from its tableScanQuery"
            }
            require(actual.type in compatibleSiloTypes(metadata.type)) {
                "View '${view.viewName}' declares '${metadata.name}' as ${metadata.type}, but its table-scan field " +
                    "'$tableScanField' has type ${actual.type}"
            }
            require(!metadata.generateIndex || actual.type == "INDEXED_STRING") {
                "View '${view.viewName}' declares '${metadata.name}' as indexed, but its table-scan field " +
                    "'$tableScanField' has type ${actual.type}"
            }
        }
    }

    private fun compatibleSiloTypes(type: MetadataType): Set<String> =
        when (type) {
            MetadataType.STRING -> setOf("STRING", "INDEXED_STRING")
            MetadataType.DATE -> setOf("DATE32")
            MetadataType.INT -> setOf("INT32", "INT64")
            MetadataType.FLOAT -> setOf("FLOAT", "DOUBLE")
            MetadataType.BOOLEAN -> setOf("BOOL")
        }

    private fun probe(
        query: String,
        view: ViewConfig,
    ) {
        querySchema(query, view.viewName)
    }

    private fun querySchema(
        query: String,
        viewName: String,
    ): List<SiloSchemaField> {
        val request = HttpRequest.newBuilder(siloUris.query)
            .header("Content-Type", "text/plain")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(query))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        require(response.statusCode() == 200) {
            "Failed to validate view '$viewName' with query '$query': SILO returned ${response.statusCode()} ${response.body()}"
        }
        return response.body().lineSequence().filter { it.isNotBlank() }.map {
            objectMapper.readValue<SiloSchemaField>(it)
        }.toList()
    }

    private fun set(values: List<String>) = values.joinToString(prefix = "{", postfix = "}", separator = ",")
}

private data class SiloSchemaField(
    val fieldName: String,
    val type: String,
)
