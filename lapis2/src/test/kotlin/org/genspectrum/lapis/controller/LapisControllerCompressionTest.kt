package org.genspectrum.lapis.controller

import com.github.luben.zstd.ZstdInputStream
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.controller.SampleRoute.AGGREGATED
import org.genspectrum.lapis.controller.SampleRoute.ALIGNED_AMINO_ACID_SEQUENCES
import org.genspectrum.lapis.controller.SampleRoute.ALIGNED_NUCLEOTIDE_SEQUENCES
import org.genspectrum.lapis.controller.SampleRoute.UNALIGNED_NUCLEOTIDE_SEQUENCES
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.LapisInfo
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders.ACCEPT_ENCODING
import org.springframework.http.HttpHeaders.CONTENT_ENCODING
import org.springframework.http.HttpHeaders.CONTENT_LENGTH
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.zip.GZIPInputStream

private const val INVALID_COMPRESSION_FORMAT = "invalidCompressionFormat"

@SpringBootTest
@AutoConfigureMockMvc
class LapisControllerCompressionTest(
    @Autowired val mockMvc: MockMvc,
) {
    @MockkBean
    lateinit var siloQueryModelMock: SiloQueryModel

    @MockkBean
    lateinit var lapisInfo: LapisInfo

    @BeforeEach
    fun setup() {
        every { lapisInfo.dataVersion } returns "1234"
    }

    @ParameterizedTest
    @MethodSource("getRequestsWithInvalidCompressionFormat")
    fun `WHEN I send a request with unknown compression format THEN should show an error`(
        request: MockHttpServletRequestBuilder,
    ) {
        mockMvc.perform(request)
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath("\$.error.detail")
                    .value(containsString("Unknown compression format: $INVALID_COMPRESSION_FORMAT")),
            )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getScenarios")
    fun `endpoints return compressed data`(requestsScenario: RequestScenario) {
        requestsScenario.mockData.mockWithData(siloQueryModelMock)

        val content = mockMvc.perform(requestsScenario.request)
            .andExpect(status().isOk)
            .andExpect(content().contentType(requestsScenario.expectedContentType))
            .andExpect(header().doesNotExist(CONTENT_LENGTH))
            .andExpect(header().string(CONTENT_ENCODING, requestsScenario.compressionFormat))
            .andReturn()
            .response
            .contentAsByteArray

        val decompressedStream = when (requestsScenario.compressionFormat) {
            Compression.GZIP.value -> GZIPInputStream(content.inputStream())
            Compression.ZSTD.value -> ZstdInputStream(content.inputStream())
            else -> throw Exception("Test issue: unknown compression format ${requestsScenario.compressionFormat}")
        }
        val decompressedContent = decompressedStream.readAllBytes().decodeToString()

        requestsScenario.mockData.assertDataMatches(decompressedContent)
    }

    private companion object {
        @JvmStatic
        val requestsWithInvalidCompressionFormat = listOf(
            Arguments.of(getSample("${AGGREGATED.pathSegment}?compression=$INVALID_COMPRESSION_FORMAT")),
            Arguments.of(
                postSample(AGGREGATED.pathSegment)
                    .content("""{"compression": "$INVALID_COMPRESSION_FORMAT"}""")
                    .contentType(APPLICATION_JSON),
            ),
        )

        @JvmStatic
        val scenarios =
            SampleRoute.entries
                .filter { !it.servesFasta }
                .flatMap {
                    getRequests(
                        endpoint = it,
                        dataFormat = MockDataCollection.DataFormat.CSV,
                        compressionFormat = "gzip",
                        expectedContentType = "$TEXT_CSV_HEADER;charset=UTF-8",
                    ) +
                        getRequests(
                            endpoint = it,
                            dataFormat = MockDataCollection.DataFormat.CSV,
                            compressionFormat = "zstd",
                            expectedContentType = "$TEXT_CSV_HEADER;charset=UTF-8",
                        )
                } +
                getRequests(
                    AGGREGATED,
                    dataFormat = MockDataCollection.DataFormat.NESTED_JSON,
                    compressionFormat = "gzip",
                    expectedContentType = APPLICATION_JSON_VALUE,
                ) +
                getRequests(
                    AGGREGATED,
                    dataFormat = MockDataCollection.DataFormat.TSV,
                    compressionFormat = "zstd",
                    expectedContentType = "$TEXT_TSV_HEADER;charset=UTF-8",
                ) +
                listOf(
                    "${UNALIGNED_NUCLEOTIDE_SEQUENCES.pathSegment}/main",
                    "${ALIGNED_NUCLEOTIDE_SEQUENCES.pathSegment}/main",
                    "${ALIGNED_AMINO_ACID_SEQUENCES.pathSegment}/gene1",
                )
                    .flatMap { getFastaRequests(it, "gzip") + getFastaRequests(it, "zstd") }
    }
}

