package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.node.TextNode
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.FIELD_WITH_ONLY_LOWERCASE_LETTERS
import org.genspectrum.lapis.FIELD_WITH_UPPERCASE_LETTER
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.AminoAcidInsertion
import org.genspectrum.lapis.request.LapisInfo
import org.genspectrum.lapis.request.NucleotideInsertion
import org.genspectrum.lapis.request.Order
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.AggregationData
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class LapisControllerCommonFieldsTest(
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

    @Test
    fun `GET aggregated with a single orderBy field`() {
        every {
            siloQueryModelMock.getAggregated(
                SequenceFiltersRequestWithFields(
                    emptyMap(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    listOf(OrderByField("country", Order.ASCENDING)),
                ),
            )
        } returns listOf(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

        mockMvc.perform(getSample("$AGGREGATED_ROUTE?orderBy=country"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
    }

    @Test
    fun `GET aggregated with orderBy fields is case insensitive for configured fields`() {
        every {
            siloQueryModelMock.getAggregated(
                SequenceFiltersRequestWithFields(
                    emptyMap(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    listOf(
                        OrderByField("country", Order.ASCENDING),
                        OrderByField(FIELD_WITH_ONLY_LOWERCASE_LETTERS, Order.ASCENDING),
                        OrderByField(FIELD_WITH_UPPERCASE_LETTER, Order.ASCENDING),
                    ),
                ),
            )
        } returns listOf(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

        val uppercaseField = FIELD_WITH_ONLY_LOWERCASE_LETTERS.uppercase()
        val lowercaseField = FIELD_WITH_UPPERCASE_LETTER.lowercase()
        mockMvc.perform(getSample("$AGGREGATED_ROUTE?orderBy=country,$uppercaseField,$lowercaseField"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
    }

    @Test
    fun `POST aggregated with flat orderBy fields is case insensitive for configured fields`() {
        every {
            siloQueryModelMock.getAggregated(
                SequenceFiltersRequestWithFields(
                    emptyMap(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    listOf(
                        OrderByField("country", Order.ASCENDING),
                        OrderByField(FIELD_WITH_ONLY_LOWERCASE_LETTERS, Order.ASCENDING),
                        OrderByField(FIELD_WITH_UPPERCASE_LETTER, Order.ASCENDING),
                    ),
                ),
            )
        } returns listOf(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

        val request = postSample(AGGREGATED_ROUTE)
            .content(
                """
                {
                    "orderBy": [
                        "country",
                        "${FIELD_WITH_ONLY_LOWERCASE_LETTERS.uppercase()}",
                        "${FIELD_WITH_UPPERCASE_LETTER.lowercase()}"
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
    fun `POST aggregated with ascending and descending orderBy fields is case insensitive for configured fields`() {
        every {
            siloQueryModelMock.getAggregated(
                SequenceFiltersRequestWithFields(
                    emptyMap(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    listOf(
                        OrderByField(FIELD_WITH_ONLY_LOWERCASE_LETTERS, Order.DESCENDING),
                        OrderByField(FIELD_WITH_UPPERCASE_LETTER, Order.ASCENDING),
                        OrderByField("age", Order.ASCENDING),
                    ),
                ),
            )
        } returns listOf(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

        val request = postSample(AGGREGATED_ROUTE)
            .content(
                """
                {
                    "orderBy": [
                        { "field": "${FIELD_WITH_ONLY_LOWERCASE_LETTERS.uppercase()}", "type": "descending" },
                        { "field": "${FIELD_WITH_UPPERCASE_LETTER.lowercase()}", "type": "ascending" },
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
        val request = postSample(AGGREGATED_ROUTE)
            .content("""{"orderBy": [ { "field": ["this is an array, not a string"] } ]}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath(
                    "\$.error.detail",
                    containsString("orderByField must have a string property \"field\""),
                ),
            )
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
                    emptyList(),
                    emptyList(),
                    100,
                ),
            )
        } returns listOf(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

        mockMvc.perform(getSample("$AGGREGATED_ROUTE?limit=100"))
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
                    emptyList(),
                    emptyList(),
                    100,
                ),
            )
        } returns listOf(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

        val request = postSample(AGGREGATED_ROUTE)
            .content("""{"limit": 100}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
    }

    @Test
    fun `POST aggregated with invalid limit`() {
        val request = postSample(AGGREGATED_ROUTE)
            .content("""{"limit": "this is not a number"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.error.detail").value("limit must be a number or null"))
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
                    emptyList(),
                    emptyList(),
                    null,
                    5,
                ),
            )
        } returns listOf(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

        mockMvc.perform(getSample("$AGGREGATED_ROUTE?offset=5"))
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
                    emptyList(),
                    emptyList(),
                    null,
                    5,
                ),
            )
        } returns listOf(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

        val request = postSample(AGGREGATED_ROUTE)
            .content("""{"offset": 5}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
    }

    @Test
    fun `POST aggregated with invalid offset`() {
        val request = postSample(AGGREGATED_ROUTE)
            .content("""{"offset": "this is not a number"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.error.detail").value("offset must be a number or null"))
    }

    @Test
    fun `GET aggregated with valid nucleotideInsertion`() {
        every {
            siloQueryModelMock.getAggregated(
                SequenceFiltersRequestWithFields(
                    emptyMap(),
                    emptyList(),
                    emptyList(),
                    listOf(NucleotideInsertion(123, "ABC", null), NucleotideInsertion(124, "DEF", "other_segment")),
                    emptyList(),
                    emptyList(),
                ),
            )
        } returns listOf(AggregationData(5, emptyMap()))

        mockMvc.perform(getSample("$AGGREGATED_ROUTE?nucleotideInsertions=ins_123:ABC,ins_other_segment:124:DEF"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(5))
    }

    @Test
    fun `GET aggregated with valid aminoAcidInsertions`() {
        every {
            siloQueryModelMock.getAggregated(
                SequenceFiltersRequestWithFields(
                    emptyMap(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    listOf(AminoAcidInsertion(123, "gene1", "ABC"), AminoAcidInsertion(124, "gene2", "DEF")),
                    emptyList(),
                ),
            )
        } returns listOf(AggregationData(5, emptyMap()))

        mockMvc.perform(getSample("$AGGREGATED_ROUTE?aminoAcidInsertions=ins_gene1:123:ABC,ins_gene2:124:DEF"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(5))
    }

    @ParameterizedTest(name = "GET {0} with invalid nucleotide mutation")
    @MethodSource("getEndpointsWithNucleotideMutationFilter")
    fun `GET endpoint with invalid nucleotide mutation filter`(endpoint: String) {
        mockMvc.perform(getSample("$endpoint?nucleotideMutations=invalidMutation"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.detail").value(Matchers.containsString("Failed to convert 'nucleotideMutations'")))
    }

    @ParameterizedTest(name = "GET {0} with invalid nucleotide mutation")
    @MethodSource("getEndpointsWithAminoAcidMutationFilter")
    fun `GET endpoind with invalid amino acid mutation`(endpoint: String) {
        mockMvc.perform(getSample("$endpoint?aminoAcidMutations=invalidMutation"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.detail").value(Matchers.containsString("Failed to convert 'aminoAcidMutations'")))
    }

    @ParameterizedTest(name = "GET {0} with invalid nucleotideInsertion")
    @MethodSource("getEndpointsWithInsertionFilter")
    fun `GET with invalid nucleotide insertion filter`(endpoint: String) {
        mockMvc.perform(getSample("$endpoint?nucleotideInsertions=invalidInsertion"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.detail").value(Matchers.containsString("Failed to convert 'nucleotideInsertions'")))
    }

    @ParameterizedTest(name = "GET {0} with invalid aminoAcidInsertion")
    @MethodSource("getEndpointsWithInsertionFilter")
    fun `GET with invalid amino acid insertionFilter`(endpoint: String) {
        mockMvc.perform(getSample("$endpoint?aminoAcidInsertions=invalidInsertion"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.detail").value(Matchers.containsString("Failed to convert 'aminoAcidInsertions'")))
    }

    private companion object {
        fun endpointsOfController() =
            listOf(
                Arguments.of(NUCLEOTIDE_MUTATIONS_ROUTE),
                Arguments.of(AMINO_ACID_MUTATIONS_ROUTE),
                Arguments.of(AGGREGATED_ROUTE),
                Arguments.of(DETAILS_ROUTE),
                Arguments.of(NUCLEOTIDE_INSERTIONS_ROUTE),
                Arguments.of(AMINO_ACID_INSERTIONS_ROUTE),
                Arguments.of("$ALIGNED_AMINO_ACID_SEQUENCES_ROUTE/S"),
            )

        @JvmStatic
        fun getEndpointsWithInsertionFilter() = endpointsOfController()

        @JvmStatic
        fun getEndpointsWithNucleotideMutationFilter() = endpointsOfController()

        @JvmStatic
        fun getEndpointsWithAminoAcidMutationFilter() = endpointsOfController()
    }
}
