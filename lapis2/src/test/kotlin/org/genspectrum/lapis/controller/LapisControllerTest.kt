package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.TextNode
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.DataVersion
import org.genspectrum.lapis.request.MutationProportionsRequest
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.SequenceFiltersRequest
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.AminoAcidInsertionResponse
import org.genspectrum.lapis.response.AminoAcidMutationResponse
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.NucleotideInsertionResponse
import org.genspectrum.lapis.response.NucleotideMutationResponse
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

        mockMvc.perform(get("$AGGREGATED_ROUTE?country=Switzerland"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$.data[0].age").value(42))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
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
        val request = post("/aggregated")
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
    }

    @Test
    fun `GET aggregated with fields`() {
        every {
            siloQueryModelMock.getAggregated(
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

        mockMvc.perform(get("$AGGREGATED_ROUTE?country=Switzerland&fields=country,age"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$.data[0].age").value(42))
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

        mockMvc.perform(get("$AGGREGATED_ROUTE?nucleotideMutations=123A,124B"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(5))
    }

    @Test
    fun `POST aggregated with fields`() {
        every {
            siloQueryModelMock.getAggregated(
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

        val request = post(AGGREGATED_ROUTE)
            .content("""{"country": "Switzerland", "fields": ["country","age"]}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$.data[0].age").value(42))
    }

    @ParameterizedTest(name = "GET {0} without explicit minProportion")
    @MethodSource("getMutationEndpoints")
    fun `GET mutations without explicit minProportion`(endpoint: String) {
        setupMutationMock(endpoint, null)

        mockMvc.perform(get("$endpoint?country=Switzerland"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].mutation").value("the mutation"))
            .andExpect(jsonPath("\$.data[0].proportion").value(0.5))
            .andExpect(jsonPath("\$.data[0].count").value(42))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
    }

    @ParameterizedTest(name = "GET {0} with minProportion")
    @MethodSource("getMutationEndpoints")
    fun `GET mutations with minProportion`(endpoint: String) {
        setupMutationMock(endpoint, 0.3)

        mockMvc.perform(get("$endpoint?country=Switzerland&minProportion=0.3"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].mutation").value("the mutation"))
            .andExpect(jsonPath("\$.data[0].proportion").value(0.5))
            .andExpect(jsonPath("\$.data[0].count").value(42))
    }

    @ParameterizedTest(name = "POST {0} without explicit minProportion")
    @MethodSource("getMutationEndpoints")
    fun `POST mutations without explicit minProportion`(endpoint: String) {
        setupMutationMock(endpoint, null)

        val request = post(endpoint)
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

        val request = post(endpoint)
            .content("""{"country": "Switzerland", "minProportion": 0.7}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].mutation").value("the mutation"))
            .andExpect(jsonPath("\$.data[0].proportion").value(0.5))
            .andExpect(jsonPath("\$.data[0].count").value(42))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
    }

    @ParameterizedTest(name = "POST {0} with invalid minProportion returns bad request")
    @MethodSource("getMutationEndpoints")
    fun `POST mutations with invalid minProportion returns bad request`(endpoint: String) {
        val request = post(endpoint)
            .content("""{"country": "Switzerland", "minProportion": "this is not a float"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.error.title").value("Bad Request"))
            .andExpect(
                jsonPath("\$.error.detail").value("minProportion must be a number"),
            )
    }

    private fun setupMutationMock(
        endpoint: String,
        minProportion: Double?,
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

        val request = post(endpoint)
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].insertion").value("the insertion"))
            .andExpect(jsonPath("\$.data[0].count").value(42))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
    }

    @ParameterizedTest(name = "GET {0}")
    @MethodSource("getInsertionEndpoints")
    fun `GET insertions`(endpoint: String) {
        setupInsertionMock(endpoint)

        val request = get("$endpoint?country=Switzerland")

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].insertion").value("the insertion"))
            .andExpect(jsonPath("\$.data[0].count").value(42))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
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
    }

    @Test
    fun `GET details`() {
        every {
            siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns listOf(DetailsData(mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42))))

        mockMvc.perform(get("$DETAILS_ROUTE?country=Switzerland"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$.data[0].age").value(42))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
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
        } returns listOf(DetailsData(mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42))))

        mockMvc.perform(get("$DETAILS_ROUTE?country=Switzerland&fields=country&fields=age"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$.data[0].age").value(42))
    }

    @Test
    fun `POST details`() {
        every {
            siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns listOf(DetailsData(mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42))))

        val request = post(DETAILS_ROUTE)
            .content("""{"country": "Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$.data[0].age").value(42))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
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
        } returns listOf(DetailsData(mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42))))

        val request = post(DETAILS_ROUTE)
            .content("""{"country": "Switzerland", "fields": ["country", "age"]}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$.data[0].age").value(42))
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

    private fun sequenceFiltersRequest(sequenceFilters: Map<String, String>) =
        SequenceFiltersRequest(
            sequenceFilters,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
        )

    private fun mutationProportionsRequest(
        sequenceFilters: Map<String, String>,
        minProportion: Double?,
    ) = MutationProportionsRequest(
        sequenceFilters,
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        minProportion,
        emptyList(),
    )

    private fun someNucleotideMutationProportion() = NucleotideMutationResponse("the mutation", 42, 0.5)

    private fun someAminoAcidMutationProportion() = AminoAcidMutationResponse("the mutation", 42, 0.5)

    private fun someNucleotideInsertion() = NucleotideInsertionResponse("the insertion", 42)

    private fun someAminoAcidInsertion() = AminoAcidInsertionResponse("the insertion", 42)
}
