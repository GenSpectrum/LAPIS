package org.genspectrum.lapis.openApi

import jakarta.servlet.http.HttpServletRequest
import org.genspectrum.lapis.config.ViewCapability
import org.genspectrum.lapis.config.ViewConfig
import org.genspectrum.lapis.config.ViewRegistry
import org.genspectrum.lapis.util.YamlObjectMapper
import org.springdoc.webmvc.api.OpenApiWebMvcResource
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import java.util.Locale

@RestController
class ViewOpenApiController(
    private val openApiResource: OpenApiWebMvcResource,
    private val viewRegistry: ViewRegistry,
    private val objectMapper: ObjectMapper,
    private val yamlObjectMapper: YamlObjectMapper,
    private val resourceServerProperties: OAuth2ResourceServerProperties,
) {
    @GetMapping("/{view}/api-docs", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun json(
        @PathVariable view: String,
        request: HttpServletRequest,
    ): ByteArray = objectMapper.writeValueAsBytes(buildViewOpenApi(view, request))

    @GetMapping("/{view}/api-docs.yaml", produces = ["application/vnd.oai.openapi"])
    fun yaml(
        @PathVariable view: String,
        request: HttpServletRequest,
    ): ByteArray = yamlObjectMapper.objectMapper.writeValueAsBytes(buildViewOpenApi(view, request))

    @GetMapping("/{view}/swagger-ui/index.html", produces = [MediaType.TEXT_HTML_VALUE])
    @ResponseBody
    fun swaggerUi(
        @PathVariable view: String,
    ) = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <title>LAPIS $view - Swagger UI</title>
          <link rel="stylesheet" href="/swagger-ui/swagger-ui.css">
        </head>
        <body>
          <div id="swagger-ui"></div>
          <script src="/swagger-ui/swagger-ui-bundle.js"></script>
          <script>SwaggerUIBundle({url: '/$view/api-docs', dom_id: '#swagger-ui'});</script>
        </body>
        </html>
        """.trimIndent()

    private fun buildViewOpenApi(
        viewName: String,
        request: HttpServletRequest,
    ): ObjectNode {
        val view = viewRegistry.require(viewName)
        val generated = objectMapper.readTree(
            openApiResource.openapiJson(request, "/api-docs", Locale.getDefault()),
        ) as ObjectNode
        val viewSpecificOpenApiSchema = buildOpenApiSchema(
            sequenceFilterFields = view.sequenceFilterFields,
            databaseConfig = view.databaseConfig,
            referenceGenomeSchema = view.referenceGenomeSchema,
            resourceServerProperties = resourceServerProperties,
        )
        generated.set("components", objectMapper.valueToTree(viewSpecificOpenApiSchema.components))
        generated.set("paths", concretePaths(generated.get("paths") as ObjectNode, view))
        return generated
    }

    private fun concretePaths(
        paths: ObjectNode,
        view: ViewConfig,
    ): ObjectNode {
        val result = objectMapper.createObjectNode()
        paths.properties().forEach { (path, item) ->
            if (!path.startsWith("/{view}/")) {
                return@forEach
            }
            val concretePath = "/${view.viewName}/" + path.removePrefix("/{view}/")
            if (!isEnabled(concretePath, view)) {
                return@forEach
            }
            val pathItem = item.deepCopy() as ObjectNode
            removeViewParameter(pathItem)
            result.set(concretePath, pathItem)
        }
        return result
    }

    private fun removeViewParameter(pathItem: ObjectNode) {
        (pathItem.get("parameters") as? ArrayNode)?.removeIf { it.isViewParameter() }
        pathItem.properties().forEach { (_, operation) ->
            if (operation is ObjectNode) {
                (operation.get("parameters") as? ArrayNode)?.removeIf { it.isViewParameter() }
            }
        }
    }

    private fun JsonNode.isViewParameter() = get("name")?.asText() == "view"

    private fun isEnabled(
        path: String,
        view: ViewConfig,
    ): Boolean =
        when {
            path.contains("/component/") -> view.supports(ViewCapability.COMPONENTS)
            path.endsWith("/aggregated") || path.endsWith("/details") -> view.supports(ViewCapability.METADATA)
            path.contains("Mutations") -> view.supports(ViewCapability.MUTATIONS)
            path.contains("Insertions") -> view.supports(ViewCapability.INSERTIONS)
            path.contains("Sequences") -> view.supports(ViewCapability.SEQUENCES)
            path.endsWith("/mostRecentCommonAncestor") || path.endsWith("/phyloSubtree") ->
                view.supports(ViewCapability.PHYLO_TREE)
            else -> true
        }
}
