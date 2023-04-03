package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.response.AggregatedResponse
import org.genspectrum.lapis.response.MutationProportion
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest
class LapisControllerTest(@Autowired val mockMvc: MockMvc) {
    @MockkBean
    lateinit var siloQueryModelMock: SiloQueryModel

    @Test
    fun `GET aggregated`() {
        every { siloQueryModelMock.aggregate(mapOf("country" to "Switzerland")) } returns AggregatedResponse(0)

        mockMvc.perform(get("/aggregated?country=Switzerland"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.count").value(0))
    }

    @Test
    fun `POST aggregated`() {
        every { siloQueryModelMock.aggregate(mapOf("country" to "Switzerland")) } returns AggregatedResponse(0)

        val request = post("/aggregated")
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.count").value(0))
    }

    @Test
    fun `GET nucleotideMutations without explicit minProportion defaults to 5 percent`() {
        every {
            siloQueryModelMock.computeMutationProportions(
                0.05,
                mapOf("country" to "Switzerland"),
            )
        } returns listOf(MutationProportion("the mutation", 42, 0.5))

        mockMvc.perform(get("/nucleotideMutations?country=Switzerland"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[0].mutation").value("the mutation"))
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
        } returns listOf(MutationProportion("the mutation", 42, 0.5))

        mockMvc.perform(get("/nucleotideMutations?country=Switzerland&minProportion=0.3"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[0].mutation").value("the mutation"))
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
        } returns listOf(MutationProportion("the mutation", 42, 0.5))

        val request = post("/nucleotideMutations")
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[0].mutation").value("the mutation"))
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
        } returns listOf(MutationProportion("the mutation", 42, 0.5))

        val request = post("/nucleotideMutations")
            .content("""{"country": "Switzerland", "minProportion": 0.7}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[0].mutation").value("the mutation"))
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
}
