package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.ObjectMapper
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
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
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

    @ParameterizedTest(name = "Aggregated data: {0}")
    @MethodSource("getColumnOrderRequests")
    fun `returns aggregated csv columns in correct order`(scenario: ColumnOrderScenario) {
        every { siloQueryModelMock.getAggregated(any()) } returns Stream.of(
            AggregationData(
                1,
                mapOf("date" to TextNode("date1"), "primaryKey" to TextNode("key1"), "region" to TextNode("region1")),
            ),
            AggregationData(
                2,
                mapOf("date" to TextNode("date2"), "primaryKey" to TextNode("key2"), "region" to TextNode("region2")),
            ),
        )

        mockMvc.perform(
            postSample("/aggregated")
                .content("""{"fields": ${(objectMapper.writeValueAsString(scenario.fields))}}""")
                .contentType(MediaType.APPLICATION_JSON)
                .header(ACCEPT, "text/csv"),
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(startsWith(scenario.expectedAggregatedCsv)))
    }

    @ParameterizedTest(name = "Details data: {0}")
    @MethodSource("getColumnOrderRequests")
    fun `returns details csv columns in correct order`(scenario: ColumnOrderScenario) {
        every { siloQueryModelMock.getDetails(any()) } returns Stream.of(
            DetailsData(
                mapOf("date" to TextNode("date1"), "primaryKey" to TextNode("key1"), "region" to TextNode("region1")),
            ),
            DetailsData(
                mapOf("date" to TextNode("date2"), "primaryKey" to TextNode("key2"), "region" to TextNode("region2")),
            ),
        )

        mockMvc.perform(
            postSample("/details")
                .content("""{"fields": ${(objectMapper.writeValueAsString(scenario.fields))}}""")
                .contentType(MediaType.APPLICATION_JSON)
                .header(ACCEPT, "text/csv"),
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(startsWith(scenario.expectedDetailsCsv)))
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

        @JvmStatic
        fun getColumnOrderRequests() =
            listOf(
                ColumnOrderScenario(
                    description = "GIVEN no fields in request THEN columns are ordered as in database config",
                    fields = emptyList(),
                    expectedAggregatedCsv = """
                        primaryKey,date,region,count
                        key1,date1,region1,1
                        key2,date2,region2,2
                    """.trimIndent(),
                    expectedDetailsCsv = """
                        primaryKey,date,region
                        key1,date1,region1
                        key2,date2,region2
                    """.trimIndent(),
                ),
                ColumnOrderScenario(
                    description = "GIVEN fields in request THEN columns are ordered as in request",
                    fields = listOf("region", "date", "primaryKey"),
                    expectedAggregatedCsv = """
                        region,date,primaryKey,count
                        region1,date1,key1,1
                        region2,date2,key2,2
                    """.trimIndent(),
                    expectedDetailsCsv = """
                        region,date,primaryKey
                        region1,date1,key1
                        region2,date2,key2
                    """.trimIndent(),
                ),
                ColumnOrderScenario(
                    description = "GIVEN fields in request in different order THEN columns are ordered as in request",
                    fields = listOf("date", "region", "primaryKey"),
                    expectedAggregatedCsv = """
                        date,region,primaryKey,count
                        date1,region1,key1,1
                        date2,region2,key2,2
                    """.trimIndent(),
                    expectedDetailsCsv = """
                        date,region,primaryKey
                        date1,region1,key1
                        date2,region2,key2
                    """.trimIndent(),
                ),
                ColumnOrderScenario(
                    description = "GIVEN fields in request with missing column WHEN lapis still returns that column " +
                        "THEN missing columns are last",
                    fields = listOf("region", "date"),
                    expectedAggregatedCsv = """
                        region,date,primaryKey,count
                        region1,date1,key1,1
                        region2,date2,key2,2
                    """.trimIndent(),
                    expectedDetailsCsv = """
                        region,date,primaryKey
                        region1,date1,key1
                        region2,date2,key2
                    """.trimIndent(),
                ),
            )
    }

    data class RequestScenario(
        val description: String,
        val mockDataCollection: MockDataCollection,
        val request: MockHttpServletRequestBuilder,
    ) {
        override fun toString() = description
    }

    data class ColumnOrderScenario(
        val description: String,
        val fields: List<String>,
        val expectedAggregatedCsv: String,
        val expectedDetailsCsv: String,
    ) {
        override fun toString() = description
    }
}
