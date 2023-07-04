package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.MockKAnnotations
import io.mockk.MockKMatcherScope
import io.mockk.every
import org.genspectrum.lapis.model.SiloNotImplementedError
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.silo.SiloException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class ExceptionHandlerTest(@Autowired val mockMvc: MockMvc) {
    @MockkBean
    lateinit var lapisController: LapisController

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
    }

    private val validRoute = "/aggregated"
    private fun MockKMatcherScope.validControllerCall() = lapisController.aggregated(any(), any())
    private val validResponse = emptyList<AggregationData>()

    @Test
    fun `throw NOT_FOUND(404) when route is not found`() {
        every { validControllerCall() } returns validResponse

        mockMvc.perform(get("/notAValidRoute"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `throw INTERNAL_SERVER_ERROR(500) with additional info for any non specific error`() {
        every { validControllerCall() } throws Exception("SomeMessage")

        mockMvc.perform(get(validRoute))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(
                content().json(
                    """
                    {
                      "title":"Unexpected error",
                      "message":"SomeMessage"
                    }
                    """,
                ),
            )
    }

    @Test
    fun `throw INTERNAL_SERVER_ERROR(500) with additional info for SiloExceptions`() {
        every { validControllerCall() } throws SiloException("SomeMessage", Exception("SomeCause"))

        mockMvc.perform(get(validRoute))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(
                content().json(
                    """
                    {
                      "title":"Silo error",
                      "message":"SomeMessage"
                    }
                    """,
                ),
            )
    }

    @Test
    fun `throw BAD_REQUEST(400) with additional info for bad requests`() {
        every { validControllerCall() } throws IllegalArgumentException("SomeMessage")

        mockMvc.perform(get(validRoute))
            .andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(
                content().json(
                    """
                    {
                      "title":"Bad request",
                      "message":"SomeMessage"
                    }
                    """,
                ),
            )
    }

    @Test
    fun `throw NOT_IMPLEMENTED(501) with additional info for request of a not implemented resource in SILO`() {
        every { validControllerCall() } throws SiloNotImplementedError("SomeMessage", Exception("SomeCause"))

        mockMvc.perform(get(validRoute))
            .andExpect(status().isNotImplemented)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(
                content().json(
                    """
                    {
                      "title":"Not implemented",
                      "message":"SomeMessage"
                    }
                    """,
                ),
            )
    }
}
