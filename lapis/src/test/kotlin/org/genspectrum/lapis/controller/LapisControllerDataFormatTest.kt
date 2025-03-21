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
class LapisControllerDataFormatTest(
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

    @ParameterizedTest(name = "{0} returns bad request")
    @MethodSource("getRequestsWithInvalidDataFormat")
    fun `GIVEN unknown data format THEN returns not acceptable`(requestsScenario: RequestScenario) {
        mockMvc.perform(requestsScenario.request)
            .andExpect(status().isNotAcceptable)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(jsonPath("\$.detail", startsWith("Acceptable representations:")))
    }

    @Test
    fun `GIVEN aggregated request with dataFormat fasta THEN returns not acceptable`() {
        mockMvc.perform(getSample("/${SampleRoute.AGGREGATED.pathSegment}?dataFormat=fasta"))
            .andExpect(status().isNotAcceptable)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(jsonPath("\$.detail", startsWith("Acceptable representations:")))
    }

    @Test
    fun `GIVEN amino acid sequences request with dataFormat csv THEN returns not acceptable`() {
        mockMvc.perform(getSample("/${SampleRoute.ALIGNED_AMINO_ACID_SEQUENCES.pathSegment}/S?dataFormat=csv"))
            .andExpect(status().isNotAcceptable)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(jsonPath("\$.detail", startsWith("Acceptable representations:")))
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

        val expectedResult = "${requestsScenario.mockDataCollection.expectedCsv.lines().first()}\n"
        mockMvc.perform(requestsScenario.request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(expectedResult))
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
                mapOf("primaryKey" to TextNode("someValue"), "date" to NullNode.instance),
            ),
        )

        val expectedCsv = """
            primaryKey,date,count
            someValue,,1
        """.trimIndent()

        mockMvc.perform(
            getSample("/aggregated?country=Switzerland&fields=primaryKey&fields=date").header(
                ACCEPT,
                "text/csv",
            ),
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(startsWith(expectedCsv)))
    }

    @Test
    fun `GIVEN details endpoint returns result with null values THEN CSV contains empty strings instead`() {
        every { siloQueryModelMock.getDetails(any()) } returns Stream.of(
            DetailsData(
                mapOf(
                    "primaryKey" to TextNode("some first value"),
                    "date" to NullNode.instance,
                    "country" to TextNode("someValue"),
                ),
            ),
        )

        val expectedCsv = """
            primaryKey,date,country
            some first value,,someValue
        """.trimIndent()

        mockMvc.perform(
            getSample("/details?country=Switzerland&fields=primaryKey&fields=date&fields=country").header(
                ACCEPT,
                "text/csv",
            ),
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(startsWith(expectedCsv)))
    }

    @Test
    fun `GIVEN fields in request WHEN getting aggregated csv THEN fiels are ordered as in request`() {
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
                .content("""{"fields": ["primaryKey", "date", "region"]}""")
                .contentType(MediaType.APPLICATION_JSON)
                .header(ACCEPT, "text/csv"),
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(
                content().string(
                    startsWith(
                        """
                        primaryKey,date,region,count
                        key1,date1,region1,1
                        key2,date2,region2,2
                        """.trimIndent(),
                    ),
                ),
            )
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
                val mockDataCollection = MockDataForEndpoints.getMockData(endpoint)
                val fieldsJsonPart = getFieldsAsJsonPart(mockDataCollection.fields)

                listOf(
                    RequestScenario(
                        "GET $endpoint with request parameter",
                        mockDataCollection,
                        getSample(endpoint)
                            .queryParam("country", "Switzerland")
                            .queryParam("dataFormat", dataFormat)
                            .withFieldsQuery(mockDataCollection.fields),
                    ),
                    RequestScenario(
                        "GET $endpoint with accept header",
                        mockDataCollection,
                        getSample(endpoint)
                            .queryParam("country", "Switzerland")
                            .withFieldsQuery(mockDataCollection.fields)
                            .header(ACCEPT, getAcceptHeaderFor(dataFormat)),
                    ),
                    RequestScenario(
                        "POST JSON $endpoint with request parameter",
                        mockDataCollection,
                        postSample(endpoint)
                            .content("""{"country": "Switzerland", "dataFormat": "$dataFormat" $fieldsJsonPart}""")
                            .contentType(MediaType.APPLICATION_JSON),
                    ),
                    RequestScenario(
                        "POST JSON $endpoint with accept header",
                        mockDataCollection,
                        postSample(endpoint)
                            .content("""{"country": "Switzerland" $fieldsJsonPart}""")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(ACCEPT, getAcceptHeaderFor(dataFormat)),
                    ),
                    RequestScenario(
                        "POST form url encoded $endpoint with request parameter",
                        mockDataCollection,
                        postSample(endpoint)
                            .param("country", "Switzerland")
                            .param("dataFormat", dataFormat)
                            .withFieldsParam(mockDataCollection.fields)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
                    ),
                    RequestScenario(
                        "POST form url encoded $endpoint with accept header",
                        mockDataCollection,
                        postSample(endpoint)
                            .param("country", "Switzerland")
                            .withFieldsParam(mockDataCollection.fields)
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
                "invalid" -> "invalid/invalid"
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
        fun getRequestsWithInvalidDataFormat() = getRequests("invalid")

        @JvmStatic
        fun getColumnOrderRequests() =
            listOf(
                ColumnOrderScenario(
                    description = "GIVEN no fields in request THEN columns are ordered as in database config",
                    fields = emptyList(),
                    expectedDetailsCsv = """
                        primaryKey,date,region,country,pangoLineage,test_boolean_column,age,floatValue
                        key1,date1,region1,,,,,
                        key2,date2,region2,,,,,
                    """.trimIndent(),
                ),
                ColumnOrderScenario(
                    description = "GIVEN fields in request THEN columns are ordered as in request",
                    fields = listOf("region", "date", "primaryKey"),
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
        val expectedDetailsCsv: String,
    ) {
        override fun toString() = description
    }
}
