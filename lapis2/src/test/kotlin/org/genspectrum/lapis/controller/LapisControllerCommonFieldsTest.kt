package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.node.TextNode
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.Order
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.AggregationData
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
class LapisControllerCommonFieldsTest(@Autowired val mockMvc: MockMvc) {
    @MockkBean
    lateinit var siloQueryModelMock: SiloQueryModel

    @Test
    fun `GET aggregated with a single orderBy field`() {
        every {
            siloQueryModelMock.getAggregated(
                SequenceFiltersRequestWithFields(
                    emptyMap(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    listOf(OrderByField("country", Order.ASCENDING)),
                ),
            )
        } returns listOf(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

        mockMvc.perform(get("/aggregated?orderBy=country"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
    }

    @Test
    fun `GET aggregated with orderBy fields`() {
        every {
            siloQueryModelMock.getAggregated(
                SequenceFiltersRequestWithFields(
                    emptyMap(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    listOf(OrderByField("country", Order.ASCENDING), OrderByField("date", Order.ASCENDING)),
                ),
            )
        } returns listOf(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

        mockMvc.perform(get("/aggregated?orderBy=country,date"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
    }

    @Test
    fun `POST aggregated with flat orderBy fields`() {
        every {
            siloQueryModelMock.getAggregated(
                SequenceFiltersRequestWithFields(
                    emptyMap(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    listOf(OrderByField("country", Order.ASCENDING), OrderByField("date", Order.ASCENDING)),
                ),
            )
        } returns listOf(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

        val request = post("/aggregated")
            .content("""{"orderBy": ["country", "date"]}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
    }

    @Test
    fun `POST aggregated with ascending and descending orderBy fields`() {
        every {
            siloQueryModelMock.getAggregated(
                SequenceFiltersRequestWithFields(
                    emptyMap(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    listOf(
                        OrderByField("country", Order.DESCENDING),
                        OrderByField("date", Order.ASCENDING),
                        OrderByField("age", Order.ASCENDING),
                    ),
                ),
            )
        } returns listOf(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

        val request = post("/aggregated")
            .content(
                """
                {
                    "orderBy": [
                        { "field": "country", "type": "descending" },
                        { "field": "date", "type": "ascending" },
                        { "field": "age" }
                    ]
                }
                """.trimIndent(),
            )
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
    }

    @Test
    fun `POST aggregated with invalid orderBy fields`() {
        val request = post("/aggregated")
            .content("""{"orderBy": [ { "field": ["this is an array, not a string"] } ]}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.error.message").value("orderByField must have a string property \"field\""))
    }

    @Test
    fun `GET aggregated with limit`() {
        every {
            siloQueryModelMock.getAggregated(
                SequenceFiltersRequestWithFields(
                    emptyMap(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    100,
                ),
            )
        } returns listOf(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

        mockMvc.perform(get("/aggregated?limit=100"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
    }

    @Test
    fun `POST aggregated with limit`() {
        every {
            siloQueryModelMock.getAggregated(
                SequenceFiltersRequestWithFields(
                    emptyMap(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    100,
                ),
            )
        } returns listOf(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

        val request = post("/aggregated")
            .content("""{"limit": 100}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
    }

    @Test
    fun `POST aggregated with invalid limit`() {
        val request = post("/aggregated")
            .content("""{"limit": "this is not a number"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.error.message").value("limit must be a number or null"))
    }

    @Test
    fun `GET aggregated with offset`() {
        every {
            siloQueryModelMock.getAggregated(
                SequenceFiltersRequestWithFields(
                    emptyMap(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    null,
                    5,
                ),
            )
        } returns listOf(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

        mockMvc.perform(get("/aggregated?offset=5"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
    }

    @Test
    fun `POST aggregated with offset`() {
        every {
            siloQueryModelMock.getAggregated(
                SequenceFiltersRequestWithFields(
                    emptyMap(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    null,
                    5,
                ),
            )
        } returns listOf(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

        val request = post("/aggregated")
            .content("""{"offset": 5}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
    }

    @Test
    fun `POST aggregated with invalid offset`() {
        val request = post("/aggregated")
            .content("""{"offset": "this is not a number"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.error.message").value("offset must be a number or null"))
    }
}
