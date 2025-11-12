package org.genspectrum.lapis.auth

import com.ninjasquad.springmockk.MockkBean
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.verify
import org.genspectrum.lapis.PRIMARY_KEY_FIELD
import org.genspectrum.lapis.controller.AGGREGATED_ROUTE
import org.genspectrum.lapis.controller.DATABASE_CONFIG_ROUTE
import org.genspectrum.lapis.controller.DETAILS_ROUTE
import org.genspectrum.lapis.controller.REFERENCE_GENOME_ROUTE
import org.genspectrum.lapis.controller.getSample
import org.genspectrum.lapis.controller.postSample
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.LapisInfo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.security.MessageDigest
import java.time.Instant
import java.util.stream.Stream

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
    @param:Autowired val mockMvc: MockMvc,
) {
    @MockkBean
    lateinit var siloQueryModelMock: SiloQueryModel

    private val validRoute = AGGREGATED_ROUTE

    @MockkBean
    lateinit var lapisInfo: LapisInfo

    @BeforeEach
    fun setUp() {
        every { siloQueryModelMock.getAggregated(any()) } returns Stream.empty()

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
    fun `given too old access key in GET request to protected instance, then access is denied`() {
        val oldKey = getCurrentAccessKey("testAggregatedDataAccessKey", Instant.now().epochSecond - 10)
        mockMvc.perform(getSample("$validRoute?accessKey=$oldKey"))
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
        val currentKey = getCurrentAccessKey("testAggregatedDataAccessKey")
        mockMvc.perform(
            getSample("$validRoute?accessKey=$currentKey&field1=value1"),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))

        verify { siloQueryModelMock.getAggregated(sequenceFilterRequest()) }
    }

    @Test
    fun `given 2s old access key for aggregated data in GET request to protected instance, then access is granted`() {
        val currentKey = getCurrentAccessKey("testAggregatedDataAccessKey", Instant.now().epochSecond - 2)
        mockMvc.perform(
            getSample("$validRoute?accessKey=$currentKey&field1=value1"),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))

        verify { siloQueryModelMock.getAggregated(sequenceFilterRequest()) }
    }

    @Test
    fun `given second valid access key for agg data in GET request to protected instance, then access is granted`() {
        val currentKey = getCurrentAccessKey("testAggregatedDataAccessKey2")
        mockMvc.perform(
            getSample("$validRoute?accessKey=$currentKey&field1=value1"),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))

        verify { siloQueryModelMock.getAggregated(sequenceFilterRequest()) }
    }

    @Test
    fun `given valid access key for aggregated data in POST request to protected instance, then access is granted`() {
        val currentKey = getCurrentAccessKey("testAggregatedDataAccessKey")
        mockMvc.perform(
            postRequestWithBody(
                """ {
                    "accessKey": "$currentKey",
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
        val currentKey = getCurrentAccessKey("testAggregatedDataAccessKey")
        mockMvc.perform(
            getSample("$validRoute?accessKey=$currentKey&$PRIMARY_KEY_FIELD=value"),
        )
            .andExpect(status().isForbidden)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(NOT_AUTHORIZED_TO_ACCESS_ENDPOINT_ERROR))
    }

    @Test
    fun `given aggregated access key in POST request but filters are too fine-grained, then access is denied`() {
        val currentKey = getCurrentAccessKey("testAggregatedDataAccessKey")
        mockMvc.perform(
            postRequestWithBody(
                """ {
                    "accessKey": "$currentKey",
                    "$PRIMARY_KEY_FIELD": "some value"
                }""",
            ),
        )
            .andExpect(status().isForbidden)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(NOT_AUTHORIZED_TO_ACCESS_ENDPOINT_ERROR))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "fields=$PRIMARY_KEY_FIELD,country",
            "fields=$PRIMARY_KEY_FIELD&fields=country",
        ],
    )
    fun `GIVEN aggregated access key in GET request but request stratifies too fine-grained THEN access is denied`(
        fieldsQueryParameter: String,
    ) {
        val currentKey = getCurrentAccessKey("testAggregatedDataAccessKey")
        mockMvc.perform(
            getSample("$validRoute?accessKey=$currentKey&$fieldsQueryParameter"),
        )
            .andExpect(status().isForbidden)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(NOT_AUTHORIZED_TO_ACCESS_ENDPOINT_ERROR))
    }

    @Test
    fun `GIVEN aggregated access key in GET request that stratifies non-fine-grained THEN access is granted`() {
        val currentKey = getCurrentAccessKey("testAggregatedDataAccessKey")
        mockMvc.perform(
            getSample("$validRoute?accessKey=$currentKey&fields=country"),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `GIVEN aggregated access key in POST request but request stratifies too fine-grained THEN access is denied`() {
        val currentKey = getCurrentAccessKey("testAggregatedDataAccessKey")
        mockMvc.perform(
            postRequestWithBody(
                """ {
                    "accessKey": "$currentKey",
                    "fields": ["$PRIMARY_KEY_FIELD", "country"]
                }""",
            ),
        )
            .andExpect(status().isForbidden)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(NOT_AUTHORIZED_TO_ACCESS_ENDPOINT_ERROR))
    }

    @Test
    fun `GIVEN aggregated access key in GET request where fields only contains primary key THEN access is granted`() {
        val currentKey = getCurrentAccessKey("testAggregatedDataAccessKey")
        mockMvc.perform(
            getSample("$validRoute?accessKey=$currentKey&fields=$PRIMARY_KEY_FIELD"),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `GIVEN aggregated access key in POST request where fields only contains primary key THEN access is granted`() {
        val currentKey = getCurrentAccessKey("testAggregatedDataAccessKey")
        mockMvc.perform(
            postRequestWithBody(
                """ {
                    "accessKey": "$currentKey",
                    "fields": ["$PRIMARY_KEY_FIELD"]
                }""",
            ),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `GIVEN aggregated accessKey in details request where fields only contains primaryKey THEN access is granted`() {
        every { siloQueryModelMock.getDetails(any()) } returns Stream.empty()

        val currentKey = getCurrentAccessKey("testAggregatedDataAccessKey")
        mockMvc.perform(
            postSample(DETAILS_ROUTE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{
                        "accessKey": "$currentKey",
                        "fields": ["$PRIMARY_KEY_FIELD"]
                    }""",
                ),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
    fun `given second valid access key for full access in GET request to protected instance, then access is granted`() {
        mockMvc.perform(
            getSample("$validRoute?accessKey=testFullAccessKey2&field1=value1"),
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
            mapOf("field1" to listOf("value1")),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
        )

    @Test
    fun `whitelisted routes are always accessible`() {
        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api-docs"))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api-docs.yaml"))
            .andExpect(status().isOk)

        mockMvc.perform(getSample(DATABASE_CONFIG_ROUTE))
            .andExpect(status().isOk)

        mockMvc.perform(getSample(REFERENCE_GENOME_ROUTE))
            .andExpect(status().isOk)
    }

    private fun postRequestWithBody(body: String) =
        postSample(validRoute)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)

    private fun hash(text: String): String {
        val bytes = text.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }

    private fun getCurrentAccessKey(
        baseKey: String,
        epoch: Long = Instant.now().epochSecond,
    ): String = hash("$baseKey:$epoch")
}
