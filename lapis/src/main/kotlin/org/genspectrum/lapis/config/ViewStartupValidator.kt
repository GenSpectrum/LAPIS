package org.genspectrum.lapis.config

import org.genspectrum.lapis.silo.SiloUris
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
        val baseSchema = querySchema("${view.baseQuery}.schema()", view.viewName)
        validateDeclaredSchema(view, baseSchema)
        val requiredSequenceColumns = buildSet {
            view.referenceGenomeSchema.getNucleotideSequenceNames().forEach {
                add(it)
                add("unaligned_$it")
            }
            addAll(view.referenceGenomeSchema.getGeneNames())
        }
        val missingColumns = requiredSequenceColumns - baseSchema.map { it.fieldName }.toSet()
        require(missingColumns.isEmpty()) {
            "View '${view.viewName}' baseQuery is missing sequence fields: ${missingColumns.joinToString()}"
        }
    }

    private fun validateDeclaredSchema(
        view: ViewConfig,
        actualFields: List<SiloSchemaField>,
    ) {
        validateMetadataSchema(view, actualFields, "baseQuery")
        val actualByName = actualFields.map { it.fieldName }.toSet()
        require(view.databaseConfig.schema.primaryKey in actualByName) {
            "View '${view.viewName}' primary key '${view.databaseConfig.schema.primaryKey}' is absent from its baseQuery"
        }
    }

    private fun validateMetadataSchema(
        view: ViewConfig,
        actualFields: List<SiloSchemaField>,
        queryName: String,
    ) {
        val actualByName = actualFields.associateBy { it.fieldName }
        view.databaseConfig.schema.metadata.forEach { metadata ->
            val queryField = metadata.name
            val actual = requireNotNull(actualByName[queryField]) {
                "View '${view.viewName}' metadata field '$queryField' is absent from its $queryName"
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
