package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.node.TextNode
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.FIELD_WITH_ONLY_LOWERCASE_LETTERS
import org.genspectrum.lapis.FIELD_WITH_UPPERCASE_LETTER
import org.genspectrum.lapis.controller.SampleRoute.AGGREGATED
import org.genspectrum.lapis.controller.SampleRoute.DETAILS
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.AminoAcidInsertion
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.NucleotideInsertion
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.Order
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.LapisInfo
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
import org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.stream.Stream

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
        } returns Stream.of(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

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
        } returns Stream.of(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

        val uppercaseField = FIELD_WITH_ONLY_LOWERCASE_LETTERS.uppercase()
        val lowercaseField = FIELD_WITH_UPPERCASE_LETTER.lowercase()
        mockMvc.perform(getSample("$AGGREGATED_ROUTE?orderBy=country,$uppercaseField,$lowercaseField"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
    }

    @ParameterizedTest(name = "POST {0} aggregated with flat orderBy fields is case insensitive for configured fields")
    @MethodSource("getAggregatedPostRequestsWithFlatOrderByFields")
    fun `POST aggregated with flat orderBy fields is case insensitive for configured fields`(
        description: String,
        request: MockHttpServletRequestBuilder,
    ) {
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
        } returns Stream.of(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

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
        } returns Stream.of(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

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
            .contentType(APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
    }

    @Test
    fun `POST aggregated with invalid orderBy fields`() {
        val request = postSample(AGGREGATED_ROUTE)
            .content("""{"orderBy": [ { "field": ["this is an array, not a string"] } ]}""")
            .contentType(APPLICATION_JSON)

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
        } returns Stream.of(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

        mockMvc.perform(getSample("$AGGREGATED_ROUTE?limit=100"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
    }

    @ParameterizedTest(name = "POST {0} aggregated with limit")
    @MethodSource("getAggregatedPostRequestsWithLimit")
    fun `POST aggregated with limit`(
        description: String,
        requestWithLimit: (Any) -> MockHttpServletRequestBuilder,
    ) {
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
        } returns Stream.of(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

        mockMvc.perform(requestWithLimit(100))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
    }

    @ParameterizedTest(name = "POST {0} aggregated with invalid limit")
    @MethodSource("getAggregatedPostRequestsWithLimit")
    fun `POST aggregated with invalid limit`(
        description: String,
        requestWithLimit: (Any) -> MockHttpServletRequestBuilder,
    ) {
        mockMvc.perform(requestWithLimit("this is not a number"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.error.detail").value(containsString("limit must be a number or null")))
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
        } returns Stream.of(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

        mockMvc.perform(getSample("$AGGREGATED_ROUTE?offset=5"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
    }

    @ParameterizedTest(name = "POST {0} aggregated with offset")
    @MethodSource("getAggregatedPostRequestsWithOffset")
    fun `POST aggregated with offset`(
        description: String,
        requestWithOffset: (Any) -> MockHttpServletRequestBuilder,
    ) {
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
        } returns Stream.of(AggregationData(0, mapOf("country" to TextNode("Switzerland"))))

        mockMvc.perform(requestWithOffset(5))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
    }

    @ParameterizedTest(name = "POST {0} aggregated with invalid offset")
    @MethodSource("getAggregatedPostRequestsWithOffset")
    fun `POST aggregated with invalid offset`(
        description: String,
        requestWithOffset: (Any) -> MockHttpServletRequestBuilder,
    ) {
        mockMvc.perform(requestWithOffset("this is not a number"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.error.detail").value(containsString("offset must be a number or null")))
    }

    @ParameterizedTest(name = "{0} aggregated with valid nucleotideInsertion")
    @MethodSource("getAggregatedRequestsWithNucleotideInsertions")
    fun `aggregated with valid nucleotideInsertion`(
        description: String,
        request: MockHttpServletRequestBuilder,
    ) {
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
        } returns Stream.of(AggregationData(5, emptyMap()))

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(5))
    }

    @ParameterizedTest(name = "{0} aggregated with valid aminoAcidInsertions")
    @MethodSource("getAggregatedRequestsWithAminoAcidInsertions")
    fun `aggregated with valid aminoAcidInsertions`(
        description: String,
        request: MockHttpServletRequestBuilder,
    ) {
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
        } returns Stream.of(AggregationData(5, emptyMap()))

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(5))
    }

    @ParameterizedTest(name = "{0} aggregated with valid nucleotideMutations")
    @MethodSource("getAggregatedRequestsWithNucleotideMutations")
    fun `aggregated with valid nucleotideMutations`(
        description: String,
        request: MockHttpServletRequestBuilder,
    ) {
        every {
            siloQueryModelMock.getAggregated(
                SequenceFiltersRequestWithFields(
                    emptyMap(),
                    listOf(NucleotideMutation(null, 123, "A"), NucleotideMutation(null, 124, "B")),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                ),
            )
        } returns Stream.of(AggregationData(5, emptyMap()))

        mockMvc.perform(getSample("$AGGREGATED_ROUTE?nucleotideMutations=123A,124B"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(5))
    }

    @ParameterizedTest(name = "{0} aggregated with valid aminoAcidMutations")
    @MethodSource("getAggregatedRequestsWithAminoAcidMutations")
    fun `aggregated with valid aminoAcidMutations`(
        description: String,
        request: MockHttpServletRequestBuilder,
    ) {
        every {
            siloQueryModelMock.getAggregated(
                SequenceFiltersRequestWithFields(
                    emptyMap(),
                    emptyList(),
                    listOf(AminoAcidMutation("gene1", 123, "A"), AminoAcidMutation("gene2", 124, "B")),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                ),
            )
        } returns Stream.of(AggregationData(5, emptyMap()))

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(5))
    }

    @ParameterizedTest(name = "GET {0} with invalid nucleotide mutation")
    @MethodSource("getEndpointsWithNucleotideMutationFilter")
    fun `GET endpoint with invalid nucleotide mutation filter`(endpoint: String) {
        val path = if (endpoint == MOST_RECENT_COMMON_ANCESTOR_ROUTE) {
            "$endpoint?phyloTreeField=primaryKey&nucleotideMutations=invalidMutation"
        } else {
            "$endpoint?nucleotideMutations=invalidMutation"
        }
        mockMvc.perform(getSample(path))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.detail").value(Matchers.containsString("Failed to convert 'nucleotideMutations'")))
    }

    @ParameterizedTest(name = "GET {0} with invalid nucleotide mutation")
    @MethodSource("getEndpointsWithAminoAcidMutationFilter")
    fun `GET endpoint with invalid amino acid mutation`(endpoint: String) {
        val path = if (endpoint == MOST_RECENT_COMMON_ANCESTOR_ROUTE) {
            "$endpoint?phyloTreeField=primaryKey&aminoAcidMutations=invalidMutation"
        } else {
            "$endpoint?aminoAcidMutations=invalidMutation"
        }
        mockMvc.perform(getSample(path))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.detail").value(Matchers.containsString("Failed to convert 'aminoAcidMutations'")))
    }

    @ParameterizedTest(name = "GET {0} with invalid nucleotideInsertion")
    @MethodSource("getEndpointsWithInsertionFilter")
    fun `GET with invalid nucleotide insertion filter`(endpoint: String) {
        val path = if (endpoint == MOST_RECENT_COMMON_ANCESTOR_ROUTE) {
            "$endpoint?phyloTreeField=primaryKey&nucleotideInsertions=invalidInsertion"
        } else {
            "$endpoint?nucleotideInsertions=invalidInsertion"
        }
        mockMvc.perform(getSample(path))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.detail").value(Matchers.containsString("Failed to convert 'nucleotideInsertions'")))
    }

    @ParameterizedTest(name = "GET {0} with invalid aminoAcidInsertion")
    @MethodSource("getEndpointsWithInsertionFilter")
    fun `GET with invalid amino acid insertionFilter`(endpoint: String) {
        val path = if (endpoint == MOST_RECENT_COMMON_ANCESTOR_ROUTE) {
            "$endpoint?phyloTreeField=primaryKey&aminoAcidInsertions=invalidInsertion"
        } else {
            "$endpoint?aminoAcidInsertions=invalidInsertion"
        }
        mockMvc.perform(getSample(path))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.detail").value(Matchers.containsString("Failed to convert 'aminoAcidInsertions'")))
    }

    @ParameterizedTest(name = "GET {0} with non existing field should throw")
    @MethodSource("getEndpointsWithFields")
    fun `GET with non existing field should throw`(endpoint: String) {
        val path = if (endpoint == MOST_RECENT_COMMON_ANCESTOR_ROUTE) {
            "$endpoint?phyloTreeField=primaryKey&fields=nonExistingField"
        } else {
            "$endpoint?fields=nonExistingField"
        }
        mockMvc.perform(getSample(path))
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath(
                    "\$.error.detail",
                ).value(Matchers.containsString("Unknown field: 'nonExistingField', known values are [primaryKey,")),
            )
    }

    private companion object {
        val endpointsOfController = SampleRoute.entries
            .map { it.pathSegment }
            .map {
                Arguments.of(
                    when (it) {
                        ALIGNED_AMINO_ACID_SEQUENCES_ROUTE -> "$it/gene1"
                        ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE -> "$it/other_segment"
                        UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE -> "$it/other_segment"
                        else -> it
                    },
                )
            }

        @JvmStatic
        fun getEndpointsWithInsertionFilter() = endpointsOfController

        @JvmStatic
        fun getEndpointsWithNucleotideMutationFilter() = endpointsOfController

        @JvmStatic
        fun getEndpointsWithAminoAcidMutationFilter() = endpointsOfController

        @JvmStatic
        fun getEndpointsWithFields() = listOf(AGGREGATED, DETAILS).map { it.pathSegment }.map { Arguments.of(it) }

        @JvmStatic
        val aggregatedPostRequestsWithFlatOrderByFields = listOf(
            Arguments.of(
                "JSON",
                postSample(AGGREGATED_ROUTE)
                    .content(
                        """
                            {
                                "orderBy": [
                                    "country",
                                    "${FIELD_WITH_ONLY_LOWERCASE_LETTERS.uppercase()}",
                                    "${FIELD_WITH_UPPERCASE_LETTER.lowercase()}"
                                ]
                            }
                        """,
                    )
                    .contentType(APPLICATION_JSON),
            ),
            Arguments.of(
                "form encoded",
                postSample(AGGREGATED_ROUTE)
                    .param(
                        "orderBy",
                        "country",
                        FIELD_WITH_ONLY_LOWERCASE_LETTERS.uppercase(),
                        FIELD_WITH_UPPERCASE_LETTER.lowercase(),
                    )
                    .contentType(APPLICATION_FORM_URLENCODED),
            ),
        )

        @JvmStatic
        val aggregatedPostRequestsWithLimit = listOf(
            Arguments.of(
                "JSON",
                { limit: Any ->
                    val limitString = when (limit) {
                        is Number -> limit.toString()
                        else -> """ "$limit" """
                    }
                    postSample(AGGREGATED_ROUTE)
                        .content("""{"limit": $limitString}""")
                        .contentType(APPLICATION_JSON)
                },
            ),
            Arguments.of(
                "form encoded",
                { limit: Any ->
                    postSample(AGGREGATED_ROUTE)
                        .param("limit", limit.toString())
                        .contentType(APPLICATION_FORM_URLENCODED)
                },
            ),
        )

        @JvmStatic
        val aggregatedPostRequestsWithOffset = listOf(
            Arguments.of(
                "JSON",
                { offset: Any ->
                    val offsetString = when (offset) {
                        is Number -> offset.toString()
                        else -> """ "$offset" """
                    }
                    postSample(AGGREGATED_ROUTE)
                        .content("""{"offset": $offsetString}""")
                        .contentType(APPLICATION_JSON)
                },
            ),
            Arguments.of(
                "form encoded",
                { offset: Any ->
                    postSample(AGGREGATED_ROUTE)
                        .param("offset", offset.toString())
                        .contentType(APPLICATION_FORM_URLENCODED)
                },
            ),
        )

        @JvmStatic
        val aggregatedRequestsWithNucleotideInsertions = listOf(
            Arguments.of(
                "GET",
                getSample(AGGREGATED_ROUTE)
                    .queryParam("nucleotideInsertions", "ins_123:ABC", "ins_other_segment:124:DEF"),
            ),
            Arguments.of(
                "POST JSON",
                postSample(AGGREGATED_ROUTE)
                    .content(
                        """
                        {
                            "nucleotideInsertions": [
                                "ins_123:ABC",
                                "ins_other_segment:124:DEF"
                            ]
                        }
                        """.trimIndent(),
                    )
                    .contentType(APPLICATION_JSON),
            ),
            Arguments.of(
                "POST form encoded",
                postSample(AGGREGATED_ROUTE)
                    .param("nucleotideInsertions", "ins_123:ABC", "ins_other_segment:124:DEF")
                    .contentType(APPLICATION_FORM_URLENCODED),
            ),
        )

        @JvmStatic
        val aggregatedRequestsWithAminoAcidInsertions = listOf(
            Arguments.of(
                "GET",
                getSample(AGGREGATED_ROUTE)
                    .queryParam("aminoAcidInsertions", "ins_gene1:123:ABC", "ins_gene2:124:DEF"),
            ),
            Arguments.of(
                "POST JSON",
                postSample(AGGREGATED_ROUTE)
                    .content(
                        """
                        {
                            "aminoAcidInsertions": [
                                "ins_gene1:123:ABC",
                                "ins_gene2:124:DEF"
                            ]
                        }
                        """.trimIndent(),
                    )
                    .contentType(APPLICATION_JSON),
            ),
            Arguments.of(
                "POST form encoded",
                postSample(AGGREGATED_ROUTE)
                    .param("aminoAcidInsertions", "ins_gene1:123:ABC", "ins_gene2:124:DEF")
                    .contentType(APPLICATION_FORM_URLENCODED),
            ),
        )

        @JvmStatic
        val aggregatedRequestsWithNucleotideMutations = listOf(
            Arguments.of(
                "GET",
                getSample(AGGREGATED_ROUTE)
                    .queryParam("nucleotideMutations", "123A", "124B"),
            ),
            Arguments.of(
                "POST JSON",
                postSample(AGGREGATED_ROUTE)
                    .content(
                        """
                        {
                            "nucleotideMutations": [
                                "123A",
                                "124B"
                            ]
                        }
                        """.trimIndent(),
                    )
                    .contentType(APPLICATION_JSON),
            ),
            Arguments.of(
                "POST form encoded",
                postSample(AGGREGATED_ROUTE)
                    .param("nucleotideMutations", "123A", "124B")
                    .contentType(APPLICATION_FORM_URLENCODED),
            ),
        )

        @JvmStatic
        val aggregatedRequestsWithAminoAcidMutations = listOf(
            Arguments.of(
                "GET",
                getSample(AGGREGATED_ROUTE)
                    .queryParam("aminoAcidMutations", "gene1:123A", "gene2:124B"),
            ),
            Arguments.of(
                "POST JSON",
                postSample(AGGREGATED_ROUTE)
                    .content(
                        """
                        {
                            "aminoAcidMutations": [
                                "gene1:123A",
                                "gene2:124B"
                            ]
                        }
                        """.trimIndent(),
                    )
                    .contentType(APPLICATION_JSON),
            ),
            Arguments.of(
                "POST form encoded",
                postSample(AGGREGATED_ROUTE)
                    .param("aminoAcidMutations", "gene1:123A", "gene2:124B")
                    .contentType(APPLICATION_FORM_URLENCODED),
            ),
        )
    }
}