data class RequestScenario(
    val callDescription: String,
    val mockData: MockData,
    val request: MockHttpServletRequestBuilder,
    val compressionFormat: String,
    val expectedContentType: String,
) {
    override fun toString() = "$callDescription returns $compressionFormat compressed data"
}

fun getRequests(
    endpoint: SampleRoute,
    dataFormat: MockDataCollection.DataFormat,
    compressionFormat: String,
    expectedContentType: String,
) = getRequests(endpoint.pathSegment, dataFormat, compressionFormat, expectedContentType)

fun getRequests(
    endpoint: String,
    dataFormat: MockDataCollection.DataFormat,
    compressionFormat: String,
    expectedContentType: String,
) = listOf(
    RequestScenario(
        callDescription = "GET $endpoint as $dataFormat with request parameter",
        mockData = MockDataForEndpoints.getMockData(endpoint).expecting(dataFormat),
        request = getSample(
            "$endpoint?country=Switzerland&dataFormat=$dataFormat&compression=$compressionFormat",
        ),
        compressionFormat = compressionFormat,
        expectedContentType = expectedContentType,
    ),
    RequestScenario(
        callDescription = "GET $endpoint as $dataFormat with accept header",
        mockData = MockDataForEndpoints.getMockData(endpoint).expecting(dataFormat),
        request = getSample("$endpoint?country=Switzerland&dataFormat=$dataFormat")
            .header(ACCEPT_ENCODING, compressionFormat),
        compressionFormat = compressionFormat,
        expectedContentType = expectedContentType,
    ),
    RequestScenario(
        callDescription = "POST $endpoint as $dataFormat with request parameter",
        mockData = MockDataForEndpoints.getMockData(endpoint).expecting(dataFormat),
        request = postSample(endpoint)
            .content(
                """{"country": "Switzerland", "dataFormat": "$dataFormat", "compression": "$compressionFormat"}""",
            )
            .contentType(APPLICATION_JSON),
        compressionFormat = compressionFormat,
        expectedContentType = expectedContentType,
    ),
    RequestScenario(
        callDescription = "POST $endpoint as $dataFormat with accept header",
        mockData = MockDataForEndpoints.getMockData(endpoint).expecting(dataFormat),
        request = postSample(endpoint)
            .content("""{"country": "Switzerland", "dataFormat": "$dataFormat"}""")
            .contentType(APPLICATION_JSON)
            .header(ACCEPT_ENCODING, compressionFormat),
        compressionFormat = compressionFormat,
        expectedContentType = expectedContentType,
    ),
)

private fun getFastaRequests(
    endpoint: String,
    compressionFormat: String,
) = listOf(
    RequestScenario(
        callDescription = "GET $endpoint with request parameter",
        mockData = MockDataForEndpoints.fastaMockData,
        request = getSample("$endpoint?country=Switzerland&compression=$compressionFormat"),
        compressionFormat = compressionFormat,
        expectedContentType = "$TEXT_X_FASTA_HEADER;charset=UTF-8",
    ),
    RequestScenario(
        callDescription = "GET $endpoint with accept header",
        mockData = MockDataForEndpoints.fastaMockData,
        request = getSample("$endpoint?country=Switzerland")
            .header(ACCEPT_ENCODING, compressionFormat),
        compressionFormat = compressionFormat,
        expectedContentType = "$TEXT_X_FASTA_HEADER;charset=UTF-8",
    ),
    RequestScenario(
        callDescription = "POST $endpoint with request parameter",
        mockData = MockDataForEndpoints.fastaMockData,
        request = postSample(endpoint)
            .content("""{"country": "Switzerland", "compression": "$compressionFormat"}""")
            .contentType(APPLICATION_JSON),
        compressionFormat = compressionFormat,
        expectedContentType = "$TEXT_X_FASTA_HEADER;charset=UTF-8",
    ),
    RequestScenario(
        callDescription = "POST $endpoint with accept header",
        mockData = MockDataForEndpoints.fastaMockData,
        request = postSample(endpoint)
            .content("""{"country": "Switzerland"}""")
            .contentType(APPLICATION_JSON)
            .header(ACCEPT_ENCODING, compressionFormat),
        compressionFormat = compressionFormat,
        expectedContentType = "$TEXT_X_FASTA_HEADER;charset=UTF-8",
    ),
)
