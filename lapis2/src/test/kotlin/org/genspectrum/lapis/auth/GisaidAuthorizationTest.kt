package org.genspectrum.lapis.auth

import com.ninjasquad.springmockk.MockkBean
import io.mockk.MockKAnnotations
import io.mockk.MockKMatcherScope
import io.mockk.every
import org.genspectrum.lapis.controller.LapisController
import org.genspectrum.lapis.response.AggregatedResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest(properties = ["lapis.databaseConfig.path=src/test/resources/config/gisaidDatabaseConfig.yaml"])
@AutoConfigureMockMvc
class GisaidAuthorizationTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var lapisController: LapisController

    private fun MockKMatcherScope.validControllerCall() = lapisController.aggregated(any())
    private val validRoute = "/aggregated"

    @BeforeEach
    fun setUp() {
        every { validControllerCall() } returns AggregatedResponse(1)

        MockKAnnotations.init(this)
    }

    @Test
    fun `given no access key in request to GISAID instance, then access is denied`() {
        mockMvc.perform(MockMvcRequestBuilders.get(validRoute))
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    """
                    {
                      "title": "Forbidden",
                      "message": "An access key is required to access this endpoint."
                    }
                    """,
                ),
            )
    }
}
