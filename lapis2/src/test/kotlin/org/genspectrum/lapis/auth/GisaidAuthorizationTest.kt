package org.genspectrum.lapis.auth

import com.ninjasquad.springmockk.MockkBean
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.verify
import org.genspectrum.lapis.model.SiloQueryModel
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
    lateinit var siloQueryModelMock: SiloQueryModel

    private val validRoute = "/aggregated"

    @BeforeEach
    fun setUp() {
        every { siloQueryModelMock.aggregate(any()) } returns AggregatedResponse(1)

        MockKAnnotations.init(this)
    }

    @Test
    fun `given no access key in GET request to GISAID instance, then access is denied`() {
        mockMvc.perform(MockMvcRequestBuilders.get(validRoute))
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    """
                    {
                      "title": "Forbidden",
                      "message": "An access key is required to access /aggregated."
                    }
                    """,
                ),
            )
    }

    @Test
    fun `given no access key in POST request to GISAID instance, then access is denied`() {
        mockMvc.perform(postRequestWithBody(""))
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    """
                    {
                      "title": "Forbidden",
                      "message": "An access key is required to access /aggregated."
                    }
                    """,
                ),
            )
    }

    @Test
    fun `given wrong access key in GET request to GISAID instance, then access is denied`() {
        mockMvc.perform(MockMvcRequestBuilders.get("$validRoute?accessKey=invalidKey"))
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    """
                    {
                      "title": "Forbidden",
                      "message": "You are not authorized to access /aggregated."
                    }
                    """,
                ),
            )
    }

    @Test
    fun `given wrong access key in POST request to GISAID instance, then access is denied`() {
        mockMvc.perform(postRequestWithBody("""{"accessKey": "invalidKey"}"""))
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    """
                    {
                      "title": "Forbidden",
                      "message": "You are not authorized to access /aggregated."
                    }
                    """,
                ),
            )
    }

    @Test
    fun `given valid access key for aggregated data in GET request to GISAID instance, then access is granted`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("$validRoute?accessKey=testAggregatedDataAccessKey&field1=value1"),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))

        verify { siloQueryModelMock.aggregate(mapOf("field1" to "value1")) }
    }

    @Test
    fun `given valid access key for aggregated data in POST request to GISAID instance, then access is granted`() {
        mockMvc.perform(
            postRequestWithBody(
                """ {
                    "accessKey": "testAggregatedDataAccessKey",
                    "field1": "value1"
                }""",
            ),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))

        verify { siloQueryModelMock.aggregate(mapOf("field1" to "value1")) }
    }

    @Test
    fun `given aggregated access key in GET request but filters are too fine-grained, then access is denied`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("$validRoute?accessKey=testAggregatedDataAccessKey&gisaid_epi_isl=value"),
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    """
                    {
                      "title": "Forbidden",
                      "message": "You are not authorized to access /aggregated."
                    }
                    """,
                ),
            )
    }

    @Test
    fun `given aggregated access key in POST request but filters are too fine-grained, then access is denied`() {
        mockMvc.perform(
            postRequestWithBody(
                """ {
                    "accessKey": "testAggregatedDataAccessKey",
                    "gisaid_epi_isl": "some value"
                }""",
            ),
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    """
                    {
                      "title": "Forbidden",
                      "message": "You are not authorized to access /aggregated."
                    }
                    """,
                ),
            )
    }

    @Test
    fun `given valid access key for full access in GET request to GISAID instance, then access is granted`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("$validRoute?accessKey=testFullAccessKey&field1=value1"),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))

        verify { siloQueryModelMock.aggregate(mapOf("field1" to "value1")) }
    }

    @Test
    fun `given valid access key for full access in POST request to GISAID instance, then access is granted`() {
        mockMvc.perform(
            postRequestWithBody(
                """ {
                    "accessKey": "testFullAccessKey",
                    "field1": "value1"
                }""",
            ),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))

        verify { siloQueryModelMock.aggregate(mapOf("field1" to "value1")) }
    }

    @Test
    fun `the swagger ui and api docs are always accessible`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/swagger-ui/index.html"),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api-docs"),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api-docs.yaml"),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    private fun postRequestWithBody(body: String) =
        MockMvcRequestBuilders.post(validRoute)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
}
