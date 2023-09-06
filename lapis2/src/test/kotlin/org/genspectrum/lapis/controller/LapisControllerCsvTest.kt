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
import org.genspectrum.lapis.response.NucleotideMutationResponse
import org.junit.jupiter.api.Test
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

    val listOfMetadata = listOf(
        DetailsData(mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42))),
        DetailsData(mapOf("country" to TextNode("Switzerland"), "age" to IntNode(43))),
    )

    val metadataCsv = """
        country,age
        Switzerland,42
        Switzerland,43
    """.trimIndent()

    val metadataTsv = """
        country	age
        Switzerland	42
        Switzerland	43
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

    @Test
    fun `GET empty details return empty CSV`() {
        every {
            siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns emptyList()

        mockMvc.perform(get("/details?country=Switzerland").header("Accept", "text/csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(""))
    }

    @Test
    fun `GET details as CSV with accept header`() {
        every {
            siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns listOf(
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

        mockMvc.perform(get("/details?country=Switzerland").header("Accept", "text/csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(
                content().string(
                    """
                       country,age,floatValue
                       Switzerland,42,3.14
                       Switzerland,43,null
                    """.trimIndent(),
                ),
            )
    }

    @Test
    fun `GET details as TSV with accept header`() {
        every {
            siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns listOfMetadata

        mockMvc.perform(get("/details?country=Switzerland").header("Accept", "text/tab-separated-values"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(metadataTsv))
    }

    @Test
    fun `GET details as CSV with request parameter`() {
        every {
            siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns listOfMetadata

        mockMvc.perform(get("/details?country=Switzerland&dataFormat=csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(metadataCsv))
    }

    @Test
    fun `GET details as TSV with request parameter`() {
        every {
            siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns listOfMetadata

        mockMvc.perform(get("/details?country=Switzerland&dataFormat=tsv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(metadataTsv))
    }

    @Test
    fun `POST details returns empty CSV`() {
        every {
            siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns emptyList()

        val request = post("/details")
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)
            .accept("text/csv")

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(""))
    }

    @Test
    fun `POST details as CSV with accept header`() {
        every {
            siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns listOfMetadata

        val request = post("/details")
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)
            .accept("text/csv")

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(metadataCsv))
    }

    @Test
    fun `POST details as TSV with accept header`() {
        every {
            siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns listOfMetadata

        val request = post("/details")
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)
            .accept("text/tab-separated-values")

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(metadataTsv))
    }

    @Test
    fun `POST details as CSV with request parameter`() {
        every {
            siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns listOfMetadata

        val request = post("/details")
            .content("""{"country": "Switzerland", "dataFormat": "csv"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(metadataCsv))
    }

    @Test
    fun `POST details as TSV with request parameter`() {
        every {
            siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns listOfMetadata

        val request = post("/details")
            .content("""{"country": "Switzerland", "dataFormat": "tsv"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(metadataTsv))
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

    @Test
    fun `GET aggregated returns empty CSV`() {
        every { siloQueryModelMock.getAggregated(any()) } returns emptyList()

        mockMvc.perform(get("/aggregated?country=Switzerland").header("Accept", "text/csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(""))
    }

    @Test
    fun `GET aggregated as CSV with accept header`() {
        every { siloQueryModelMock.getAggregated(any()) } returns aggregationData

        mockMvc.perform(get("/aggregated?country=Switzerland").header("Accept", "text/csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(aggregationDataCsv))
    }

    @Test
    fun `GET aggregated as TSV with accept header`() {
        every { siloQueryModelMock.getAggregated(any()) } returns aggregationData

        mockMvc.perform(get("/aggregated?country=Switzerland").header("Accept", "text/tab-separated-values"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(aggregationDataTsv))
    }

    @Test
    fun `GET aggregated as CSV with request parameter`() {
        every { siloQueryModelMock.getAggregated(any()) } returns aggregationData

        mockMvc.perform(get("/aggregated?country=Switzerland&dataFormat=csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(aggregationDataCsv))
    }

    @Test
    fun `GET aggregated as TSV with request parameter`() {
        every { siloQueryModelMock.getAggregated(any()) } returns aggregationData

        mockMvc.perform(get("/aggregated?country=Switzerland&dataFormat=tsv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(aggregationDataTsv))
    }

    @Test
    fun `POST aggregated returns empty CSV`() {
        every { siloQueryModelMock.getAggregated(any()) } returns emptyList()

        val request = post("/aggregated")
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)
            .accept("text/csv")

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(""))
    }

    @Test
    fun `POST aggregated as CSV with accept header`() {
        every { siloQueryModelMock.getAggregated(any()) } returns aggregationData

        val request = post("/aggregated")
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)
            .accept("text/csv")

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(aggregationDataCsv))
    }

    @Test
    fun `POST aggregated as TSV with accept header`() {
        every { siloQueryModelMock.getAggregated(any()) } returns aggregationData

        val request = post("/aggregated")
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)
            .accept("text/tab-separated-values")

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(aggregationDataTsv))
    }

    @Test
    fun `POST aggregated as CSV with request parameter`() {
        every { siloQueryModelMock.getAggregated(any()) } returns aggregationData

        val request = post("/aggregated")
            .content("""{"country": "Switzerland", "dataFormat": "csv"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(aggregationDataCsv))
    }

    @Test
    fun `POST aggregated as TSV with request parameter`() {
        every { siloQueryModelMock.getAggregated(any()) } returns aggregationData

        val request = post("/aggregated")
            .content("""{"country": "Switzerland", "dataFormat": "tsv"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(aggregationDataTsv))
    }

    @Test
    fun `POST nucleotideMutations returns empty CSV`() {
        every { siloQueryModelMock.computeNucleotideMutationProportions(any()) } returns emptyList()

        val request = post("/nucleotideMutations")
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)
            .accept("text/csv")

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(""))
    }

    @Test
    fun `POST nucleotideMutations as CSV with accept header`() {
        every { siloQueryModelMock.computeNucleotideMutationProportions(any()) } returns nucleotideMutationData

        val request = post("/nucleotideMutations")
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)
            .accept("text/csv")

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(mutationDataCsv))
    }

    @Test
    fun `POST nucleotideMutations as TSV with accept header`() {
        every { siloQueryModelMock.computeNucleotideMutationProportions(any()) } returns nucleotideMutationData

        val request = post("/nucleotideMutations")
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)
            .accept("text/tab-separated-values")

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(mutationDataTsv))
    }

    @Test
    fun `GET nucleotideMutations returns empty CSV`() {
        every { siloQueryModelMock.computeNucleotideMutationProportions(any()) } returns emptyList()

        mockMvc.perform(get("/nucleotideMutations?country=Switzerland").header("Accept", "text/csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(""))
    }

    @Test
    fun `GET nucleotideMutations as CSV with accept header`() {
        every { siloQueryModelMock.computeNucleotideMutationProportions(any()) } returns nucleotideMutationData
        mockMvc.perform(get("/nucleotideMutations?country=Switzerland").header("Accept", "text/csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(mutationDataCsv))
    }

    @Test
    fun `GET nucleotideMutations as TSV with accept header`() {
        every { siloQueryModelMock.computeNucleotideMutationProportions(any()) } returns nucleotideMutationData
        mockMvc.perform(get("/nucleotideMutations?country=Switzerland").header("Accept", "text/tab-separated-values"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(mutationDataTsv))
    }

    @Test
    fun `GET nucleotideMutations as CSV with request parameter`() {
        every { siloQueryModelMock.computeNucleotideMutationProportions(any()) } returns nucleotideMutationData
        mockMvc.perform(get("/nucleotideMutations?country=Switzerland&dataFormat=csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(mutationDataCsv))
    }

    @Test
    fun `GET nucleotideMutations as TSV with request parameter`() {
        every { siloQueryModelMock.computeNucleotideMutationProportions(any()) } returns nucleotideMutationData
        mockMvc.perform(get("/nucleotideMutations?country=Switzerland&dataFormat=tsv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(mutationDataTsv))
    }

    @Test
    fun `POST aminoAcidMutations returns empty CSV`() {
        every { siloQueryModelMock.computeAminoAcidMutationProportions(any()) } returns emptyList()

        val request = post("/aminoAcidMutations")
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)
            .accept("text/csv")

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(""))
    }

    @Test
    fun `POST aminoAcidMutations as CSV with accept header`() {
        every { siloQueryModelMock.computeAminoAcidMutationProportions(any()) } returns aminoAcidMutationData

        val request = post("/aminoAcidMutations")
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)
            .accept("text/csv")

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(mutationDataCsv))
    }

    @Test
    fun `POST aminoAcidMutations as TSV with accept header`() {
        every { siloQueryModelMock.computeAminoAcidMutationProportions(any()) } returns aminoAcidMutationData

        val request = post("/aminoAcidMutations")
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)
            .accept("text/tab-separated-values")

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(mutationDataTsv))
    }

    @Test
    fun `GET aminoAcidMutations returns empty CSV`() {
        every { siloQueryModelMock.computeAminoAcidMutationProportions(any()) } returns emptyList()

        mockMvc.perform(get("/aminoAcidMutations?country=Switzerland").header("Accept", "text/csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(""))
    }

    @Test
    fun `GET aminoAcidMutations as CSV with accept header`() {
        every { siloQueryModelMock.computeAminoAcidMutationProportions(any()) } returns aminoAcidMutationData
        mockMvc.perform(get("/aminoAcidMutations?country=Switzerland").header("Accept", "text/csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(mutationDataCsv))
    }

    @Test
    fun `GET aminoAcidMutations as TSV with accept header`() {
        every { siloQueryModelMock.computeAminoAcidMutationProportions(any()) } returns aminoAcidMutationData
        mockMvc.perform(get("/aminoAcidMutations?country=Switzerland").header("Accept", "text/tab-separated-values"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(mutationDataTsv))
    }

    @Test
    fun `GET aminoAcidMutations as CSV with request parameter`() {
        every { siloQueryModelMock.computeAminoAcidMutationProportions(any()) } returns aminoAcidMutationData
        mockMvc.perform(get("/aminoAcidMutations?country=Switzerland&dataFormat=csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(mutationDataCsv))
    }

    @Test
    fun `GET aminoAcidMutations as TSV with request parameter`() {
        every { siloQueryModelMock.computeAminoAcidMutationProportions(any()) } returns aminoAcidMutationData
        mockMvc.perform(get("/aminoAcidMutations?country=Switzerland&dataFormat=tsv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/tab-separated-values;charset=UTF-8"))
            .andExpect(content().string(mutationDataTsv))
    }
}
