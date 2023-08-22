package org.genspectrum.lapis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class SwaggerUiTest(@Autowired val mockMvc: MockMvc) {

    @Test
    fun `Swagger UI endpoint is reachable`() {
        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("text/html"))
            .andExpect(content().string(containsString("Swagger UI")))
    }

    @Test
    fun `JSON API docs are available`() {
        mockMvc.perform(get("/api-docs"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("\$.openapi").exists())
            .andExpect(jsonPath("\$.paths./aggregated").exists())
    }

    @Test
    fun `YAML API docs are available`() {
        val result = mockMvc.perform(get("/api-docs.yaml"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/vnd.oai.openapi"))
            .andReturn()

        val objectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
        val yaml = objectMapper.readTree(result.response.contentAsString)
        assertTrue(yaml.has("openapi"))
        assertTrue(yaml.get("paths").has("/aggregated"))
    }
}

@SpringBootTest(properties = ["lapis.base-url=/base/url"])
@AutoConfigureMockMvc
class SwaggerUiWithBasePathTest(@Autowired val mockMvc: MockMvc) {

    @Test
    fun `JSON API docs are available with base url prefix`() {
        mockMvc.perform(get("/base/url/api-docs"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("\$.openapi").exists())
            .andExpect(jsonPath("\$.paths./aggregated").exists())
    }

    @Test
    fun `swagger config is available with the base url prefix`() {
        mockMvc.perform(get("/base/url/api-docs/swagger-config"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
    }

    @Test
    fun `API docs are available without the base url prefix because a proxy strips it away`() {
        mockMvc.perform(get("/api-docs"))
            .andExpect(status().isOk)
            .andExpect(forwardedUrl("/base/url/api-docs"))
    }

    @Test
    fun `swagger config is available without the base url prefix because a proxy strips it away`() {
        mockMvc.perform(get("/api-docs/swagger-config"))
            .andExpect(status().isOk)
            .andExpect(forwardedUrl("/base/url/api-docs/swagger-config"))
    }
}
