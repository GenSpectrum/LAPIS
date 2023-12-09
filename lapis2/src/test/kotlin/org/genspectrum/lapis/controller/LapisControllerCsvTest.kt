package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.LapisInfo
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.AminoAcidInsertionResponse
import org.genspectrum.lapis.response.AminoAcidMutationResponse
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.NucleotideInsertionResponse
import org.genspectrum.lapis.response.NucleotideMutationResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
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

        mockMvc.perform(get("$endpoint?country=Switzerland"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "application/json"))
            .andExpect(jsonPath("\$.data").isEmpty())
    }

    @ParameterizedTest(name = "POST {0} returns empty JSON")
    @MethodSource("getEndpoints")
    fun `POST returns empty json`(endpoint: String) {
        mockEndpointReturnEmptyList(endpoint)

        val request = post(endpoint)
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

        mockMvc.perform(get("$endpoint?country=Switzerland").header("Accept", "text/csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(""))
    }

    @ParameterizedTest(name = "POST {0} returns empty CSV")
    @MethodSource("getEndpoints")
    fun `POST {0} returns empty CSV`(endpoint: String) {
        mockEndpointReturnEmptyList(endpoint)

        val request = post(endpoint)
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)
            .accept("text/csv")

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(""))
    }

    @ParameterizedTest(name = "GET {0} returns as CSV with accept header")
    @MethodSource("getEndpoints")
    fun `GET returns as CSV with accept header`(endpoint: String) {
        mockEndpointReturnData(endpoint)

        mockMvc.perform(get("$endpoint?country=Switzerland").header("Accept", "text/csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(returnedCsvData(endpoint)))
    }

    @ParameterizedTest(name = "POST {0} returns as CSV with accept header")
    @MethodSource("getEndpoints")
    fun `POST returns as CSV with accept header`(endpoint: String) {
        mockEndpointReturnData(endpoint)

        val request = post(endpoint)
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)
            .accept("text/csv")

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(returnedCsvData(endpoint)))
    }

    @ParameterizedTest(name = "GET {0} returns as CSV with request parameter")
    @MethodSource("getEndpoints")
    fun `GET returns as CSV with request parameter`(endpoint: String) {
        mockEndpointReturnData(endpoint)

        mockMvc.perform(get("$endpoint?country=Switzerland&dataFormat=csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(returnedCsvData(endpoint)))
    }

    @ParameterizedTest(name = "POST {0} returns as CSV with request parameter")
    @MethodSource("getEndpoints")
    fun `POST returns as CSV with request parameter`(endpoint: String) {
        mockEndpointReturnData(endpoint)

        val request = post(endpoint)
            .content("""{"country": "Switzerland", "dataFormat": "csv"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(returnedCsvData(endpoint)))
    }

    @ParameterizedTest(name = "GET {0} returns as TSV with accept header")
    @MethodSource("getEndpoints")
    fun `GET returns as TSV with accept header`(endpoint: String) {
        mockEndpointReturnData(endpoint)

        mockMvc.perform(get("$endpoint?country=Switzerland").header("Accept", "text/tab-separated-values"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(returnedTsvData(endpoint)))
    }

    @ParameterizedTest(name = "POST {0} returns as TSV with accept header")
    @MethodSource("getEndpoints")
    fun `POST returns as TSV with accept header`(endpoint: String) {
        mockEndpointReturnData(endpoint)

        val request = post(endpoint)
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)
            .accept("text/tab-separated-values")

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(returnedTsvData(endpoint)))
    }

    @ParameterizedTest(name = "GET {0} returns as TSV with request parameter")
    @MethodSource("getEndpoints")
    fun `GET returns as TSV with request parameter`(endpoint: String) {
        mockEndpointReturnData(endpoint)

        mockMvc.perform(get("$endpoint?country=Switzerland&dataFormat=tsv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(returnedTsvData(endpoint)))
    }

    @ParameterizedTest(name = "POST {0} returns as TSV with request parameter")
    @MethodSource("getEndpoints")
    fun `POST returns as TSV with request parameter`(endpoint: String) {
        mockEndpointReturnData(endpoint)

        val request = post(endpoint)
            .content("""{"country": "Switzerland", "dataFormat": "tsv"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(returnedTsvData(endpoint)))
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
        fun getEndpoints() =
            listOf(
                Arguments.of(DETAILS_ROUTE),
                Arguments.of(AGGREGATED_ROUTE),
                Arguments.of(NUCLEOTIDE_MUTATIONS_ROUTE),
                Arguments.of(AMINO_ACID_MUTATIONS_ROUTE),
                Arguments.of(NUCLEOTIDE_INSERTIONS_ROUTE),
                Arguments.of(AMINO_ACID_INSERTIONS_ROUTE),
            )
    }

    private fun sequenceFiltersRequestWithFields(
        sequenceFilters: Map<String, String>,
        fields: List<String> = emptyList(),
    ) = SequenceFiltersRequestWithFields(
        sequenceFilters,
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        fields,
        emptyList(),
    )
}
