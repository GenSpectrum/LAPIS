package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.TextNode
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.MutationData
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
        every { siloQueryModelMock.aggregate(mapOf("country" to "Switzerland")) } returns listOf(
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
        every { siloQueryModelMock.aggregate(mapOf("country" to "Switzerland")) } returns listOf(
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
                mapOf("country" to "Switzerland"),
                listOf("country", "age"),
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
    fun `POST aggregated with fields`() {
        every {
            siloQueryModelMock.aggregate(
                mapOf("country" to "Switzerland"),
                listOf("country", "age"),
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
                0.05,
                mapOf("country" to "Switzerland"),
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
                0.3,
                mapOf("country" to "Switzerland"),
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
                0.05,
                mapOf("country" to "Switzerland"),
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
                0.7,
                mapOf("country" to "Switzerland"),
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
                jsonPath("\$.message").value("Invalid minProportion: Could not parse 'this is not a float' to float."),
            )
    }

    @Test
    fun `GET details`() {
        every {
            siloQueryModelMock.getDetails(
                mapOf("country" to "Switzerland"),
                emptyList(),
            )
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
                mapOf("country" to "Switzerland"),
                listOf("country", "age"),
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
            siloQueryModelMock.getDetails(
                mapOf("country" to "Switzerland"),
                emptyList(),
            )
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
                mapOf("country" to "Switzerland"),
                listOf("country", "age"),
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

    private fun someMutationProportion() = MutationData("the mutation", 42, 0.5)
}
