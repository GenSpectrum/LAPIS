package org.genspectrum.lapis.config

import jakarta.servlet.http.HttpServletRequest
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.util.YamlObjectMapper
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.readValue
import java.io.File

private val VIEW_NAME_REGEX = Regex("[a-z0-9]+(?:-[a-z0-9]+)*")
private val RESERVED_VIEW_NAMES = setOf("actuator", "error")
const val ACTIVE_VIEW_REQUEST_ATTRIBUTE = "org.genspectrum.lapis.activeView"

data class ViewsConfig(
    val views: List<ViewDefinition>,
)

data class ViewDefinition(
    val viewName: String,
    val baseQuery: String,
    val databaseConfig: String,
    val referenceGenome: String,
)

data class ViewConfig(
    val viewName: String,
    val baseQuery: String,
    val databaseConfig: DatabaseConfig,
    val referenceGenome: ReferenceGenome,
    val referenceGenomeSchema: ReferenceGenomeSchema,
    val sequenceFilterFields: SequenceFilterFields,
)

@Component
class ViewRegistry(
    @Value("\${lapis.viewsConfig.path}") manifestPath: String,
    yamlObjectMapper: YamlObjectMapper,
    databaseConfigValidator: DatabaseConfigValidator,
) {
    val views: Map<String, ViewConfig> = run {
        val manifestFile = File(manifestPath).absoluteFile
        val manifest = yamlObjectMapper.objectMapper.readValue<ViewsConfig>(manifestFile)
        require(manifest.views.isNotEmpty()) { "The views config must define at least one view" }

        val duplicateNames = manifest.views.groupingBy { it.viewName }.eachCount().filterValues { it > 1 }.keys
        require(duplicateNames.isEmpty()) { "Duplicate view names: ${duplicateNames.joinToString()}" }

        manifest.views.associate { definition ->
            validateDefinition(definition)
            val databaseConfigFile = resolveRelativeTo(manifestFile, definition.databaseConfig)
            val referenceGenomeFile = resolveRelativeTo(manifestFile, definition.referenceGenome)
            val databaseConfig = yamlObjectMapper.objectMapper.readValue<DatabaseConfig>(databaseConfigFile)
                .let { databaseConfigValidator.validate(it) }
            val referenceGenome = ReferenceGenome.readFromFile(referenceGenomeFile.path)
            val referenceGenomeSchema = ReferenceGenomeSchema(
                nucleotideSequences = referenceGenome.nucleotideSequences.map { ReferenceSequenceSchema(it.name) },
                genes = referenceGenome.genes.map { ReferenceSequenceSchema(it.name) },
            )

            definition.viewName to ViewConfig(
                viewName = definition.viewName,
                baseQuery = definition.baseQuery.trim(),
                databaseConfig = databaseConfig,
                referenceGenome = referenceGenome,
                referenceGenomeSchema = referenceGenomeSchema,
                sequenceFilterFields = SequenceFilterFields.fromDatabaseConfig(databaseConfig),
            )
        }
    }

    val siloClientThreadCount = run {
        val threadCounts = views.values.map { it.databaseConfig.siloClientThreadCount }.distinct()
        require(threadCounts.size == 1) {
            "All views must configure the same siloClientThreadCount because the SILO client is shared"
        }
        threadCounts.single()
    }

    fun get(viewName: String): ViewConfig? = views[viewName]

    fun require(viewName: String): ViewConfig = get(viewName) ?: throw ViewNotFoundException(viewName)

    fun first(): ViewConfig = views.values.first()

    private fun validateDefinition(definition: ViewDefinition) {
        require(VIEW_NAME_REGEX.matches(definition.viewName)) {
            "Invalid view name '${definition.viewName}': expected a lowercase URL slug"
        }
        require(definition.viewName !in RESERVED_VIEW_NAMES) { "Reserved view name: ${definition.viewName}" }
        require(definition.baseQuery.isNotBlank()) { "View '${definition.viewName}' has an empty baseQuery" }
    }

    private fun resolveRelativeTo(
        manifestFile: File,
        configuredPath: String,
    ): File {
        val configuredFile = File(configuredPath)
        return if (configuredFile.isAbsolute) configuredFile else File(manifestFile.parentFile, configuredPath)
    }
}

@Component
class ActiveView(
    private val requestProvider: ObjectProvider<HttpServletRequest>,
) {
    val configOrNull: ViewConfig?
        get() = runCatching {
            requestProvider.ifAvailable?.getAttribute(ACTIVE_VIEW_REQUEST_ATTRIBUTE) as? ViewConfig
        }.getOrNull()

    val config: ViewConfig
        get() = configOrNull ?: throw BadRequestException("No LAPIS view selected")

    val databaseConfig: DatabaseConfig get() = config.databaseConfig
    val referenceGenome: ReferenceGenome get() = config.referenceGenome
    val referenceGenomeSchema: ReferenceGenomeSchema get() = config.referenceGenomeSchema
    val sequenceFilterFields: SequenceFilterFields get() = config.sequenceFilterFields
}

class ViewNotFoundException(
    viewName: String,
) : RuntimeException("Unknown LAPIS view '$viewName'")
