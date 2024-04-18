package org.genspectrum.lapis.controller

import com.github.luben.zstd.ZstdInputStream
import com.jayway.jsonpath.JsonPath
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_CSV_VALUE
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_TSV_VALUE
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_X_FASTA_VALUE
import org.genspectrum.lapis.controller.MockDataCollection.DataFormat.CSV
import org.genspectrum.lapis.controller.MockDataCollection.DataFormat.NESTED_JSON
import org.genspectrum.lapis.controller.MockDataCollection.DataFormat.PLAIN_JSON
import org.genspectrum.lapis.controller.MockDataCollection.DataFormat.TSV
import org.genspectrum.lapis.controller.SampleRoute.AGGREGATED
import org.genspectrum.lapis.controller.SampleRoute.ALIGNED_AMINO_ACID_SEQUENCES
import org.genspectrum.lapis.controller.SampleRoute.ALIGNED_NUCLEOTIDE_SEQUENCES
import org.genspectrum.lapis.controller.SampleRoute.UNALIGNED_NUCLEOTIDE_SEQUENCES
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.LapisInfo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.zip.GZIPInputStream

private const val INVALID_COMPRESSION_FORMAT = "invalidCompressionFormat"

const val COMPRESSION_FORMAT_GZIP = "gzip"
const val COMPRESSION_FORMAT_ZSTD = "zstd"

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

        val response = mockMvc.perform(requestsScenario.request)
            .andExpect(status().isOk)
            .andExpect(content().contentType(requestsScenario.expectedContentType))
            .andExpect(header().doesNotExist(CONTENT_LENGTH))
            .andExpect(
                when (requestsScenario.expectedContentEncoding) {
                    null -> header().doesNotExist(CONTENT_ENCODING)
                    else -> header().string(CONTENT_ENCODING, requestsScenario.expectedContentEncoding)
                },
            )
            .andReturn()

        val compressionFormat = requestsScenario.compressionFormat

        val decompressedContent = decompressContent(response, compressionFormat)

        requestsScenario.mockData.assertDataMatches(decompressedContent)
    }

    @ParameterizedTest
    @MethodSource("getCompressionFormats")
    fun `GIVEN model throws bad request WHEN requesting compressed data THEN it should return compressed error`(
        compressionFormat: String,
    ) {
        val errorMessage = "test message"
        every { siloQueryModelMock.getAggregated(any()) } throws BadRequestException(errorMessage)

        val response = mockMvc.perform(getSample("${AGGREGATED.pathSegment}?compression=$compressionFormat"))
            .andExpect(status().isBadRequest)
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(header().string(CONTENT_ENCODING, compressionFormat))
            .andReturn()

        val decompressedContent = decompressContent(response, compressionFormat)

        val errorDetail = JsonPath.read<String>(decompressedContent, "$.error.detail")
        assertThat(errorDetail, `is`(errorMessage))
    }

    @ParameterizedTest
    @MethodSource("getCompressionFormats")
    fun `GIVEN model throws bad request WHEN accepting compressed data THEN it should return compressed error`(
        compressionFormat: String,
    ) {
        val errorMessage = "test message"
        every { siloQueryModelMock.getAggregated(any()) } throws BadRequestException(errorMessage)

        val response = mockMvc.perform(getSample(AGGREGATED.pathSegment).header(ACCEPT_ENCODING, compressionFormat))
            .andExpect(status().isBadRequest)
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(header().string(CONTENT_ENCODING, compressionFormat))
            .andReturn()

        val decompressedContent = decompressContent(response, compressionFormat)

        val errorDetail = JsonPath.read<String>(decompressedContent, "$.error.detail")
        assertThat(errorDetail, `is`(errorMessage))
    }

    @Test
    fun `GIVEN multiple values in accept encoding header THEN it should return compressed data`() {
        val mockData = MockDataForEndpoints.getMockData(AGGREGATED.pathSegment).expecting(PLAIN_JSON)
        mockData.mockWithData(siloQueryModelMock)

        val acceptEncodingAsBrowsersSendIt = "$COMPRESSION_FORMAT_GZIP, br, deflate"

        mockMvc.perform(getSample(AGGREGATED.pathSegment).header(ACCEPT_ENCODING, acceptEncodingAsBrowsersSendIt))
            .andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(header().string(CONTENT_ENCODING, COMPRESSION_FORMAT_GZIP))
    }

    private fun decompressContent(
        response: MvcResult,
        compressionFormat: String,
    ): String {
        val content = response.response.contentAsByteArray

        val decompressedStream = when (compressionFormat) {
            Compression.GZIP.value -> GZIPInputStream(content.inputStream())
            Compression.ZSTD.value -> ZstdInputStream(content.inputStream())
            else -> throw Exception("Test issue: unknown compression format $compressionFormat")
        }

        return decompressedStream.readAllBytes().decodeToString()
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
                        dataFormat = CSV,
                        compressionFormat = COMPRESSION_FORMAT_GZIP,
                    ) +
                        getRequests(
                            endpoint = it,
                            dataFormat = CSV,
                            compressionFormat = COMPRESSION_FORMAT_ZSTD,
                        )
                } +
                getRequests(
                    AGGREGATED,
                    dataFormat = NESTED_JSON,
                    compressionFormat = COMPRESSION_FORMAT_GZIP,
                ) +
                getRequests(
                    AGGREGATED,
                    dataFormat = TSV,
                    compressionFormat = COMPRESSION_FORMAT_ZSTD,
                ) +
                listOf(
                    "${UNALIGNED_NUCLEOTIDE_SEQUENCES.pathSegment}/main",
                    "${ALIGNED_NUCLEOTIDE_SEQUENCES.pathSegment}/main",
                    "${ALIGNED_AMINO_ACID_SEQUENCES.pathSegment}/gene1",
                )
                    .flatMap {
                        getFastaRequests(it, COMPRESSION_FORMAT_GZIP) + getFastaRequests(
                            it,
                            COMPRESSION_FORMAT_ZSTD,
                        )
                    }

        @JvmStatic
        val compressionFormats = listOf(COMPRESSION_FORMAT_GZIP, COMPRESSION_FORMAT_ZSTD)
    }
}

