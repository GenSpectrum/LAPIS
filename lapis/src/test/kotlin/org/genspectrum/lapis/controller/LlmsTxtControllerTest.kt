package org.genspectrum.lapis.controller

import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class LlmsTxtControllerTest(
    @param:Autowired val mockMvc: MockMvc,
) {
    @Test
    fun `WHEN calling llms txt endpoint THEN returns 200 OK`() {
        mockMvc.perform(get("/llms.txt"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("text/plain;charset=UTF-8"))
            .andExpect(content().string(startsWith("# LAPIS")))
    }
}
