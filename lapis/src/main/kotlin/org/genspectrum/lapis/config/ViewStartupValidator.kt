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
        val baseSchema = querySchema("${applyRequestFilter(view.baseQuery, "true")}.schema()", view.viewName)
        validateDeclaredSchema(view, baseSchema)
        if (view.supports(ViewCapability.SEQUENCES)) {
            val requiredSequenceColumns = buildSet {
                view.referenceGenomeSchema.getNucleotideSequenceNames().forEach {
                    add(it)
                    add("unaligned_$it")
                }
                addAll(view.referenceGenomeSchema.getGeneNames())
            }
            val missingColumns = requiredSequenceColumns - baseSchema.map { it.fieldName }.toSet()
            require(missingColumns.isEmpty()) {
                "View '${view.viewName}' enables sequences but its baseQuery is missing: " +
                    missingColumns.joinToString()
            }
        }
        view.tableScanQuery?.let {
            validateTableScanSchema(
                view = view,
                actualFields = querySchema("${applyRequestFilter(it, "true")}.schema()", view.viewName),
            )
        }
        if (view.supports(ViewCapability.PHYLO_TREE)) {
            val treeFields = view.databaseConfig.schema.metadata.filter { it.isPhyloTreeField }.map { it.name }
            require(treeFields.isNotEmpty()) {
                "View '${view.viewName}' enables phyloTree but defines no phylogenetic tree metadata field"
            }
        }
    }

    private fun validateDeclaredSchema(
        view: ViewConfig,
        actualFields: List<SiloSchemaField>,
    ) {
        validateMetadataSchema(view, actualFields, "baseQuery") { it.name }
        val actualByName = actualFields.map { it.fieldName }.toSet()
        require(view.databaseConfig.schema.primaryKey in actualByName) {
            "View '${view.viewName}' primary key '${view.databaseConfig.schema.primaryKey}' is absent from its baseQuery"
        }
    }

    private fun validateTableScanSchema(
        view: ViewConfig,
        actualFields: List<SiloSchemaField>,
    ) = validateMetadataSchema(view, actualFields, "tableScanQuery") { view.fieldAliases[it.name] ?: it.name }

    private fun validateMetadataSchema(
        view: ViewConfig,
        actualFields: List<SiloSchemaField>,
        queryName: String,
        fieldName: (DatabaseMetadata) -> String,
    ) {
        val actualByName = actualFields.associateBy { it.fieldName }
        view.databaseConfig.schema.metadata.forEach { metadata ->
            val queryField = fieldName(metadata)
            val actual = requireNotNull(actualByName[queryField]) {
                "View '${view.viewName}' metadata field '${metadata.name}' resolves to '$queryField', " +
                    "which is absent from its $queryName"
            }
            require(actual.type in compatibleSiloTypes(metadata.type)) {
                "View '${view.viewName}' declares '${metadata.name}' as ${metadata.type}, " +
                    "but '$queryField' in its $queryName has type ${actual.type}"
            }
            require(!metadata.generateIndex || actual.type == "INDEXED_STRING") {
                "View '${view.viewName}' declares '${metadata.name}' as indexed, " +
                    "but '$queryField' in its $queryName has type ${actual.type}"
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
}

private data class SiloSchemaField(
    val fieldName: String,
    val type: String,
)
