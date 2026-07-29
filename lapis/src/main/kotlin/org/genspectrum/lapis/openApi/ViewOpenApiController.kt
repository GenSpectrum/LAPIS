package org.genspectrum.lapis.openApi

import io.swagger.v3.core.util.Json31
import io.swagger.v3.oas.annotations.Hidden
import jakarta.servlet.http.HttpServletRequest
import org.genspectrum.lapis.config.ViewConfig
import org.genspectrum.lapis.config.ViewRegistry
import org.genspectrum.lapis.controller.ALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.ALIGNED_SINGLE_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.ALL_ALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.ALL_UNALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.UNALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.controller.UNALIGNED_SINGLE_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION
import org.genspectrum.lapis.util.YamlObjectMapper
import org.springdoc.webmvc.api.OpenApiWebMvcResource
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import java.util.Locale

@RestController
@Hidden
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
          <link rel="stylesheet" href="../../swagger-ui/swagger-ui.css">
        </head>
        <body>
          <div id="swagger-ui"></div>
          <script src="../../swagger-ui/swagger-ui-bundle.js"></script>
          <script>SwaggerUIBundle({url: '../api-docs', dom_id: '#swagger-ui', operationsSorter: 'alpha'});</script>
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
        setServerUrl(generated, request)
        val viewSpecificOpenApiSchema = buildOpenApiSchema(
            sequenceFilterFields = view.sequenceFilterFields,
            databaseConfig = view.databaseConfig,
            referenceGenomeSchema = view.referenceGenomeSchema,
            resourceServerProperties = resourceServerProperties,
        )
        mergeComponents(
            generated = generated,
            viewSpecific = objectMapper.readTree(
                Json31.mapper().writeValueAsBytes(viewSpecificOpenApiSchema.components),
            )
                as ObjectNode,
        )
        generated.set("paths", concretePaths(generated.get("paths") as ObjectNode, view))
        setAggregatedFieldsDescription(generated, view)
        configureNucleotideSequenceOperations(generated, view)
        return generated
    }

    private fun setServerUrl(
        openApi: ObjectNode,
        request: HttpServletRequest,
    ) {
        val server = (openApi.get("servers") as? ArrayNode)?.firstOrNull() as? ObjectNode ?: return
        server.put("url", ServletUriComponentsBuilder.fromContextPath(request).build().toUriString())
    }

    private fun mergeComponents(
        generated: ObjectNode,
        viewSpecific: ObjectNode,
    ) {
        val generatedComponents = (generated.get("components") as? ObjectNode)
            ?: objectMapper.createObjectNode().also { generated.set("components", it) }
        viewSpecific.properties().forEach { (sectionName, viewSpecificSection) ->
            if (viewSpecificSection !is ObjectNode) {
                generatedComponents.set(sectionName, viewSpecificSection)
                return@forEach
            }
            val generatedSection = (generatedComponents.get(sectionName) as? ObjectNode)
                ?: objectMapper.createObjectNode().also { generatedComponents.set(sectionName, it) }
            viewSpecificSection.properties().forEach { (name, value) -> generatedSection.set(name, value) }
        }
    }

    private fun concretePaths(
        paths: ObjectNode,
        view: ViewConfig,
    ): ObjectNode {
        val result = objectMapper.createObjectNode()
        paths.properties().forEach { (path, item) ->
            if (!path.startsWith("/{view}/")) {
                result.set(path, item.deepCopy())
                return@forEach
            }
            val concretePath = "/${view.viewName}/" + path.removePrefix("/{view}/")
            if (concretePath.contains("NucleotideSequences/{segment}") &&
                view.referenceGenomeSchema.isSingleSegmented()
            ) {
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

    private fun JsonNode.isViewParameter() = get("name")?.stringValue() == "view"

    private fun setAggregatedFieldsDescription(
        openApi: ObjectNode,
        view: ViewConfig,
    ) {
        val parameters = openApi.at("/paths/~1${view.viewName}~1sample~1aggregated/get/parameters") as? ArrayNode
            ?: return
        (parameters.firstOrNull { it.get("name")?.stringValue() == "fields" } as? ObjectNode)
            ?.put("description", aggregatedFieldsDescription(view.referenceGenomeSchema))
    }

    private fun configureNucleotideSequenceOperations(
        openApi: ObjectNode,
        view: ViewConfig,
    ) {
        val singleSegmented = view.referenceGenomeSchema.isSingleSegmented()
        listOf(
            "alignedNucleotideSequences" to if (singleSegmented) {
                ALIGNED_SINGLE_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION
            } else {
                ALL_ALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION
            },
            "unalignedNucleotideSequences" to if (singleSegmented) {
                UNALIGNED_SINGLE_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION
            } else {
                ALL_UNALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION
            },
        ).forEach { (route, description) ->
            val path = openApi.at("/paths/~1${view.viewName}~1sample~1$route") as? ObjectNode ?: return@forEach
            path.properties().forEach { (method, operationNode) ->
                val operation = operationNode as? ObjectNode ?: return@forEach
                operation.put("description", description)
                operation.put("operationId", nucleotideSequenceOperationId(route, method, singleSegmented))
                operation.putArray("tags").add(nucleotideSequenceControllerTag(singleSegmented))
                configureNucleotideSequenceResponse(operation, singleSegmented)
                if (method == "get" && singleSegmented) {
                    (operation.get("parameters") as? ArrayNode)?.removeIf {
                        it.get("name")?.stringValue() == "segments"
                    }
                }
                if (method == "post") {
                    val schemaName = if (singleSegmented) {
                        NUCLEOTIDE_SEQUENCE_REQUEST_SCHEMA
                    } else {
                        ALL_NUCLEOTIDE_SEQUENCE_REQUEST_SCHEMA
                    }
                    operation.at("/requestBody/content").properties().forEach { (_, mediaType) ->
                        (mediaType.get("schema") as? ObjectNode)?.put("\$ref", "#/components/schemas/$schemaName")
                    }
                }
            }
            if (!singleSegmented) {
                configureSegmentNucleotideSequenceOperations(openApi, view, route)
            }
        }
    }

    private fun configureSegmentNucleotideSequenceOperations(
        openApi: ObjectNode,
        view: ViewConfig,
        route: String,
    ) {
        val path = openApi.at("/paths/~1${view.viewName}~1sample~1$route~1{segment}") as? ObjectNode ?: return
        val alignment = if (route.startsWith("unaligned")) "Unaligned" else "Aligned"
        val description = if (route.startsWith("unaligned")) {
            UNALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION
        } else {
            ALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION
        }
        path.properties().forEach { (method, operationNode) ->
            val operation = operationNode as? ObjectNode ?: return@forEach
            operation.put("description", description)
            operation.put("operationId", "${method}$alignment" + "NucleotideSequence")
            operation.putArray("tags").add(nucleotideSequenceControllerTag(singleSegmented = false))
        }
    }

    private fun nucleotideSequenceControllerTag(singleSegmented: Boolean) =
        if (singleSegmented) "single-segmented-sequence-controller" else "multi-segmented-sequence-controller"

    private fun configureNucleotideSequenceResponse(
        operation: ObjectNode,
        singleSegmented: Boolean,
    ) {
        operation.at("/responses/200/content").properties().forEach { (_, mediaType) ->
            val mediaTypeObject = mediaType as? ObjectNode ?: return@forEach
            val schema = mediaTypeObject.get("schema") as? ObjectNode ?: return@forEach
            val alternatives = schema.get("oneOf") as? ArrayNode ?: return@forEach
            val desiredSchema = alternatives.firstOrNull {
                val text = it.toString()
                if (singleSegmented) {
                    "NucleotideSequencesResponse" in text && "AllNucleotideSequencesResponse" !in text
                } else {
                    "AllNucleotideSequencesResponse" in text
                }
            } ?: alternatives.get(if (singleSegmented) 1 else 0)
            mediaTypeObject.set("schema", desiredSchema.deepCopy())
        }
    }

    private fun nucleotideSequenceOperationId(
        route: String,
        method: String,
        singleSegmented: Boolean,
    ): String {
        val alignment = if (route.startsWith("unaligned")) "Unaligned" else "Aligned"
        val prefix = if (method == "get") "get" else "post"
        val all = if (singleSegmented) "" else "All"
        val suffix = if (method == "get" || !singleSegmented) "Sequences" else "Sequence"
        return "$prefix$all$alignment" + "Nucleotide$suffix"
    }
}
