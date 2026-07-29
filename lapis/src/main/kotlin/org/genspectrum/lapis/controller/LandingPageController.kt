package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Hidden
import org.genspectrum.lapis.config.ActiveView
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.config.ViewRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody

@Hidden
@Controller
class LandingPageController(
    @param:Value("\${lapis.docs.url:}") private val lapisDocsUrl: String,
    private val activeView: ActiveView,
) {
    private val databaseConfig get() = activeView.databaseConfig
    private val referenceGenomeSchema get() = activeView.referenceGenomeSchema
    private val links = buildMap {
        if (lapisDocsUrl.isNotBlank()) {
            put("Documentation", lapisDocsUrl)
        }
        put("Swagger UI", "swagger-ui/index.html")
        put("OpenAPI specification JSON", "api-docs")
        put("OpenAPI specification YAML", "api-docs.yaml")
        put("llms.txt - instructions for LLM agents how to use LAPIS", "llms.txt")
        put("GitHub", "https://github.com/GenSpectrum/LAPIS")
    }

    @GetMapping("/{view}/", produces = [MediaType.TEXT_HTML_VALUE])
    fun indexHtml(model: Model): String {
        populateModel(model)
        return "index"
    }

    @GetMapping("/{view}/", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun indexJson() = links + ("Instance name" to databaseConfig.schema.instanceName)

    @GetMapping("/{view}/llms.txt", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun llmsTxt(model: Model): String {
        populateLlmsModel(model)
        return "llms.txt"
    }

    private fun populateModel(model: Model) {
        model.addAttribute("instanceName", databaseConfig.schema.instanceName)
        model.addAttribute("docsUrl", lapisDocsUrl)
    }

    private fun populateLlmsModel(model: Model) {
        model.addAttribute("instanceName", databaseConfig.schema.instanceName)
        model.addAttribute("docsUrl", lapisDocsUrl)

        model.addAttribute("isSingleSegmented", referenceGenomeSchema.isSingleSegmented())
        model.addAttribute("segmentNames", referenceGenomeSchema.getNucleotideSequenceNames())
        model.addAttribute("segmentCount", referenceGenomeSchema.getNucleotideSequenceNames().size)
        model.addAttribute("geneNames", referenceGenomeSchema.getGeneNames())
        model.addAttribute("firstGene", referenceGenomeSchema.getGeneNames().firstOrNull())
        model.addAttribute(
            "segmentMutationPrefix",
            referenceGenomeSchema.getNucleotideSequenceNames().firstOrNull()?.let { "$it:" },
        )

        model.addAttribute("metadataFields", databaseConfig.schema.metadata)
        model.addAttribute("stringField", getFirstFieldOfType(MetadataType.STRING))
        model.addAttribute(
            "lineageField",
            databaseConfig.schema.metadata
                .firstOrNull { it.generateLineageIndex != null }
                ?.name,
        )
        model.addAttribute("dateField", getFirstFieldOfType(MetadataType.DATE))
        model.addAttribute("intField", getFirstFieldOfType(MetadataType.INT))
        model.addAttribute("floatField", getFirstFieldOfType(MetadataType.FLOAT))
        model.addAttribute("booleanField", getFirstFieldOfType(MetadataType.BOOLEAN))
        model.addAttribute("hasPhyloTreeField", databaseConfig.schema.metadata.any { it.isPhyloTreeField })
    }

    private fun getFirstFieldOfType(metadataType: MetadataType): String? =
        databaseConfig.schema.metadata
            .firstOrNull { it.type == metadataType && it.name != databaseConfig.schema.primaryKey }
            ?.name
            ?: databaseConfig.schema.metadata.firstOrNull { it.type == metadataType }?.name
}

@Hidden
@Controller
class RootDiscoveryController(
    private val viewRegistry: ViewRegistry,
) {
    @GetMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun indexJson() =
        mapOf(
            "views" to viewRegistry.views.values.map {
                mapOf(
                    "viewName" to it.viewName,
                    "instanceName" to it.databaseConfig.schema.instanceName,
                    "url" to "/${it.viewName}/",
                )
            },
        )

    @GetMapping("/", produces = [MediaType.TEXT_HTML_VALUE])
    @ResponseBody
    fun indexHtml() =
        buildString {
            append(
                "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">" +
                    "<title>LAPIS views</title></head><body>",
            )
            append("<h1>LAPIS views</h1><ul>")
            viewRegistry.views.values.forEach {
                append(
                    "<li><a href=\"/${it.viewName}/\">${it.viewName} - ${it.databaseConfig.schema.instanceName}</a></li>",
                )
            }
            append("</ul></body></html>")
        }
}
