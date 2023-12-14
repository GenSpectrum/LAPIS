package org.genspectrum.lapis.auth

import com.ninjasquad.springmockk.MockkBean
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.verify
import org.genspectrum.lapis.controller.AGGREGATED_ROUTE
import org.genspectrum.lapis.controller.getSample
import org.genspectrum.lapis.controller.postSample
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.LapisInfo
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
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

private const val NOT_AUTHORIZED_TO_ACCESS_ENDPOINT_ERROR = """
{
    "error" : {
        "title": "Forbidden",
        "detail": "You are not authorized to access /sample/aggregated."
    }
}
"""

private const val FORBIDDEN_TO_ACCESS_ENDPOINT_ERROR = """
{
    "error" : {
        "title": "Forbidden",
        "detail": "An access key is required to access /sample/aggregated."
    }
}
"""

@SpringBootTest(properties = ["lapis.databaseConfig.path=src/test/resources/config/protectedDataDatabaseConfig.yaml"])
@AutoConfigureMockMvc
class ProtectedDataAuthorizationTest(
    @Autowired val mockMvc: MockMvc,
) {
    @MockkBean
    lateinit var siloQueryModelMock: SiloQueryModel

    private val validRoute = AGGREGATED_ROUTE

    @MockkBean
    lateinit var lapisInfo: LapisInfo

    @BeforeEach
    fun setUp() {
        every { siloQueryModelMock.getAggregated(any()) } returns emptyList()

        every {
            lapisInfo.dataVersion
        } returns "1234"

        MockKAnnotations.init(this)
    }

    @Test
    fun `given no access key in GET request to protected instance, then access is denied`() {
        mockMvc.perform(getSample(validRoute))
            .andExpect(status().isForbidden)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(FORBIDDEN_TO_ACCESS_ENDPOINT_ERROR))
    }

    @Test
    fun `given no access key in POST request to protected instance, then access is denied`() {
        mockMvc.perform(postRequestWithBody(""))
            .andExpect(status().isForbidden)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(FORBIDDEN_TO_ACCESS_ENDPOINT_ERROR))
    }

    @Test
    fun `given wrong access key in GET request to protected instance, then access is denied`() {
        mockMvc.perform(getSample("$validRoute?accessKey=invalidKey"))
            .andExpect(status().isForbidden)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(NOT_AUTHORIZED_TO_ACCESS_ENDPOINT_ERROR))
    }

    @Test
    fun `given wrong access key in POST request to protected instance, then access is denied`() {
        mockMvc.perform(postRequestWithBody("""{"accessKey": "invalidKey"}"""))
            .andExpect(status().isForbidden)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(NOT_AUTHORIZED_TO_ACCESS_ENDPOINT_ERROR))
    }

    @Test
    fun `given valid access key for aggregated data in GET request to protected instance, then access is granted`() {
        mockMvc.perform(
            getSample("$validRoute?accessKey=testAggregatedDataAccessKey&field1=value1"),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))

        verify { siloQueryModelMock.getAggregated(sequenceFilterRequest()) }
    }

    @Test
    fun `given valid access key for aggregated data in POST request to protected instance, then access is granted`() {
        mockMvc.perform(
            postRequestWithBody(
                """ {
                    "accessKey": "testAggregatedDataAccessKey",
                    "field1": "value1"
                }""",
            ),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))

        verify { siloQueryModelMock.getAggregated(sequenceFilterRequest()) }
    }

    @Test
    fun `given aggregated access key in GET request but filters are too fine-grained, then access is denied`() {
        mockMvc.perform(
            getSample("$validRoute?accessKey=testAggregatedDataAccessKey&gisaid_epi_isl=value"),
        )
            .andExpect(status().isForbidden)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(NOT_AUTHORIZED_TO_ACCESS_ENDPOINT_ERROR))
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
            .andExpect(status().isForbidden)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(NOT_AUTHORIZED_TO_ACCESS_ENDPOINT_ERROR))
    }

    @Test
    fun `given valid access key for full access in GET request to protected instance, then access is granted`() {
        mockMvc.perform(
            getSample("$validRoute?accessKey=testFullAccessKey&field1=value1"),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))

        verify { siloQueryModelMock.getAggregated(sequenceFilterRequest()) }
    }

    @Test
    fun `given valid access key for full access in POST request to protected instance, then access is granted`() {
        mockMvc.perform(
            postRequestWithBody(
                """ {
                    "accessKey": "testFullAccessKey",
                    "field1": "value1"
                }""",
            ),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))

        verify { siloQueryModelMock.getAggregated(sequenceFilterRequest()) }
    }

    private fun sequenceFilterRequest() =
        SequenceFiltersRequestWithFields(
            mapOf("field1" to "value1"),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
        )

    @Test
    fun `the swagger ui and api docs are always accessible`() {
        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api-docs"))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api-docs.yaml"))
            .andExpect(status().isOk)
    }

    private fun postRequestWithBody(body: String) =
        postSample(validRoute)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
}
