package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.LapisInfo
import org.junit.jupiter.api.BeforeEach
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
        every {
            lapisInfo.dataVersion
        } returns "1234"
    }

    @ParameterizedTest(name = "GET {0} returns empty JSON")
    @MethodSource("getEndpoints")
    fun `GET returns empty json`(endpoint: String) {
        MockDataForEndpoints.getMockData(endpoint).mockToReturnEmptyData(siloQueryModelMock)

        mockMvc.perform(getSample("$endpoint?country=Switzerland"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "application/json"))
            .andExpect(jsonPath("\$.data").isEmpty())
    }

    @ParameterizedTest(name = "POST {0} returns empty JSON")
    @MethodSource("getEndpoints")
    fun `POST returns empty json`(endpoint: String) {
        MockDataForEndpoints.getMockData(endpoint).mockToReturnEmptyData(siloQueryModelMock)

        val request = postSample(endpoint)
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)
            .accept("application/json")

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "application/json"))
            .andExpect(jsonPath("\$.data").isEmpty())
    }

    @ParameterizedTest(name = "GET {0} returns empty CSV")
    @MethodSource("getEndpoints")
    fun `GET returns empty CSV`(endpoint: String) {
        MockDataForEndpoints.getMockData(endpoint).mockToReturnEmptyData(siloQueryModelMock)

        mockMvc.perform(getSample("$endpoint?country=Switzerland").header(ACCEPT, "text/csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(""))
    }

    @ParameterizedTest(name = "POST {0} returns empty CSV")
    @MethodSource("getEndpoints")
    fun `POST {0} returns empty CSV`(endpoint: String) {
        MockDataForEndpoints.getMockData(endpoint).mockToReturnEmptyData(siloQueryModelMock)

        val request = postSample(endpoint)
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)
            .accept("text/csv")

        mockMvc.perform(request)
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
            .andExpect(content().string(requestsScenario.mockDataCollection.expectedCsv))
    }

    @ParameterizedTest(name = "{0} returns data as CSV without headers")
    @MethodSource("getCsvWithoutHeadersRequests")
    fun `request returns data as CSV without headers`(requestsScenario: RequestScenario) {
        requestsScenario.mockDataCollection.mockWithData(siloQueryModelMock)

        mockMvc.perform(requestsScenario.request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;headers=false;charset=UTF-8"))
            .andExpect(content().string(returnedCsvWithoutHeadersData(requestsScenario.mockDataCollection)))
    }

    @ParameterizedTest(name = "{0} returns data as TSV")
    @MethodSource("getTsvRequests")
    fun `request returns data as TSV`(requestsScenario: RequestScenario) {
        requestsScenario.mockDataCollection.mockWithData(siloQueryModelMock)

        mockMvc.perform(requestsScenario.request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(requestsScenario.mockDataCollection.expectedTsv))
    }

    fun returnedCsvWithoutHeadersData(mockDataCollection: MockDataCollection) =
        mockDataCollection.expectedCsv
            .lines()
            .drop(1)
            .joinToString("\n")

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
                        "POST $endpoint with request parameter",
                        MockDataForEndpoints.getMockData(endpoint),
                        postSample(endpoint)
                            .content("""{"country": "Switzerland", "dataFormat": "$dataFormat"}""")
                            .contentType(MediaType.APPLICATION_JSON),
                    ),
                    RequestScenario(
                        "POST $endpoint with accept header",
                        MockDataForEndpoints.getMockData(endpoint),
                        postSample(endpoint)
                            .content("""{"country": "Switzerland"}""")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(ACCEPT, getAcceptHeaderFor(dataFormat)),
                    ),
                )
            }

        private fun getAcceptHeaderFor(dataFormat: String) =
            when (dataFormat) {
                "csv" -> TEXT_CSV_HEADER
                "csv-without-headers" -> TEXT_CSV_WITHOUT_HEADERS_HEADER
                "tsv" -> TEXT_TSV_HEADER
                "json" -> MediaType.APPLICATION_JSON_VALUE
                else -> throw IllegalArgumentException("Unknown data format: $dataFormat")
            }

        @JvmStatic
        fun getCsvRequests() = getRequests("csv")

        @JvmStatic
        fun getCsvWithoutHeadersRequests() = getRequests("csv-without-headers")

        @JvmStatic
        fun getTsvRequests() = getRequests("tsv")
    }

    data class RequestScenario(
        val description: String,
        val mockDataCollection: MockDataCollection,
        val request: MockHttpServletRequestBuilder,
    ) {
        override fun toString() = description
    }
}
