package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.AminoAcidMutationResponse
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.NucleotideInsertionResponse
import org.genspectrum.lapis.response.NucleotideMutationResponse
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class LapisControllerCsvTest(@Autowired val mockMvc: MockMvc) {
    @MockkBean
    lateinit var siloQueryModelMock: SiloQueryModel

    @ParameterizedTest(name = "GET empty {0}")
    @MethodSource("getEndpoints")
    fun `GET empty return empty CSV`(
        endpoint: String,
    ) {
        mockEndpointReturnEmptyList(endpoint)

        mockMvc.perform(get(endpoint + "?country=Switzerland").header("Accept", "text/csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(""))
    }

    @ParameterizedTest(name = "POST empty {0}")
    @MethodSource("getEndpoints")
    fun `POST empty {0} return empty CSV`(
        endpoint: String,
    ) {
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
    fun `GET returns as CSV with accept header`(
        endpoint: String,
    ) {
        mockEndpointReturnData(endpoint)

        mockMvc.perform(get(endpoint + "?country=Switzerland").header("Accept", "text/csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(returnedCsvData(endpoint)))
    }

    @ParameterizedTest(name = "POST {0} returns as CSV with accept header")
    @MethodSource("getEndpoints")
    fun `POST returns as CSV with accept header`(
        endpoint: String,
    ) {
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
    fun `GET returns as CSV with request parameter`(
        endpoint: String,
    ) {
        mockEndpointReturnData(endpoint)

        mockMvc.perform(get(endpoint + "?country=Switzerland&dataFormat=csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(returnedCsvData(endpoint)))
    }

    @ParameterizedTest(name = "POST {0} returns as CSV with request parameter")
    @MethodSource("getEndpoints")
    fun `POST returns as CSV with request parameter`(
        endpoint: String,
    ) {
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
    fun `GET returns as TSV with accept header`(
        endpoint: String,
    ) {
        mockEndpointReturnData(endpoint)

        mockMvc.perform(get(endpoint + "?country=Switzerland").header("Accept", "text/tab-separated-values"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(returnedTsvData(endpoint)))
    }

    @ParameterizedTest(name = "POST {0} returns as TSV with accept header")
    @MethodSource("getEndpoints")
    fun `POST returns as TSV with accept header`(
        endpoint: String,
    ) {
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
    fun `GET returns as TSV with request parameter`(
        endpoint: String,
    ) {
        mockEndpointReturnData(endpoint)

        mockMvc.perform(get(endpoint + "?country=Switzerland&dataFormat=tsv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(returnedTsvData(endpoint)))
    }

    @ParameterizedTest(name = "POST {0} returns as TSV with request parameter")
    @MethodSource("getEndpoints")
    fun `POST returns as TSV with request parameter`(
        endpoint: String,
    ) {
        mockEndpointReturnData(endpoint)

        val request = post(endpoint)
            .content("""{"country": "Switzerland", "dataFormat": "tsv"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(returnedTsvData(endpoint)))
    }

    fun mockEndpointReturnEmptyList(endpoint: String) {
        if (endpoint == "/details") {
            every {
                siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
            } returns emptyList()
        }
        if (endpoint == "/aggregated") {
            every {
                siloQueryModelMock.getAggregated(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
            } returns emptyList()
        }
        if (endpoint == "/nucleotideMutations") {
            every { siloQueryModelMock.computeNucleotideMutationProportions(any()) } returns emptyList()
        }
        if (endpoint == "/aminoAcidMutations") {
            every { siloQueryModelMock.computeAminoAcidMutationProportions(any()) } returns emptyList()
        }
        if (endpoint == "/nucleotideInsertions") {
            every { siloQueryModelMock.getNucleotideInsertions(any()) } returns emptyList()
        }
    }

    fun mockEndpointReturnData(endpoint: String) {
        if (endpoint == "/details") {
            every {
                siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
            } returns detailsData
        }
        if (endpoint == "/aggregated") {
            every {
                siloQueryModelMock.getAggregated(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
            } returns aggregationData
        }
        if (endpoint == "/nucleotideMutations") {
            every { siloQueryModelMock.computeNucleotideMutationProportions(any()) } returns nucleotideMutationData
        }
        if (endpoint == "/aminoAcidMutations") {
            every { siloQueryModelMock.computeAminoAcidMutationProportions(any()) } returns aminoAcidMutationData
        }
        if (endpoint == "/nucleotideInsertions") {
            every { siloQueryModelMock.getNucleotideInsertions(any()) } returns nucleotideInsertionData
        }
    }

    fun returnedCsvData(endpoint: String): String {
        if (endpoint == "/details") {
            return detailsDataCsv
        }
        if (endpoint == "/aggregated") {
            return aggregationDataCsv
        }
        if (endpoint == "/nucleotideMutations") {
            return mutationDataCsv
        }
        if (endpoint == "/aminoAcidMutations") {
            return mutationDataCsv
        }
        if (endpoint == "/nucleotideInsertions") {
            return nucleotideInsertionDataCsv
        }
        return ""
    }

    fun returnedTsvData(endpoint: String): String {
        if (endpoint == "/details") {
            return detailsDataTsv
        }
        if (endpoint == "/aggregated") {
            return aggregationDataTsv
        }
        if (endpoint == "/nucleotideMutations") {
            return mutationDataTsv
        }
        if (endpoint == "/aminoAcidMutations") {
            return mutationDataTsv
        }
        if (endpoint == "/nucleotideInsertions") {
            return nucleotideInsertionDataTsv
        }
        return ""
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

    private companion object {
        @JvmStatic
        fun getEndpoints() = listOf(
            Arguments.of("/details"),
            Arguments.of("/aggregated"),
            Arguments.of("/nucleotideMutations"),
            Arguments.of("/aminoAcidMutations"),
            Arguments.of("/nucleotideInsertions"),
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
