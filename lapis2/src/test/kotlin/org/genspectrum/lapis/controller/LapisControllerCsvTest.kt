package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
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
            mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42), "floatValue" to DoubleNode(3.14)),
            mapOf("country" to TextNode("Switzerland"), "age" to IntNode(43), "floatValue" to NullNode.instance),
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
    fun `GET details as CSV with request parameter`() {
        every {
            siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns listOf(
            mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42)),
            mapOf("country" to TextNode("Switzerland"), "age" to IntNode(43)),
        )

        mockMvc.perform(get("/details?country=Switzerland&dataFormat=csv"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(
                content().string(
                    """
                        country,age
                        Switzerland,42
                        Switzerland,43
                    """.trimIndent(),
                ),
            )
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
    fun `POST details as CSV with request parameter`() {
        every {
            siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns emptyList()

        val request = post("/details")
            .content("""{"country": "Switzerland", "dataFormat": "csv"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(content().string(""))
    }

    private fun sequenceFiltersRequestWithFields(
        sequenceFilters: Map<String, String>,
        fields: List<String> = emptyList(),
    ) = SequenceFiltersRequestWithFields(
        sequenceFilters,
        emptyList(),
        emptyList(),
        fields,
        emptyList(),
    )
}
