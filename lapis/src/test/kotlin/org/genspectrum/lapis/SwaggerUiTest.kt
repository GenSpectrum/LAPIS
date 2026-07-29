package org.genspectrum.lapis

import org.genspectrum.lapis.controller.AMINO_ACID_MUTATIONS_OVER_TIME_ROUTE
import org.genspectrum.lapis.controller.NUCLEOTIDE_MUTATIONS_OVER_TIME_ROUTE
import org.genspectrum.lapis.controller.QUERIES_OVER_TIME_ROUTE
import org.genspectrum.lapis.controller.SampleRoute
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.module.kotlin.kotlinModule

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class SwaggerUiTest(
    @param:Autowired val mockMvc: MockMvc,
) {
    @Test
    fun `Swagger UI endpoint is reachable`() {
        mockMvc.perform(get("/test/swagger-ui/index.html"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith("text/html"))
            .andExpect(content().string(containsString("Swagger UI")))
    }

    @Test
    fun `unprefixed API docs are unavailable`() {
        mockMvc.perform(get("/api-docs"))
            .andExpect(status().isNotFound)
        mockMvc.perform(get("/api-docs.yaml"))
            .andExpect(status().isNotFound)
        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `view-specific JSON API docs are available`() {
        val result = mockMvc.perform(get("/test/api-docs"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("\$.openapi").exists())
            .andExpect(jsonPath("\$.paths./test/sample/aggregated").exists())
            .andExpect(jsonPath("\$.paths./test/sample/alignedNucleotideSequences").exists())
            .andExpect(jsonPath("\$.paths./actuator/health").exists())
            .andExpect(jsonPath("\$.paths./sample/aggregated").doesNotExist())
            .andReturn()

        assertFalse(result.response.contentAsString.contains(Regex(":null[,}]")))
        val json = tools.jackson.module.kotlin.jacksonObjectMapper().readTree(result.response.contentAsString)
        val paths = json.get("paths")
        SampleRoute.entries.forEach {
            assertTrue(paths.has("/test/sample${it.pathSegment}"), "Missing ${it.pathSegment}")
        }
        listOf(QUERIES_OVER_TIME_ROUTE, NUCLEOTIDE_MUTATIONS_OVER_TIME_ROUTE, AMINO_ACID_MUTATIONS_OVER_TIME_ROUTE)
            .forEach { assertTrue(paths.has("/test/component$it"), "Missing $it") }
        assertTrue(paths.has("/test/query/parse"))
        val schemas = json.get("components").get("schemas")
        assertTrue(schemas.has("SequenceFilters"))
        assertTrue(schemas.has("DatabaseConfig"))
        assertTrue(schemas.has("LapisInfo"))
        assertTrue(schemas.has("ReferenceGenome"))
        assertTrue(
            schemas.get("AggregatedPostRequest").at("/properties/orderBy/oneOf/0/type").stringValue() == "array",
        )

        val alignedNucleotideSequences = json.get("paths").get("/test/sample/alignedNucleotideSequences")
        assertTrue(
            alignedNucleotideSequences.get("get").get("parameters").any {
                it.get("name")?.stringValue() == "segments"
            },
        )
        assertTrue(
            alignedNucleotideSequences.get("post").at("/requestBody/content/application~1json/schema/\$ref")
                .stringValue().endsWith("/AllNucleotideSequenceRequest"),
        )
        assertFalse(
            alignedNucleotideSequences.get("get").at("/responses/200/content/application~1json/schema").has("oneOf"),
        )
        assertTrue(
            alignedNucleotideSequences.get("post").get("operationId").stringValue() ==
                "postAllAlignedNucleotideSequences",
        )
        assertTrue(
            alignedNucleotideSequences.get("post").get("tags").single().stringValue() ==
                "multi-segmented-sequence-controller",
        )
        assertTrue(
            paths.get("/test/sample/alignedNucleotideSequences/{segment}").get("post").get("operationId")
                .stringValue() == "postAlignedNucleotideSequence",
        )
    }

    @Test
    fun `view-specific API docs use the forwarded public origin`() {
        mockMvc.perform(
            get("/test/api-docs")
                .header("X-Forwarded-Host", "lapis.example.org")
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Prefix", "/lapis"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.servers[0].url").value("https://lapis.example.org/lapis"))
    }

    @Test
    fun `view-specific Swagger UI retains the configured operation order`() {
        mockMvc.perform(get("/test/swagger-ui/index.html"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("operationsSorter: 'alpha'")))
    }

    @Test
    fun `unknown view routes return not found`() {
        mockMvc.perform(get("/missing/sample/aggregated"))
            .andExpect(status().isNotFound)
        mockMvc.perform(get("/missing/"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `view-specific YAML API docs are available`() {
        val result = mockMvc.perform(get("/test/api-docs.yaml"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/vnd.oai.openapi"))
            .andReturn()

        val yamlMapper = YAMLMapper.builder().addModule(kotlinModule()).build()
        val yaml = yamlMapper.readTree(result.response.contentAsString)
        assertTrue(yaml.has("openapi"))
        assertTrue(yaml.get("paths").has("/test/sample/aggregated"))
        assertTrue(!yaml.get("paths").has("/sample/aggregated"))
    }
}

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "lapis.viewsConfig.path=src/test/resources/config/views-test-fasta-single.yaml",
        "lapis.validateViewsOnStartup=false",
    ],
)
@AutoConfigureMockMvc
class SingleSegmentedViewSwaggerUiTest(
    @param:Autowired val mockMvc: MockMvc,
) {
    @Test
    fun `single-segment view docs expose the single-segment nucleotide operations`() {
        val result = mockMvc.perform(get("/test/api-docs"))
            .andExpect(status().isOk)
            .andReturn()
        val json = tools.jackson.module.kotlin.jacksonObjectMapper().readTree(result.response.contentAsString)
        val paths = json.get("paths")
        val operation = paths.get("/test/sample/alignedNucleotideSequences")

        assertFalse(operation.get("get").get("parameters").any { it.get("name")?.stringValue() == "segments" })
        assertTrue(
            operation.get("post").at("/requestBody/content/application~1json/schema/\$ref")
                .stringValue().endsWith("/NucleotideSequenceRequest"),
        )
        assertFalse(operation.get("get").at("/responses/200/content/application~1json/schema").has("oneOf"))
        assertTrue(operation.get("post").get("operationId").stringValue() == "postAlignedNucleotideSequence")
        assertTrue(
            operation.get("post").get("tags").single().stringValue() == "single-segmented-sequence-controller",
        )
        assertFalse(paths.has("/test/sample/alignedNucleotideSequences/{segment}"))
    }
}
