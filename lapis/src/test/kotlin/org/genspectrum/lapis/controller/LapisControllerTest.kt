package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.TextNode
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.controller.SequenceEndpointTestScenario.Mode.AllSequences
import org.genspectrum.lapis.controller.SequenceEndpointTestScenario.Mode.SingleSequence
import org.genspectrum.lapis.model.FastaHeaderTemplate
import org.genspectrum.lapis.model.SequencesResponse
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.DEFAULT_MIN_PROPORTION
import org.genspectrum.lapis.request.GENES_PROPERTY
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.ExplicitlyNullable
import org.genspectrum.lapis.response.InsertionResponse
import org.genspectrum.lapis.response.MutationResponse
import org.genspectrum.lapis.silo.DataVersion
import org.genspectrum.lapis.silo.SequenceType
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.stream.Stream

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

    @ParameterizedTest(name = "{0} aggregated")
    @MethodSource("getRequests")
    fun aggregated(
        testName: String,
        request: (String) -> MockHttpServletRequestBuilder,
    ) {
        every {
            siloQueryModelMock.getAggregated(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns Stream.of(
            AggregationData(
                0,
                mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42)),
            ),
        )

        mockMvc.perform(request(AGGREGATED_ROUTE))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$.data[0].age").value(42))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
            .andExpect(jsonPath("\$.info.dataVersion").value(1234))
    }

    @ParameterizedTest(name = "{0} aggregated with fields")
    @MethodSource("getRequestsWithFields")
    fun `aggregated with fields`(
        testName: String,
        request: (String) -> MockHttpServletRequestBuilder,
    ) {
        every {
            siloQueryModelMock.getAggregated(
                sequenceFiltersRequestWithFields(
                    sequenceFilters = mapOf("country" to "Switzerland"),
                    fields = listOf("country", "date"),
                ),
            )
        } returns Stream.of(
            AggregationData(
                0,
                mapOf("country" to TextNode("Switzerland"), "date" to TextNode("a date")),
            ),
        )

        mockMvc.perform(request(AGGREGATED_ROUTE))
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
        } returns Stream.of(
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

    @ParameterizedTest(name = "{0} without explicit minProportion")
    @MethodSource("getMutationRequests")
    fun `mutations without explicit minProportion`(
        testName: String,
        requestWithMinProportion: (Double?) -> MockHttpServletRequestBuilder,
        setupMutationMock: (siloQueryModelMock: SiloQueryModel, minProportion: Double) -> Unit,
    ) {
        setupMutationMock(siloQueryModelMock, DEFAULT_MIN_PROPORTION)

        mockMvc.perform(requestWithMinProportion(null))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].mutation").value("the mutation"))
            .andExpect(jsonPath("\$.data[0].proportion").value(0.5))
            .andExpect(jsonPath("\$.data[0].count").value(42))
            .andExpect(jsonPath("\$.data[0].coverage").value(52))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
            .andExpect(jsonPath("\$.info.dataVersion").value(1234))
    }

    @ParameterizedTest(name = "{0} with minProportion")
    @MethodSource("getMutationRequests")
    fun `mutations with minProportion`(
        testName: String,
        requestWithMinProportion: (Double?) -> MockHttpServletRequestBuilder,
        setupMutationMock: (siloQueryModelMock: SiloQueryModel, minProportion: Double) -> Unit,
    ) {
        setupMutationMock(siloQueryModelMock, 0.3)

        mockMvc.perform(requestWithMinProportion(0.3))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].mutation").value("the mutation"))
            .andExpect(jsonPath("\$.data[0].proportion").value(0.5))
            .andExpect(jsonPath("\$.data[0].count").value(42))
    }

    @ParameterizedTest(name = "{0} with invalid minProportion returns bad request")
    @MethodSource("getMutationRequests")
    fun `mutations with invalid minProportion returns bad request`(
        testName: String,
        requestWithMinProportion: (Any?) -> MockHttpServletRequestBuilder,
        setupMutationMock: (siloQueryModelMock: SiloQueryModel, minProportion: Double) -> Unit,
    ) {
        mockMvc.perform(requestWithMinProportion("this is not a float"))
            .andExpect(status().isBadRequest)
            .andExpect(
                if (testName.contains("GET")) {
                    jsonPath("\$.detail").value(containsString("Failed to convert 'minProportion'"))
                } else {
                    jsonPath("\$.error.detail").value(containsString("minProportion must be a number"))
                },
            )
    }

    @ParameterizedTest(name = "{0} only returns mutation, proportion and count")
    @MethodSource("getMutationRequests")
    fun `GET mutations only returns expected fields`(
        testName: String,
        requestWithMinProportion: (Any?) -> MockHttpServletRequestBuilder,
        setupMutationMock: (siloQueryModelMock: SiloQueryModel, minProportion: Double) -> Unit,
    ) {
        setupMutationMock(siloQueryModelMock, DEFAULT_MIN_PROPORTION)

        mockMvc.perform(requestWithMinProportion(null))
            .andExpect(status().isOk)
            .andExpect(
                jsonPath(
                    "\$.data[0].keys()",
                    containsInAnyOrder(
                        "mutation",
                        "proportion",
                        "coverage",
                        "count",
                        "sequenceName",
                        "mutationFrom",
                        "mutationTo",
                        "position",
                    ),
                ),
            )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getInsertionRequests")
    fun insertions(
        testName: String,
        request: MockHttpServletRequestBuilder,
        setupInsertionMock: (siloQueryModelMock: SiloQueryModel) -> Unit,
    ) {
        setupInsertionMock(siloQueryModelMock)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].insertion").value("the insertion"))
            .andExpect(jsonPath("\$.data[0].count").value(42))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
            .andExpect(jsonPath("\$.info.dataVersion").value(1234))
    }

    @ParameterizedTest(name = "{0} only returns expected fields")
    @MethodSource("getInsertionRequests")
    fun `insertions only returns expected fields`(
        testName: String,
        request: MockHttpServletRequestBuilder,
        setupInsertionMock: (siloQueryModelMock: SiloQueryModel) -> Unit,
    ) {
        setupInsertionMock(siloQueryModelMock)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(
                jsonPath(
                    "\$.data[0].keys()",
                    containsInAnyOrder("insertion", "count", "position", "sequenceName", "insertedSymbols"),
                ),
            )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getAlignedAminoAcidSequencesScenarios")
    fun alignedAminoAcidSequences(scenario: SequenceEndpointTestScenario) {
        every {
            siloQueryModelMock.getGenomicSequence(
                sequenceFilters = sequenceFiltersRequest(mapOf("country" to "Switzerland")),
                sequenceType = SequenceType.ALIGNED,
                sequenceNames = listOf("geneName"),
                rawFastaHeaderTemplate = "{primaryKey}",
            )
        } returns MockDataForEndpoints
            .sequenceEndpointMockData("geneName")
            .sequencesResponse
            .copy()

        val responseContent = mockMvc.perform(scenario.request)
            .andExpect(status().isOk)
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
            .andReturn()
            .response
            .contentAsString

        scenario.mockData.assertDataMatches(responseContent)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getAllAlignedAminoAcidSequencesScenarios")
    fun allAlignedAminoAcidSequences(scenario: SequenceEndpointTestScenario) {
        scenario.mockData.mockWithData(siloQueryModelMock)

        val responseContent = mockMvc.perform(scenario.request)
            .andExpect(status().isOk)
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
            .andReturn()
            .response
            .contentAsString

        scenario.mockData.assertDataMatches(responseContent)
    }

    @Test
    fun `WHEN getting all amino acid sequences with gene THEN calls model with correct arguments`() {
        every {
            siloQueryModelMock.getGenomicSequence(
                sequenceFilters = sequenceFiltersRequest(mapOf("country" to "Switzerland")),
                sequenceType = SequenceType.ALIGNED,
                sequenceNames = listOf("gene1"),
                rawFastaHeaderTemplate = "{primaryKey}|{.gene}",
            )
        } returns SequencesResponse(
            sequenceData = Stream.empty(),
            requestedSequenceNames = listOf("gene1"),
            fastaHeaderTemplate = FastaHeaderTemplate("", emptySet()),
        )

        mockMvc.perform(
            getSample(ALIGNED_AMINO_ACID_SEQUENCES_ROUTE)
                .param(GENES_PROPERTY, "gene1")
                .param("country", "Switzerland"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `WHEN posting all aligned sequences with segment THEN calls model with correct arguments`() {
        every {
            siloQueryModelMock.getGenomicSequence(
                sequenceFilters = sequenceFiltersRequestWithGenes(
                    sequenceFilters = mapOf("country" to "Switzerland"),
                    genes = listOf("gene1"),
                ),
                sequenceType = SequenceType.ALIGNED,
                sequenceNames = listOf("gene1"),
                rawFastaHeaderTemplate = "{primaryKey}|{.gene}",
            )
        } returns SequencesResponse(
            sequenceData = Stream.empty(),
            requestedSequenceNames = listOf("gene1"),
            fastaHeaderTemplate = FastaHeaderTemplate("", emptySet()),
        )

        mockMvc.perform(
            postSample(ALIGNED_AMINO_ACID_SEQUENCES_ROUTE)
                .contentType(APPLICATION_JSON)
                .content("""{"country": "Switzerland", "$GENES_PROPERTY": ["gene1"]}"""),
        )
            .andExpect(status().isOk)
    }

    private companion object {
        @JvmStatic
        val standardRequests = listOf(
            "GET" to
                { route: String ->
                    getSample(route)
                        .queryParam("country", "Switzerland")
                },
            "POST JSON" to
                { route: String ->
                    postSample(route)
                        .content("""{"country": "Switzerland"}""")
                        .contentType(MediaType.APPLICATION_JSON)
                },
            "POST form encoded" to
                { route: String ->
                    postSample(route)
                        .param("country", "Switzerland")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                },
        )

        @JvmStatic
        val mutationRequests =
            mutationRequestsForMinProportion(NUCLEOTIDE_MUTATIONS_ROUTE)
                .map { (description, request) ->
                    Arguments.of(
                        description,
                        request,
                        { siloQueryModelMock: SiloQueryModel, minProportion: Double ->
                            every {
                                siloQueryModelMock.computeNucleotideMutationProportions(
                                    mutationProportionsRequest(
                                        mapOf("country" to "Switzerland"),
                                        minProportion,
                                    ),
                                )
                            } returns Stream.of(someNucleotideMutationProportion())
                        },
                    )
                } +
                mutationRequestsForMinProportion(AMINO_ACID_MUTATIONS_ROUTE)
                    .map { (description, request) ->
                        Arguments.of(
                            description,
                            request,
                            { siloQueryModelMock: SiloQueryModel, minProportion: Double ->
                                every {
                                    siloQueryModelMock.computeAminoAcidMutationProportions(
                                        mutationProportionsRequest(
                                            mapOf("country" to "Switzerland"),
                                            minProportion,
                                        ),
                                    )
                                } returns Stream.of(someAminoAcidMutationProportion())
                            },
                        )
                    }

        @JvmStatic
        val insertionRequests =
            standardRequests.map { (description, request) ->
                Arguments.of(
                    "$description $NUCLEOTIDE_INSERTIONS_ROUTE",
                    request(NUCLEOTIDE_INSERTIONS_ROUTE),
                    { siloQueryModelMock: SiloQueryModel ->
                        every {
                            siloQueryModelMock.getNucleotideInsertions(
                                sequenceFiltersRequest(mapOf("country" to "Switzerland")),
                            )
                        } returns Stream.of(someNucleotideInsertion())
                    },
                )
            } +
                standardRequests.map { (description, request) ->
                    Arguments.of(
                        "$description $AMINO_ACID_INSERTIONS_ROUTE",
                        request(AMINO_ACID_INSERTIONS_ROUTE),
                        { siloQueryModelMock: SiloQueryModel ->
                            every {
                                siloQueryModelMock.getAminoAcidInsertions(
                                    sequenceFiltersRequest(mapOf("country" to "Switzerland")),
                                )
                            } returns Stream.of(someAminoAcidInsertion())
                        },
                    )
                }

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
            Arguments.of(
                "POST form encoded",
                postSample(AGGREGATED_ROUTE)
                    .param("country", "Switzerland", "Germany")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE),
            ),
        )

        @JvmStatic
        val requests = standardRequests.map { (description, request) -> Arguments.of(description, request) }

        @JvmStatic
        val requestsWithFields = listOf(
            Arguments.of(
                "GET",
                { route: String ->
                    getSample(route)
                        .queryParam("country", "Switzerland")
                        .queryParam("fields", "country", "date")
                },
            ),
            Arguments.of(
                "POST JSON",
                { route: String ->
                    postSample(route)
                        .content("""{"country": "Switzerland", "fields": ["country","date"]}}""")
                        .contentType(MediaType.APPLICATION_JSON)
                },
            ),
            Arguments.of(
                "POST form encoded",
                { route: String ->
                    postSample(route)
                        .param("country", "Switzerland")
                        .param("fields", "country", "date")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                },
            ),
        )

        @JvmStatic
        val alignedAminoAcidSequencesScenarios = SequenceEndpointTestScenario.createScenarios(
            route = "$ALIGNED_AMINO_ACID_SEQUENCES_ROUTE/geneName",
            mode = SingleSequence("geneName"),
        )

        @JvmStatic
        val allAlignedAminoAcidSequencesScenarios = SequenceEndpointTestScenario.createScenarios(
            route = ALIGNED_AMINO_ACID_SEQUENCES_ROUTE,
            mode = AllSequences,
        )
    }

    @ParameterizedTest(name = "{0} details")
    @MethodSource("getRequests")
    fun details(
        testName: String,
        request: (String) -> MockHttpServletRequestBuilder,
    ) {
        every {
            siloQueryModelMock.getDetails(sequenceFiltersRequestWithFields(mapOf("country" to "Switzerland")))
        } returns Stream.of(DetailsData(mapOf("country" to TextNode("Switzerland"), "age" to IntNode(42))))

        mockMvc.perform(request(DETAILS_ROUTE))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$.data[0].age").value(42))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
            .andExpect(jsonPath("\$.info.dataVersion").value(1234))
    }

    @ParameterizedTest(name = "{0} details with fields")
    @MethodSource("getRequestsWithFields")
    fun `details with fields`(
        testName: String,
        request: (String) -> MockHttpServletRequestBuilder,
    ) {
        every {
            siloQueryModelMock.getDetails(
                sequenceFiltersRequestWithFields(
                    sequenceFilters = mapOf("country" to "Switzerland"),
                    fields = listOf("country", "date"),
                ),
            )
        } returns Stream.of(DetailsData(mapOf("country" to TextNode("Switzerland"), "date" to TextNode("a date"))))

        mockMvc.perform(request(DETAILS_ROUTE))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].country").value("Switzerland"))
            .andExpect(jsonPath("\$.data[0].date").value("a date"))
    }
}

fun getSample(path: String): MockHttpServletRequestBuilder = get("/sample/$path")

fun postSample(path: String): MockHttpServletRequestBuilder = post("/sample/$path")

private fun mutationRequestsForMinProportion(
    endpoint: String,
): List<Pair<String, (Any?) -> MockHttpServletRequestBuilder>> =
    listOf(
        "GET $endpoint" to
            { minProportion ->
                getSample(endpoint)
                    .queryParam("country", "Switzerland")
                    .also {
                        if (minProportion != null) {
                            it.queryParam("minProportion", minProportion.toString())
                        }
                    }
            },
        "POST JSON $endpoint" to
            { minProportion ->
                val minProportionJsonPart = when (minProportion) {
                    null -> ""
                    is Number -> """, "minProportion": $minProportion"""
                    else -> """, "minProportion": "$minProportion" """
                }
                postSample(endpoint)
                    .content("""{"country": "Switzerland" $minProportionJsonPart}""")
                    .contentType(MediaType.APPLICATION_JSON)
            },
        "POST form encoded $endpoint" to
            { minProportion ->
                postSample(endpoint)
                    .param("country", "Switzerland")
                    .also {
                        if (minProportion != null) {
                            it.param("minProportion", minProportion.toString())
                        }
                    }
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            },
    )

private fun someNucleotideMutationProportion() =
    MutationResponse(
        mutation = "the mutation",
        count = 42,
        coverage = 52,
        proportion = 0.5,
        sequenceName = ExplicitlyNullable("sequenceName"),
        mutationFrom = "G",
        mutationTo = "T",
        position = 29741,
    )

private fun someAminoAcidMutationProportion() =
    MutationResponse(
        mutation = "the mutation",
        count = 42,
        coverage = 52,
        proportion = 0.5,
        sequenceName = ExplicitlyNullable("sequenceName"),
        mutationFrom = "G",
        mutationTo = "T",
        position = 29741,
    )

private fun someNucleotideInsertion() =
    InsertionResponse(
        insertion = "the insertion",
        count = 42,
        insertedSymbols = "CAGAAG",
        position = 22204,
        sequenceName = "sequenceName",
    )

private fun someAminoAcidInsertion() =
    InsertionResponse(
        insertion = "the insertion",
        count = 42,
        insertedSymbols = "CAGAAG",
        position = 22204,
        sequenceName = "sequenceName",
    )
