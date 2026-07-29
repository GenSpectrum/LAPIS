package org.genspectrum.lapis.config

import com.fasterxml.jackson.annotation.JsonProperty
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
    val capabilities: Set<ViewCapability>,
)

enum class ViewCapability {
    @JsonProperty("metadata")
    METADATA,

    @JsonProperty("mutations")
    MUTATIONS,

    @JsonProperty("insertions")
    INSERTIONS,

    @JsonProperty("sequences")
    SEQUENCES,

    @JsonProperty("phyloTree")
    PHYLO_TREE,

    @JsonProperty("components")
    COMPONENTS,
}

data class ViewConfig(
    val viewName: String,
    val baseQuery: String,
    val capabilities: Set<ViewCapability>,
    val databaseConfig: DatabaseConfig,
    val referenceGenome: ReferenceGenome,
    val referenceGenomeSchema: ReferenceGenomeSchema,
    val sequenceFilterFields: SequenceFilterFields,
) {
    fun supports(capability: ViewCapability) = capability in capabilities

    fun asEffectiveConfig() =
        EffectiveViewConfig(
            viewName = viewName,
            baseQuery = baseQuery,
            capabilities = capabilities,
            schema = databaseConfig.schema,
            defaultNucleotideSequence = databaseConfig.defaultNucleotideSequence,
            defaultAminoAcidSequence = databaseConfig.defaultAminoAcidSequence,
            siloClientThreadCount = databaseConfig.siloClientThreadCount,
        )
}

data class EffectiveViewConfig(
    val viewName: String,
    val baseQuery: String,
    val capabilities: Set<ViewCapability>,
    val schema: DatabaseSchema,
    val defaultNucleotideSequence: String?,
    val defaultAminoAcidSequence: String?,
    val siloClientThreadCount: Int,
)

@Component
class ViewRegistry(
    @Value("\${lapis.viewsConfig.path}") manifestPath: String,
    @Value("\${lapis.legacyRoutesEnabled:false}") legacyRoutesEnabled: Boolean,
    @Value("\${referenceGenome.segments:}") legacySegments: String,
    @Value("\${referenceGenome.genes:}") legacyGenes: String,
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
            val referenceGenomeSchema = when {
                legacyRoutesEnabled && legacySegments.isNotBlank() -> ReferenceGenomeSchema(
                    nucleotideSequences = legacySegments.split(',').filter { it.isNotBlank() }
                        .map(::ReferenceSequenceSchema),
                    genes = legacyGenes.split(',').filter { it.isNotBlank() }.map(::ReferenceSequenceSchema),
                )
                else -> ReferenceGenomeSchema(
                    nucleotideSequences = referenceGenome.nucleotideSequences.map { ReferenceSequenceSchema(it.name) },
                    genes = referenceGenome.genes.map { ReferenceSequenceSchema(it.name) },
                )
            }

            definition.viewName to ViewConfig(
                viewName = definition.viewName,
                baseQuery = definition.baseQuery.trim(),
                capabilities = definition.capabilities,
                databaseConfig = databaseConfig,
                referenceGenome = referenceGenome,
                referenceGenomeSchema = referenceGenomeSchema,
                sequenceFilterFields = SequenceFilterFields.fromDatabaseConfig(databaseConfig),
            )
        }
    }

    init {
        val threadCounts = views.values.map { it.databaseConfig.siloClientThreadCount }.distinct()
        require(threadCounts.size == 1) {
            "All views must configure the same siloClientThreadCount because the SILO client is shared"
        }
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
        require(definition.capabilities.isNotEmpty()) { "View '${definition.viewName}' must declare capabilities" }
        require(
            ViewCapability.COMPONENTS !in definition.capabilities ||
                setOf(ViewCapability.METADATA, ViewCapability.MUTATIONS).all { it in definition.capabilities },
        ) { "View '${definition.viewName}' requires metadata and mutations when components are enabled" }
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
    private val viewRegistry: ViewRegistry,
    @Value("\${lapis.legacyRoutesEnabled:false}") private val legacyRoutesEnabled: Boolean,
) {
    val config: ViewConfig
        get() = runCatching {
            requestProvider.ifAvailable?.getAttribute(ACTIVE_VIEW_REQUEST_ATTRIBUTE) as? ViewConfig
        }.getOrNull()
            ?: if (legacyRoutesEnabled) viewRegistry.first() else throw BadRequestException("No LAPIS view selected")

    val databaseConfig: DatabaseConfig get() = config.databaseConfig
    val referenceGenome: ReferenceGenome get() = config.referenceGenome
    val referenceGenomeSchema: ReferenceGenomeSchema get() = config.referenceGenomeSchema
    val sequenceFilterFields: SequenceFilterFields get() = config.sequenceFilterFields
}

class ViewNotFoundException(
    viewName: String,
) : RuntimeException("Unknown LAPIS view '$viewName'")
