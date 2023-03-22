package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.MockKAnnotations
import io.mockk.MockKMatcherScope
import io.mockk.every
import org.genspectrum.lapis.response.AggregatedResponse
import org.genspectrum.lapis.silo.SiloException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest
class ExceptionHandlerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var lapisController: LapisController

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
    }

    private val validRoute = "/aggregated"
    private fun MockKMatcherScope.validControllerCall() = lapisController.aggregated(any())
    private val validResponse = AggregatedResponse(1)

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
}
