package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.model.AggregatedModel
import org.genspectrum.lapis.response.AggregatedResponse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest
class LapisControllerTest(@Autowired val mockMvc: MockMvc) {
    @MockkBean
    lateinit var aggregatedModelMock: AggregatedModel

    @Test
    fun aggregated() {
        every { aggregatedModelMock.handleRequest(mapOf("country" to "Switzerland")) } returns AggregatedResponse(0)

        mockMvc.perform(get("/aggregated?country=Switzerland"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.count").value(0))
    }

    @Test
    fun `post aggregated`() {
        every { aggregatedModelMock.handleRequest(mapOf("country" to "Switzerland")) } returns AggregatedResponse(0)

        val request = post("/aggregated")
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.count").value(0))
    }
}
