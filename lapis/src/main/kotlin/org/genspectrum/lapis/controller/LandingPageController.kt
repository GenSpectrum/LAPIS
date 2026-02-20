package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Hidden
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody

@Hidden
@Controller
class LandingPageController(
    @Value("\${lapis.docs.url:}") private val lapisDocsUrl: String,
    private val databaseConfig: DatabaseConfig,
    private val referenceGenomeSchema: ReferenceGenomeSchema,
) {
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

    @GetMapping("/", produces = [MediaType.TEXT_HTML_VALUE])
    fun indexHtml(model: Model): String {
        populateModel(model)
        return "index"
    }

    @GetMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun indexJson() = links + ("Instance name" to databaseConfig.schema.instanceName)

    @GetMapping("/llms.txt", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun llmsTxt(model: Model): String {
        populateModel(model)
        return "llms.txt"
    }

    private fun populateModel(model: Model) {
        model.addAttribute("instanceName", databaseConfig.schema.instanceName)
        model.addAttribute("metadataFields", databaseConfig.schema.metadata.joinToString(", ") { it.name })
        model.addAttribute("genes", referenceGenomeSchema.getGeneNames().joinToString(", "))
        model.addAttribute(
            "segments",
            if (!referenceGenomeSchema.isSingleSegmented()) {
                referenceGenomeSchema.getNucleotideSequenceNames().joinToString(", ")
            } else {
                ""
            },
        )
        model.addAttribute("docsUrl", lapisDocsUrl)
    }
}
