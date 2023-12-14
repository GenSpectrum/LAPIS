package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.MockKAnnotations
import io.mockk.MockKMatcherScope
import io.mockk.every
import org.genspectrum.lapis.model.SiloNotImplementedError
import org.genspectrum.lapis.request.LapisInfo
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.silo.DataVersion
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
class ExceptionHandlerTest(
    @Autowired val mockMvc: MockMvc,
) {
    @MockkBean
    lateinit var lapisController: LapisController

    @MockkBean
    lateinit var dataVersion: DataVersion

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)

        every {
            dataVersion.dataVersion
        } returns "1234"
    }

    private val validRoute = "/aggregated"

    private fun MockKMatcherScope.validControllerCall() =
        lapisController.aggregated(any(), any(), any(), any(), any(), any(), any())

    private val validResponse = LapisResponse(emptyList<AggregationData>(), LapisInfo())

    @Test
    fun `throw NOT_FOUND(404) when route is not found`() {
        every { validControllerCall() } returns validResponse

        mockMvc.perform(get("/notAValidRoute"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `throw INTERNAL_SERVER_ERROR(500) with additional info for any non specific error`() {
        every { validControllerCall() } throws Exception("SomeMessage")

        mockMvc.perform(getSample(validRoute))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(
                content().json(
                    """
                    {
                        "error": {
                            "title": "Internal Server Error",
                            "detail": "SomeMessage"
                         },
                         "info": {
                            "dataVersion": "1234"
                         }
                    }
                    """,
                ),
            )
    }

    @Test
    fun `Passes through exception with status code from SILO`() {
        every { validControllerCall() } throws SiloException(123, "SomeTitle", "SomeMessage")

        mockMvc.perform(getSample(validRoute))
            .andExpect(status().`is`(123))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(
                content().json(
                    """
                    {
                        "error": {
                            "title": "SomeTitle",
                            "detail": "SomeMessage"
                         },
                         "info": {
                            "dataVersion": "1234"
                         }
                    }
                    """,
                ),
            )
    }

    @Test
    fun `throw BAD_REQUEST(400) with additional info for bad requests`() {
        every { validControllerCall() } throws BadRequestException("SomeMessage")

        mockMvc.perform(getSample(validRoute))
            .andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(
                content().json(
                    """
                    {
                        "error": {
                            "title": "Bad Request",
                            "detail": "SomeMessage"
                         },
                         "info": {
                            "dataVersion": "1234"
                         }
                    }
                    """,
                ),
            )
    }

    @Test
    fun `throw NOT_IMPLEMENTED(501) with additional info for request of a not implemented resource in SILO`() {
        every { validControllerCall() } throws SiloNotImplementedError("SomeMessage", Exception("SomeCause"))

        mockMvc.perform(getSample(validRoute))
            .andExpect(status().isNotImplemented)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(
                content().json(
                    """
                    {
                        "error": {
                            "title": "Not Implemented",
                            "detail": "SomeMessage"
                         },
                         "info": {
                            "dataVersion": "1234"
                         }
                    }
                    """,
                ),
            )
    }
}
