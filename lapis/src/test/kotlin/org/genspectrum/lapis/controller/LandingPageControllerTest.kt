package org.genspectrum.lapis.controller

import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.http.MediaType.TEXT_HTML_VALUE
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class LandingPageControllerTest(
    @param:Autowired val mockMvc: MockMvc,
) {
    @Test
    fun `WHEN calling landing page THEN returns hello json`() {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(jsonPath("['Swagger UI']").exists())
    }

    @Test
    fun `WHEN calling landing page as browser THEN return hello html`() {
        mockMvc.perform(get("/").accept(TEXT_HTML))
            .andExpect(status().isOk)
            .andExpect(content().contentType("$TEXT_HTML_VALUE;charset=UTF-8"))
            .andExpect(content().string(startsWith("<!DOCTYPE html>")))
    }

    @Test
    fun `WHEN calling llms txt endpoint THEN returns 200 OK`() {
        mockMvc.perform(get("/llms.txt"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("text/plain;charset=UTF-8"))
            .andExpect(content().string(startsWith("# LAPIS")))
    }
}
