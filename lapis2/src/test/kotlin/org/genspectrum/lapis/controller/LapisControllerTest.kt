package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.TextNode
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.MutationProportionsRequest
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.MutationData
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class LapisControllerTest(@Autowired val mockMvc: MockMvc) {
    @MockkBean
    lateinit var siloQueryModelMock: SiloQueryModel

    @Test
    fun `GET aggregated`() {
        every {
            siloQueryModelMock.aggregate(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns listOf(
            AggregationData(
                0,
                mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42)),
            ),
        )

        mockMvc.perform(get("/aggregated?country=Switzerland"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[0].count").value(0))
            .andExpect(jsonPath("\$[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$[0].age").value(42))
    }

    @Test
    fun `POST aggregated`() {
        every {
            siloQueryModelMock.aggregate(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns listOf(
            AggregationData(
                0,
                emptyMap(),
            ),
        )
        val request = post("/aggregated")
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[0].count").value(0))
    }

    @Test
    fun `GET aggregated with fields`() {
        every {
            siloQueryModelMock.aggregate(
                sequenceFiltersRequestWithFields(
                    mapOf("country" to "Switzerland"),
                    listOf("country", "age"),
                ),
            )
        } returns listOf(
            AggregationData(
                0,
                mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42)),
            ),
        )

        mockMvc.perform(get("/aggregated?country=Switzerland&fields=country,age"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[0].count").value(0))
            .andExpect(jsonPath("\$[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$[0].age").value(42))
    }

    @Test
    fun `GET aggregated with invalid nucleotide mutation`() {
        mockMvc.perform(get("/aggregated?nucleotideMutations=invalidMutation"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.detail").value(containsString("Failed to convert 'nucleotideMutations'")))
    }

    @Test
    fun `GET aggregated with invalid amino acid mutation`() {
        mockMvc.perform(get("/aggregated?aminoAcidMutations=invalidMutation"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.detail").value(containsString("Failed to convert 'aminoAcidMutations'")))
    }

    @Test
    fun `GET aggregated with valid mutation`() {
        every {
            siloQueryModelMock.aggregate(
                SequenceFiltersRequestWithFields(
                    emptyMap(),
                    listOf(NucleotideMutation(null, 123, "A"), NucleotideMutation(null, 124, "B")),
                    emptyList(),
                    emptyList(),
                ),
            )
        } returns listOf(AggregationData(5, emptyMap()))

        mockMvc.perform(get("/aggregated?nucleotideMutations=123A,124B"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[0].count").value(5))
    }

    @Test
    fun `POST aggregated with fields`() {
        every {
            siloQueryModelMock.aggregate(
                sequenceFiltersRequestWithFields(
                    mapOf("country" to "Switzerland"),
                    listOf("country", "age"),
                ),
            )
        } returns listOf(
            AggregationData(
                0,
                mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42)),
            ),
        )

        val request = post("/aggregated")
            .content("""{"country": "Switzerland", "fields": ["country","age"]}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[0].count").value(0))
            .andExpect(jsonPath("\$[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$[0].age").value(42))
    }

    @Test
    fun `GET nucleotideMutations without explicit minProportion defaults to 5 percent`() {
        every {
            siloQueryModelMock.computeMutationProportions(
                mutationProportionsRequest(
                    mapOf("country" to "Switzerland"),
                    0.05,
                ),
            )
        } returns listOf(someMutationProportion())

        mockMvc.perform(get("/nucleotideMutations?country=Switzerland"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[0].position").value("the mutation"))
            .andExpect(jsonPath("\$[0].proportion").value(0.5))
            .andExpect(jsonPath("\$[0].count").value(42))
    }

    @Test
    fun `GET nucleotideMutations with minProportion`() {
        every {
            siloQueryModelMock.computeMutationProportions(
                mutationProportionsRequest(
                    mapOf("country" to "Switzerland"),
                    0.3,
                ),
            )
        } returns listOf(someMutationProportion())

        mockMvc.perform(get("/nucleotideMutations?country=Switzerland&minProportion=0.3"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[0].position").value("the mutation"))
            .andExpect(jsonPath("\$[0].proportion").value(0.5))
            .andExpect(jsonPath("\$[0].count").value(42))
    }

    @Test
    fun `POST nucleotideMutations without explicit minProportion defaults to 5 percent`() {
        every {
            siloQueryModelMock.computeMutationProportions(
                mutationProportionsRequest(
                    mapOf("country" to "Switzerland"),
                    0.05,
                ),
            )
        } returns listOf(someMutationProportion())

        val request = post("/nucleotideMutations")
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[0].position").value("the mutation"))
            .andExpect(jsonPath("\$[0].proportion").value(0.5))
            .andExpect(jsonPath("\$[0].count").value(42))
    }

    @Test
    fun `POST nucleotideMutations with minProportion`() {
        every {
            siloQueryModelMock.computeMutationProportions(
                mutationProportionsRequest(
                    mapOf("country" to "Switzerland"),
                    0.7,
                ),
            )
        } returns listOf(someMutationProportion())

        val request = post("/nucleotideMutations")
            .content("""{"country": "Switzerland", "minProportion": 0.7}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[0].position").value("the mutation"))
            .andExpect(jsonPath("\$[0].proportion").value(0.5))
            .andExpect(jsonPath("\$[0].count").value(42))
    }

    @Test
    fun `POST nucleotideMutations with invalid minProportion returns bad request`() {
        val request = post("/nucleotideMutations")
            .content("""{"country": "Switzerland", "minProportion": "this is not a float"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.title").value("Bad request"))
            .andExpect(
                jsonPath("\$.message").value("minProportion must be a number"),
            )
    }

    @Test
    fun `GET details`() {
        every {
            siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns listOf(mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42)))

        mockMvc.perform(get("/details?country=Switzerland"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$[0].age").value(42))
    }

    @Test
    fun `GET details with fields`() {
        every {
            siloQueryModelMock.getDetails(
                sequenceFiltersRequestWithFields(
                    mapOf("country" to "Switzerland"),
                    listOf("country", "age"),
                ),
            )
        } returns listOf(mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42)))

        mockMvc.perform(get("/details?country=Switzerland&fields=country&fields=age"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$[0].age").value(42))
    }

    @Test
    fun `POST details`() {
        every {
            siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns listOf(mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42)))

        val request = post("/details")
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$[0].age").value(42))
    }

    @Test
    fun `POST details with fields`() {
        every {
            siloQueryModelMock.getDetails(
                sequenceFiltersRequestWithFields(
                    mapOf("country" to "Switzerland"),
                    listOf("country", "age"),
                ),
            )
        } returns listOf(mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42)))

        val request = post("/details")
            .content("""{"country": "Switzerland", "fields": ["country", "age"]}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$[0].age").value(42))
    }

    private fun sequenceFiltersRequestWithFields(
        sequenceFilters: Map<String, String>,
        fields: List<String> = emptyList(),
    ) = SequenceFiltersRequestWithFields(
        sequenceFilters,
        emptyList(),
        emptyList(),
        fields,
    )

    private fun mutationProportionsRequest(sequenceFilters: Map<String, String>, minProportion: Double) =
        MutationProportionsRequest(
            sequenceFilters,
            emptyList(),
            emptyList(),
            minProportion,
        )

    private fun someMutationProportion() = MutationData("the mutation", 42, 0.5)
}
