package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.LapisInfo
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.AminoAcidInsertionResponse
import org.genspectrum.lapis.response.AminoAcidMutationResponse
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.NucleotideInsertionResponse
import org.genspectrum.lapis.response.NucleotideMutationResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
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
        mockEndpointReturnEmptyList(endpoint)

        mockMvc.perform(getSample("$endpoint?country=Switzerland"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "application/json"))
            .andExpect(jsonPath("\$.data").isEmpty())
    }

    @ParameterizedTest(name = "POST {0} returns empty JSON")
    @MethodSource("getEndpoints")
    fun `POST returns empty json`(endpoint: String) {
        mockEndpointReturnEmptyList(endpoint)

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
        mockEndpointReturnEmptyList(endpoint)

        mockMvc.perform(getSample("$endpoint?country=Switzerland").header("Accept", "text/csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(""))
    }

    @ParameterizedTest(name = "POST {0} returns empty CSV")
    @MethodSource("getEndpoints")
    fun `POST {0} returns empty CSV`(endpoint: String) {
        mockEndpointReturnEmptyList(endpoint)

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
        mockEndpointReturnData(requestsScenario.endpoint)

        mockMvc.perform(requestsScenario.request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(returnedCsvData(requestsScenario.endpoint)))
    }

    @ParameterizedTest(name = "{0} returns data as CSV without headers")
    @MethodSource("getCsvWithoutHeadersRequests")
    fun `request returns data as CSV without headers`(requestsScenario: RequestScenario) {
        mockEndpointReturnData(requestsScenario.endpoint)

        mockMvc.perform(requestsScenario.request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;headers=false;charset=UTF-8"))
            .andExpect(content().string(returnedCsvWithoutHeadersData(requestsScenario.endpoint)))
    }

    @ParameterizedTest(name = "{0} returns data as TSV")
    @MethodSource("getTsvRequests")
    fun `request returns data as TSV`(requestsScenario: RequestScenario) {
        mockEndpointReturnData(requestsScenario.endpoint)

        mockMvc.perform(requestsScenario.request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(returnedTsvData(requestsScenario.endpoint)))
    }

    fun mockEndpointReturnEmptyList(endpoint: String) =
        when (endpoint) {
            DETAILS_ROUTE ->
                every {
                    siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
                } returns emptyList()

            AGGREGATED_ROUTE ->
                every {
                    siloQueryModelMock.getAggregated(
                        sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")),
                    )
                } returns emptyList()

            NUCLEOTIDE_MUTATIONS_ROUTE ->
                every {
                    siloQueryModelMock.computeNucleotideMutationProportions(any())
                } returns emptyList()

            AMINO_ACID_MUTATIONS_ROUTE ->
                every {
                    siloQueryModelMock.computeAminoAcidMutationProportions(any())
                } returns emptyList()

            NUCLEOTIDE_INSERTIONS_ROUTE ->
                every {
                    siloQueryModelMock.getNucleotideInsertions(any())
                } returns emptyList()

            AMINO_ACID_INSERTIONS_ROUTE ->
                every {
                    siloQueryModelMock.getAminoAcidInsertions(any())
                } returns emptyList()

            else -> throw IllegalArgumentException("Unknown endpoint: $endpoint")
        }

    fun mockEndpointReturnData(endpoint: String) =
        when (endpoint) {
            DETAILS_ROUTE ->
                every {
                    siloQueryModelMock.getDetails(
                        sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")),
                    )
                } returns detailsData

            AGGREGATED_ROUTE ->
                every {
                    siloQueryModelMock.getAggregated(
                        sequenceFiltersRequestWithFields(
                            mapOf("country" to "Switzerland"),
                        ),
                    )
                } returns aggregationData

            NUCLEOTIDE_MUTATIONS_ROUTE ->
                every {
                    siloQueryModelMock.computeNucleotideMutationProportions(any())
                } returns nucleotideMutationData

            AMINO_ACID_MUTATIONS_ROUTE ->
                every {
                    siloQueryModelMock.computeAminoAcidMutationProportions(any())
                } returns aminoAcidMutationData

            NUCLEOTIDE_INSERTIONS_ROUTE ->
                every {
                    siloQueryModelMock.getNucleotideInsertions(any())
                } returns nucleotideInsertionData

            AMINO_ACID_INSERTIONS_ROUTE ->
                every {
                    siloQueryModelMock.getAminoAcidInsertions(any())
                } returns aminoAcidInsertionData

            else -> throw IllegalArgumentException("Unknown endpoint: $endpoint")
        }

    fun returnedCsvData(endpoint: String) =
        when (endpoint) {
            DETAILS_ROUTE -> detailsDataCsv
            AGGREGATED_ROUTE -> aggregationDataCsv
            NUCLEOTIDE_MUTATIONS_ROUTE -> mutationDataCsv
            AMINO_ACID_MUTATIONS_ROUTE -> mutationDataCsv
            NUCLEOTIDE_INSERTIONS_ROUTE -> nucleotideInsertionDataCsv
            AMINO_ACID_INSERTIONS_ROUTE -> aminoAcidInsertionDataCsv
            else -> throw IllegalArgumentException("Unknown endpoint: $endpoint")
        }

    fun returnedCsvWithoutHeadersData(endpoint: String) =
        returnedCsvData(endpoint)
            .lines()
            .drop(1)
            .joinToString("\n")

    fun returnedTsvData(endpoint: String) =
        when (endpoint) {
            DETAILS_ROUTE -> detailsDataTsv
            AGGREGATED_ROUTE -> aggregationDataTsv
            NUCLEOTIDE_MUTATIONS_ROUTE -> mutationDataTsv
            AMINO_ACID_MUTATIONS_ROUTE -> mutationDataTsv
            NUCLEOTIDE_INSERTIONS_ROUTE -> nucleotideInsertionDataTsv
            AMINO_ACID_INSERTIONS_ROUTE -> aminoAcidInsertionDataTsv
            else -> throw IllegalArgumentException("Unknown endpoint: $endpoint")
        }

    val detailsData = listOf(
        DetailsData(
            mapOf(
                "country" to TextNode("Switzerland"),
                "age" to IntNode(42),
                "floatValue" to DoubleNode(3.14),
            ),
        ),
        DetailsData(
            mapOf(
                "country" to TextNode("Switzerland"),
                "age" to IntNode(43),
                "floatValue" to NullNode.instance,
            ),
        ),
    )

    val detailsDataCsv = """
        country,age,floatValue
        Switzerland,42,3.14
        Switzerland,43,null
    """.trimIndent()

    val detailsDataTsv = """
        country	age	floatValue
        Switzerland	42	3.14
        Switzerland	43	null
    """.trimIndent()

    val aggregationData = listOf(
        AggregationData(
            0,
            mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42)),
        ),
    )

    val aggregationDataCsv = """
        country,age,count
        Switzerland,42,0
    """.trimIndent()

    val aggregationDataTsv = """
        country	age	count
        Switzerland	42	0
    """.trimIndent()

    val nucleotideMutationData = listOf(
        NucleotideMutationResponse(
            "sequenceName:1234",
            2345,
            0.987,
        ),
    )

    val aminoAcidMutationData = listOf(
        AminoAcidMutationResponse(
            "sequenceName:1234",
            2345,
            0.987,
        ),
    )

    val mutationDataCsv = """
        mutation,count,proportion
        sequenceName:1234,2345,0.987
    """.trimIndent()

    val mutationDataTsv = """
        mutation	count	proportion
        sequenceName:1234	2345	0.987
    """.trimIndent()

    val nucleotideInsertionData = listOf(
        NucleotideInsertionResponse(
            "ins_1234:CAGAA",
            41,
        ),
    )

    val nucleotideInsertionDataCsv = """
        insertion,count
        ins_1234:CAGAA,41
    """.trimIndent()

    val nucleotideInsertionDataTsv = """
        insertion	count
        ins_1234:CAGAA	41
    """.trimIndent()

    val aminoAcidInsertionData = listOf(
        AminoAcidInsertionResponse(
            "ins_ORF1a:1234:CAGAA",
            41,
        ),
    )

    val aminoAcidInsertionDataCsv = """
        insertion,count
        ins_ORF1a:1234:CAGAA,41
    """.trimIndent()

    val aminoAcidInsertionDataTsv = """
        insertion	count
        ins_ORF1a:1234:CAGAA	41
    """.trimIndent()

    private companion object {
        @JvmStatic
        val endpoints = SampleRoute.entries.filter { !it.servesFasta }.map { it.pathSegment }

        @JvmStatic
        fun getRequests(dataFormat: String) =
            endpoints.flatMap { endpoint ->
                listOf(
                    RequestScenario(
                        "GET $endpoint with request parameter",
                        endpoint,
                        getSample("$endpoint?country=Switzerland&dataFormat=$dataFormat"),
                    ),
                    RequestScenario(
                        "GET $endpoint with accept header",
                        endpoint,
                        getSample("$endpoint?country=Switzerland")
                            .header("Accept", getAcceptHeaderFor(dataFormat)),
                    ),
                    RequestScenario(
                        "POST $endpoint with request parameter",
                        endpoint,
                        postSample(endpoint)
                            .content("""{"country": "Switzerland", "dataFormat": "$dataFormat"}""")
                            .contentType(MediaType.APPLICATION_JSON),
                    ),
                    RequestScenario(
                        "POST $endpoint with accept header",
                        endpoint,
                        postSample(endpoint)
                            .content("""{"country": "Switzerland", "dataFormat": "$dataFormat"}""")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Accept", getAcceptHeaderFor(dataFormat)),
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
        val endpoint: String,
        val request: MockHttpServletRequestBuilder,
    ) {
        override fun toString() = description
    }
}