data class RequestScenario(
    val callDescription: String,
    val mockData: MockData,
    val request: MockHttpServletRequestBuilder,
    val compressionFormat: String,
    val expectedContentType: String,
    val expectedContentEncoding: String?,
) {
    override fun toString() = "$callDescription returns $compressionFormat compressed data"
}

fun getRequests(
    endpoint: SampleRoute,
    dataFormat: MockDataCollection.DataFormat,
    compressionFormat: String,
) = getRequests(endpoint.pathSegment, dataFormat, compressionFormat)

fun getRequests(
    endpoint: String,
    dataFormat: MockDataCollection.DataFormat,
    compressionFormat: String,
) = listOf(
    RequestScenario(
        callDescription = "GET $endpoint as $dataFormat with request parameter",
        mockData = MockDataForEndpoints.getMockData(endpoint).expecting(dataFormat),
        request = getSample(
            "$endpoint?country=Switzerland&dataFormat=$dataFormat&compression=$compressionFormat",
        ),
        compressionFormat = compressionFormat,
        expectedContentType = getContentTypeForCompressionFormat(compressionFormat),
        expectedContentEncoding = null,
    ),
    RequestScenario(
        callDescription = "GET $endpoint as $dataFormat with accept header",
        mockData = MockDataForEndpoints.getMockData(endpoint).expecting(dataFormat),
        request = getSample("$endpoint?country=Switzerland&dataFormat=$dataFormat")
            .header(ACCEPT_ENCODING, compressionFormat),
        compressionFormat = compressionFormat,
        expectedContentType = getContentTypeForDataFormat(dataFormat),
        expectedContentEncoding = compressionFormat,
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
        expectedContentType = getContentTypeForCompressionFormat(compressionFormat),
        expectedContentEncoding = null,
    ),
    RequestScenario(
        callDescription = "POST $endpoint as $dataFormat with accept header",
        mockData = MockDataForEndpoints.getMockData(endpoint).expecting(dataFormat),
        request = postSample(endpoint)
            .content("""{"country": "Switzerland", "dataFormat": "$dataFormat"}""")
            .contentType(APPLICATION_JSON)
            .header(ACCEPT_ENCODING, compressionFormat),
        compressionFormat = compressionFormat,
        expectedContentType = getContentTypeForDataFormat(dataFormat),
        expectedContentEncoding = compressionFormat,
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
        expectedContentType = getContentTypeForCompressionFormat(compressionFormat),
        expectedContentEncoding = null,
    ),
    RequestScenario(
        callDescription = "GET $endpoint with accept header",
        mockData = MockDataForEndpoints.fastaMockData,
        request = getSample("$endpoint?country=Switzerland")
            .header(ACCEPT_ENCODING, compressionFormat),
        compressionFormat = compressionFormat,
        expectedContentType = "$TEXT_X_FASTA_VALUE;charset=UTF-8",
        expectedContentEncoding = compressionFormat,
    ),
    RequestScenario(
        callDescription = "POST $endpoint with request parameter",
        mockData = MockDataForEndpoints.fastaMockData,
        request = postSample(endpoint)
            .content("""{"country": "Switzerland", "compression": "$compressionFormat"}""")
            .contentType(APPLICATION_JSON),
        compressionFormat = compressionFormat,
        expectedContentType = getContentTypeForCompressionFormat(compressionFormat),
        expectedContentEncoding = null,
    ),
    RequestScenario(
        callDescription = "POST $endpoint with accept header",
        mockData = MockDataForEndpoints.fastaMockData,
        request = postSample(endpoint)
            .content("""{"country": "Switzerland"}""")
            .contentType(APPLICATION_JSON)
            .header(ACCEPT_ENCODING, compressionFormat),
        compressionFormat = compressionFormat,
        expectedContentType = "$TEXT_X_FASTA_VALUE;charset=UTF-8",
        expectedContentEncoding = compressionFormat,
    ),
)

fun getContentTypeForCompressionFormat(compressionFormat: String) =
    when (compressionFormat) {
        COMPRESSION_FORMAT_GZIP -> "application/gzip"
        COMPRESSION_FORMAT_ZSTD -> "application/zstd"
        else -> throw Exception("Test issue: unknown compression format $compressionFormat")
    }

private fun getContentTypeForDataFormat(dataFormat: MockDataCollection.DataFormat) =
    when (dataFormat) {
        PLAIN_JSON -> APPLICATION_JSON_VALUE
        NESTED_JSON -> APPLICATION_JSON_VALUE
        CSV -> "$TEXT_CSV_VALUE;charset=UTF-8"
        TSV -> "$TEXT_TSV_VALUE;charset=UTF-8"
    }
