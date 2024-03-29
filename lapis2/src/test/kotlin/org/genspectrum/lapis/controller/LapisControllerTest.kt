package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.DEFAULT_MIN_PROPORTION
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.AminoAcidInsertionResponse
import org.genspectrum.lapis.response.AminoAcidMutationResponse
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.NucleotideInsertionResponse
import org.genspectrum.lapis.response.NucleotideMutationResponse
import org.genspectrum.lapis.silo.DataVersion
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.hasSize
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class LapisControllerTest(
    @Autowired val mockMvc: MockMvc,
) {
    @MockkBean
    lateinit var siloQueryModelMock: SiloQueryModel

    @MockkBean
    lateinit var dataVersion: DataVersion

    @BeforeEach
    fun setup() {
        every {
            dataVersion.dataVersion
        } returns "1234"
    }

    @Test
    fun `GET aggregated`() {
        every {
            siloQueryModelMock.getAggregated(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns listOf(
            AggregationData(
                0,
                mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42)),
            ),
        )

        mockMvc.perform(getSample("$AGGREGATED_ROUTE?country=Switzerland"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$.data[0].age").value(42))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
            .andExpect(jsonPath("\$.info.dataVersion").value(1234))
    }

    @Test
    fun `POST aggregated`() {
        every {
            siloQueryModelMock.getAggregated(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns listOf(
            AggregationData(
                0,
                emptyMap(),
            ),
        )
        val request = postSample("/aggregated")
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
            .andExpect(jsonPath("\$.info.dataVersion").value(1234))
    }

    @Test
    fun `GET aggregated with fields`() {
        every {
            siloQueryModelMock.getAggregated(
                sequenceFiltersRequestWithFields(
                    mapOf("country" to "Switzerland"),
                    listOf("country", "date"),
                ),
            )
        } returns listOf(
            AggregationData(
                0,
                mapOf("country" to TextNode("Switzerland"), "date" to TextNode("a date")),
            ),
        )

        mockMvc.perform(getSample("$AGGREGATED_ROUTE?country=Switzerland&fields=country,date"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$.data[0].date").value("a date"))
    }

    @ParameterizedTest(name = "{0} aggregated with multiple values for filter field")
    @MethodSource("getRequestsWithMultipleValuesForField")
    fun `aggregated with multiple values for filter field`(
        testName: String,
        request: MockHttpServletRequestBuilder,
    ) {
        every {
            siloQueryModelMock.getAggregated(
                sequenceFiltersRequestWithArrayValuedFilters(
                    mapOf("country" to listOf("Switzerland", "Germany")),
                ),
            )
        } returns listOf(
            AggregationData(
                0,
                mapOf("country" to TextNode("Switzerland")),
            ),
        )

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
    }

    @Test
    fun `GET aggregated with valid mutation`() {
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
        } returns listOf(AggregationData(5, emptyMap()))

        mockMvc.perform(getSample("$AGGREGATED_ROUTE?nucleotideMutations=123A,124B"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(5))
    }

    @Test
    fun `POST aggregated with fields`() {
        every {
            siloQueryModelMock.getAggregated(
                sequenceFiltersRequestWithFields(
                    mapOf("country" to "Switzerland"),
                    listOf("country", "date"),
                ),
            )
        } returns listOf(
            AggregationData(
                0,
                mapOf("country" to TextNode("Switzerland"), "date" to TextNode("a date")),
            ),
        )

        val request = postSample(AGGREGATED_ROUTE)
            .content("""{"country": "Switzerland", "fields": ["country","date"]}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$.data[0].date").value("a date"))
    }

    @ParameterizedTest(name = "GET {0} without explicit minProportion")
    @MethodSource("getMutationEndpoints")
    fun `GET mutations without explicit minProportion`(endpoint: String) {
        setupMutationMock(endpoint, DEFAULT_MIN_PROPORTION)

        mockMvc.perform(getSample("$endpoint?country=Switzerland"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].mutation").value("the mutation"))
            .andExpect(jsonPath("\$.data[0].proportion").value(0.5))
            .andExpect(jsonPath("\$.data[0].count").value(42))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
            .andExpect(jsonPath("\$.info.dataVersion").value(1234))
    }

    @ParameterizedTest(name = "GET {0} with minProportion")
    @MethodSource("getMutationEndpoints")
    fun `GET mutations with minProportion`(endpoint: String) {
        setupMutationMock(endpoint, 0.3)

        mockMvc.perform(getSample("$endpoint?country=Switzerland&minProportion=0.3"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].mutation").value("the mutation"))
            .andExpect(jsonPath("\$.data[0].proportion").value(0.5))
            .andExpect(jsonPath("\$.data[0].count").value(42))
    }

    @ParameterizedTest(name = "POST {0} without explicit minProportion")
    @MethodSource("getMutationEndpoints")
    fun `POST mutations without explicit minProportion`(endpoint: String) {
        setupMutationMock(endpoint, DEFAULT_MIN_PROPORTION)

        val request = postSample(endpoint)
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].mutation").value("the mutation"))
            .andExpect(jsonPath("\$.data[0].proportion").value(0.5))
            .andExpect(jsonPath("\$.data[0].count").value(42))
    }

    @ParameterizedTest(name = "POST {0} with minProportion")
    @MethodSource("getMutationEndpoints")
    fun `POST mutations with minProportion`(endpoint: String) {
        setupMutationMock(endpoint, 0.7)

        val request = postSample(endpoint)
            .content("""{"country": "Switzerland", "minProportion": 0.7}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].mutation").value("the mutation"))
            .andExpect(jsonPath("\$.data[0].proportion").value(0.5))
            .andExpect(jsonPath("\$.data[0].count").value(42))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
            .andExpect(jsonPath("\$.info.dataVersion").value(1234))
    }

    @ParameterizedTest(name = "POST {0} with invalid minProportion returns bad request")
    @MethodSource("getMutationEndpoints")
    fun `POST mutations with invalid minProportion returns bad request`(endpoint: String) {
        val request = postSample(endpoint)
            .content("""{"country": "Switzerland", "minProportion": "this is not a float"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.error.title").value("Bad Request"))
            .andExpect(
                jsonPath("\$.error.detail").value("minProportion must be a number"),
            )
    }

    @ParameterizedTest(name = "GET {0} only returns mutation, proportion and count")
    @MethodSource("getMutationEndpoints")
    fun `GET mutations only returns mutation, proportion and count`(endpoint: String) {
        setupMutationMock(endpoint, DEFAULT_MIN_PROPORTION)

        val mvcResult = mockMvc.perform(getSample("$endpoint?country=Switzerland"))
            .andExpect(status().isOk)
            .andReturn()

        val response = ObjectMapper().readValue<Map<*, *>>(mvcResult.response.contentAsString)
        val data = response["data"] as List<*>
        val firstDataObject = data[0] as Map<*, *>

        assertThat(firstDataObject.keys, hasSize(3))
        assertThat(firstDataObject.keys, containsInAnyOrder("mutation", "proportion", "count"))
    }

    private fun setupMutationMock(
        endpoint: String,
        minProportion: Double = DEFAULT_MIN_PROPORTION,
    ) {
        if (endpoint == "/nucleotideMutations") {
            every {
                siloQueryModelMock.computeNucleotideMutationProportions(
                    mutationProportionsRequest(
                        mapOf("country" to "Switzerland"),
                        minProportion,
                    ),
                )
            } returns listOf(someNucleotideMutationProportion())
        }
        if (endpoint == "/aminoAcidMutations") {
            every {
                siloQueryModelMock.computeAminoAcidMutationProportions(
                    mutationProportionsRequest(
                        mapOf("country" to "Switzerland"),
                        minProportion,
                    ),
                )
            } returns listOf(someAminoAcidMutationProportion())
        }
    }

    @ParameterizedTest(name = "POST {0}")
    @MethodSource("getInsertionEndpoints")
    fun `POST insertions`(endpoint: String) {
        setupInsertionMock(endpoint)

        val request = postSample(endpoint)
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].insertion").value("the insertion"))
            .andExpect(jsonPath("\$.data[0].count").value(42))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
            .andExpect(jsonPath("\$.info.dataVersion").value(1234))
    }

    @ParameterizedTest(name = "GET {0}")
    @MethodSource("getInsertionEndpoints")
    fun `GET insertions`(endpoint: String) {
        setupInsertionMock(endpoint)

        val request = getSample("$endpoint?country=Switzerland")

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].insertion").value("the insertion"))
            .andExpect(jsonPath("\$.data[0].count").value(42))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
            .andExpect(jsonPath("\$.info.dataVersion").value(1234))
    }

    @ParameterizedTest(name = "GET {0} only returns mutation, proportion and count")
    @MethodSource("getInsertionEndpoints")
    fun `GET insertions only returns insertion and count`(endpoint: String) {
        setupInsertionMock(endpoint)

        val mvcResult = mockMvc.perform(getSample("$endpoint?country=Switzerland"))
            .andExpect(status().isOk)
            .andReturn()

        val response = ObjectMapper().readValue(mvcResult.response.contentAsString, Map::class.java)
        val data = response["data"] as List<*>
        val firstDataObject = data[0] as Map<*, *>

        assertThat(firstDataObject.keys, hasSize(2))
        assertThat(firstDataObject.keys, containsInAnyOrder("insertion", "count"))
    }

    private fun setupInsertionMock(endpoint: String) {
        when (endpoint) {
            NUCLEOTIDE_INSERTIONS_ROUTE -> {
                every {
                    siloQueryModelMock.getNucleotideInsertions(
                        sequenceFiltersRequest(mapOf("country" to "Switzerland")),
                    )
                } returns listOf(someNucleotideInsertion())
            }

            AMINO_ACID_INSERTIONS_ROUTE -> {
                every {
                    siloQueryModelMock.getAminoAcidInsertions(sequenceFiltersRequest(mapOf("country" to "Switzerland")))
                } returns listOf(someAminoAcidInsertion())
            }

            else -> throw IllegalArgumentException("Unknown endpoint: $endpoint")
        }
    }

    private companion object {
        @JvmStatic
        fun getMutationEndpoints() =
            listOf(
                Arguments.of(NUCLEOTIDE_MUTATIONS_ROUTE),
                Arguments.of(AMINO_ACID_MUTATIONS_ROUTE),
            )

        @JvmStatic
        fun getInsertionEndpoints() =
            listOf(
                Arguments.of(NUCLEOTIDE_INSERTIONS_ROUTE),
                Arguments.of(AMINO_ACID_INSERTIONS_ROUTE),
            )

        @JvmStatic
        val requestsWithMultipleValuesForField = listOf(
            Arguments.of(
                "GET",
                getSample("$AGGREGATED_ROUTE?country=Switzerland&country=Germany"),
            ),
            Arguments.of(
                "POST",
                postSample(AGGREGATED_ROUTE)
                    .content("""{"country": ["Switzerland", "Germany"]}""")
                    .contentType(MediaType.APPLICATION_JSON),
            ),
        )
    }

    @Test
    fun `GET details`() {
        every {
            siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns listOf(DetailsData(mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42))))

        mockMvc.perform(getSample("$DETAILS_ROUTE?country=Switzerland"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$.data[0].age").value(42))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
            .andExpect(jsonPath("\$.info.dataVersion").value(1234))
    }

    @Test
    fun `GET details with fields`() {
        every {
            siloQueryModelMock.getDetails(
                sequenceFiltersRequestWithFields(
                    mapOf("country" to "Switzerland"),
                    listOf("country", "date"),
                ),
            )
        } returns listOf(DetailsData(mapOf("country" to TextNode("Switzerland"), "date" to TextNode("a date"))))

        mockMvc.perform(getSample("$DETAILS_ROUTE?country=Switzerland&fields=country&fields=date"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$.data[0].date").value("a date"))
    }

    @Test
    fun `POST details`() {
        every {
            siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns listOf(DetailsData(mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42))))

        val request = postSample(DETAILS_ROUTE)
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$.data[0].age").value(42))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
            .andExpect(jsonPath("\$.info.dataVersion").value(1234))
    }

    @Test
    fun `POST details with fields`() {
        every {
            siloQueryModelMock.getDetails(
                sequenceFiltersRequestWithFields(
                    mapOf("country" to "Switzerland"),
                    listOf("country", "date"),
                ),
            )
        } returns listOf(DetailsData(mapOf("country" to TextNode("Switzerland"), "date" to TextNode("a date"))))

        val request = postSample(DETAILS_ROUTE)
            .content("""{"country": "Switzerland", "fields": ["country", "date"]}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$.data[0].date").value("a date"))
    }

    private fun someNucleotideMutationProportion() = NucleotideMutationResponse("the mutation", 42, 0.5)

    private fun someAminoAcidMutationProportion() = AminoAcidMutationResponse("the mutation", 42, 0.5)

    private fun someNucleotideInsertion() = NucleotideInsertionResponse("the insertion", 42)

    private fun someAminoAcidInsertion() = AminoAcidInsertionResponse("the insertion", 42)
}

fun getSample(path: String): MockHttpServletRequestBuilder = get("/sample/$path")

fun postSample(path: String): MockHttpServletRequestBuilder = post("/sample/$path")
