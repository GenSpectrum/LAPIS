package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_CSV_VALUE
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_CSV_WITHOUT_HEADERS_VALUE
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_TSV_VALUE
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.LapisInfo
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.stream.Stream

private const val DATA_VERSION = "1234"

@SpringBootTest
@AutoConfigureMockMvc
class LapisControllerCsvTest(
    @Autowired val mockMvc: MockMvc,
) {
    @MockkBean
    lateinit var siloQueryModelMock: SiloQueryModel

    @MockkBean
    lateinit var lapisInfo: LapisInfo

    @BeforeEach
    fun setup() {
        every { lapisInfo.dataVersion } returns DATA_VERSION
    }

    @ParameterizedTest(name = "{0} returns empty JSON")
    @MethodSource("getJsonRequests")
    fun `returns empty json`(requestsScenario: RequestScenario) {
        requestsScenario.mockDataCollection.mockToReturnEmptyData(siloQueryModelMock)

        mockMvc.perform(requestsScenario.request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "application/json"))
            .andExpect(jsonPath("\$.data").isEmpty())
    }

    @ParameterizedTest(name = "{0} returns empty CSV")
    @MethodSource("getCsvRequests")
    fun `returns empty CSV`(requestsScenario: RequestScenario) {
        requestsScenario.mockDataCollection.mockToReturnEmptyData(siloQueryModelMock)

        mockMvc.perform(requestsScenario.request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(""))
    }

    @ParameterizedTest(name = "{0} returns data as CSV")
    @MethodSource("getCsvRequests")
    fun `request returns data as CSV`(requestsScenario: RequestScenario) {
        requestsScenario.mockDataCollection.mockWithData(siloQueryModelMock)

        mockMvc.perform(requestsScenario.request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(startsWith(requestsScenario.mockDataCollection.expectedCsv)))
    }

    @ParameterizedTest(name = "{0} returns data as CSV without headers")
    @MethodSource("getCsvWithoutHeadersRequests")
    fun `request returns data as CSV without headers`(requestsScenario: RequestScenario) {
        requestsScenario.mockDataCollection.mockWithData(siloQueryModelMock)

        mockMvc.perform(requestsScenario.request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/plain"))
            .andExpect(content().string(startsWith(returnedCsvWithoutHeadersData(requestsScenario.mockDataCollection))))
    }

    @ParameterizedTest(name = "{0} returns data as TSV")
    @MethodSource("getTsvRequests")
    fun `request returns data as TSV`(requestsScenario: RequestScenario) {
        requestsScenario.mockDataCollection.mockWithData(siloQueryModelMock)

        mockMvc.perform(requestsScenario.request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(startsWith(requestsScenario.mockDataCollection.expectedTsv)))
    }

    fun returnedCsvWithoutHeadersData(mockDataCollection: MockDataCollection) =
        mockDataCollection.expectedCsv
            .lines()
            .drop(1)
            .joinToString("\n")

    @Test
    fun `GIVEN aggregated endpoint returns result with null values THEN CSV contains empty strings instead`() {
        every { siloQueryModelMock.getAggregated(any()) } returns Stream.of(
            AggregationData(
                1,
                mapOf("firstKey" to TextNode("someValue"), "keyWithNullValue" to NullNode.instance),
            ),
        )

        val expectedCsv = """
            firstKey,keyWithNullValue,count
            someValue,,1
        """.trimIndent()

        mockMvc.perform(getSample("/aggregated?country=Switzerland").header(ACCEPT, "text/csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(startsWith(expectedCsv)))
    }

    @Test
    fun `GIVEN details endpoint returns result with null values THEN CSV contains empty strings instead`() {
        every { siloQueryModelMock.getDetails(any()) } returns Stream.of(
            DetailsData(
                mapOf(
                    "firstKey" to TextNode("some first value"),
                    "keyWithNullValue" to NullNode.instance,
                    "someOtherKey" to TextNode("someValue"),
                ),
            ),
        )

        val expectedCsv = """
            firstKey,keyWithNullValue,someOtherKey
            some first value,,someValue
        """.trimIndent()

        mockMvc.perform(getSample("/details?country=Switzerland").header(ACCEPT, "text/csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(startsWith(expectedCsv)))
    }

    private companion object {
        @JvmStatic
        val endpoints = SampleRoute.entries.filter { !it.servesFasta }.map { it.pathSegment }

        @JvmStatic
        fun getRequests(dataFormat: String) =
            endpoints.flatMap { endpoint ->
                listOf(
                    RequestScenario(
                        "GET $endpoint with request parameter",
                        MockDataForEndpoints.getMockData(endpoint),
                        getSample("$endpoint?country=Switzerland&dataFormat=$dataFormat"),
                    ),
                    RequestScenario(
                        "GET $endpoint with accept header",
                        MockDataForEndpoints.getMockData(endpoint),
                        getSample("$endpoint?country=Switzerland")
                            .header(ACCEPT, getAcceptHeaderFor(dataFormat)),
                    ),
                    RequestScenario(
                        "POST JSON $endpoint with request parameter",
                        MockDataForEndpoints.getMockData(endpoint),
                        postSample(endpoint)
                            .content("""{"country": "Switzerland", "dataFormat": "$dataFormat"}""")
                            .contentType(MediaType.APPLICATION_JSON),
                    ),
                    RequestScenario(
                        "POST JSON $endpoint with accept header",
                        MockDataForEndpoints.getMockData(endpoint),
                        postSample(endpoint)
                            .content("""{"country": "Switzerland"}""")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(ACCEPT, getAcceptHeaderFor(dataFormat)),
                    ),
                    RequestScenario(
                        "POST form url encoded $endpoint with request parameter",
                        MockDataForEndpoints.getMockData(endpoint),
                        postSample(endpoint)
                            .param("country", "Switzerland")
                            .param("dataFormat", dataFormat)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
                    ),
                    RequestScenario(
                        "POST form url encoded $endpoint with accept header",
                        MockDataForEndpoints.getMockData(endpoint),
                        postSample(endpoint)
                            .param("country", "Switzerland")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .header(ACCEPT, getAcceptHeaderFor(dataFormat)),
                    ),
                )
            }

        private fun getAcceptHeaderFor(dataFormat: String) =
            when (dataFormat) {
                "csv" -> TEXT_CSV_VALUE
                "csv-without-headers" -> TEXT_CSV_WITHOUT_HEADERS_VALUE
                "tsv" -> TEXT_TSV_VALUE
                "json" -> MediaType.APPLICATION_JSON_VALUE
                else -> throw IllegalArgumentException("Unknown data format: $dataFormat")
            }

        @JvmStatic
        fun getCsvRequests() = getRequests("csv")

        @JvmStatic
        fun getCsvWithoutHeadersRequests() = getRequests("csv-without-headers")

        @JvmStatic
        fun getTsvRequests() = getRequests("tsv")

        @JvmStatic
        fun getJsonRequests() = getRequests("json")
    }

    data class RequestScenario(
        val description: String,
        val mockDataCollection: MockDataCollection,
        val request: MockHttpServletRequestBuilder,
    ) {
        override fun toString() = description
    }
}
