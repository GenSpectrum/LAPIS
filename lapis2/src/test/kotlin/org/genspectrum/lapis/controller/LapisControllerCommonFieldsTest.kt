package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.node.TextNode
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.FIELD_WITH_ONLY_LOWERCASE_LETTERS
import org.genspectrum.lapis.FIELD_WITH_UPPERCASE_LETTER
import org.genspectrum.lapis.controller.SampleRoute.AGGREGATED
import org.genspectrum.lapis.controller.SampleRoute.ALIGNED_AMINO_ACID_SEQUENCES
import org.genspectrum.lapis.controller.SampleRoute.ALIGNED_NUCLEOTIDE_SEQUENCES
import org.genspectrum.lapis.controller.SampleRoute.AMINO_ACID_INSERTIONS
import org.genspectrum.lapis.controller.SampleRoute.AMINO_ACID_MUTATIONS
import org.genspectrum.lapis.controller.SampleRoute.DETAILS
import org.genspectrum.lapis.controller.SampleRoute.NUCLEOTIDE_INSERTIONS
import org.genspectrum.lapis.controller.SampleRoute.NUCLEOTIDE_MUTATIONS
import org.genspectrum.lapis.controller.SampleRoute.UNALIGNED_NUCLEOTIDE_SEQUENCES
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.AminoAcidInsertion
import org.genspectrum.lapis.request.LapisInfo
import org.genspectrum.lapis.request.NucleotideInsertion
import org.genspectrum.lapis.request.Order
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.AminoAcidInsertionResponse
import org.genspectrum.lapis.response.AminoAcidMutationResponse
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.NucleotideInsertionResponse
import org.genspectrum.lapis.response.NucleotideMutationResponse
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
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
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
            .contentType(APPLICATION_JSON)

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
            .contentType(APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
    }

    @Test
    fun `POST aggregated with invalid limit`() {
        val request = postSample(AGGREGATED_ROUTE)
            .content("""{"limit": "this is not a number"}""")
            .contentType(APPLICATION_JSON)

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
            .contentType(APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(0))
    }

    @Test
    fun `POST aggregated with invalid offset`() {
        val request = postSample(AGGREGATED_ROUTE)
            .content("""{"offset": "this is not a number"}""")
            .contentType(APPLICATION_JSON)

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

    @ParameterizedTest(name = "GET data from {0} as file")
    @MethodSource("getDownloadAsFileScenarios")
    fun `GET data as file`(scenario: DownloadAsFileScenario) {
        scenario.setupMock(siloQueryModelMock)

        var queryString = "$DOWNLOAD_AS_FILE_PROPERTY=true"
        if (scenario.requestedDataFormat != null) {
            queryString += "&$FORMAT_PROPERTY=${scenario.requestedDataFormat}"
        }

        mockMvc.perform(getSample("${scenario.endpoint}?$queryString"))
            .andExpect(status().isOk)
            .andExpectAttachmentWithContent(
                expectedFilename = scenario.expectedFilename,
                expectedFileContent = scenario.expectedFileContent,
            )
    }

    @ParameterizedTest(name = "POST data from {0} as file")
    @MethodSource("getDownloadAsFileScenarios")
    fun `POST data as file`(scenario: DownloadAsFileScenario) {
        scenario.setupMock(siloQueryModelMock)

        val maybeDataFormat = when {
            scenario.requestedDataFormat != null -> """, "$FORMAT_PROPERTY": "${scenario.requestedDataFormat}" """
            else -> ""
        }
        val request = """{ "$DOWNLOAD_AS_FILE_PROPERTY": true $maybeDataFormat }"""

        mockMvc.perform(postSample(scenario.endpoint).content(request).contentType(APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpectAttachmentWithContent(
                expectedFilename = scenario.expectedFilename,
                expectedFileContent = scenario.expectedFileContent,
            )
    }

    fun ResultActions.andExpectAttachmentWithContent(
        expectedFilename: String,
        expectedFileContent: String,
    ): ResultActions =
        this.andExpect(header().string("Content-Disposition", "attachment; filename=$expectedFilename"))
            .andExpect(content().string(expectedFileContent))

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
        val downloadAsFileScenarios = SampleRoute.entries.flatMap { DownloadAsFileScenario.forEndpoint(it) }
    }
}

data class DownloadAsFileScenario(
    val endpoint: String,
    val requestedDataFormat: String?,
    val expectedFilename: String,
    val expectedFileContent: String,
    val setupMock: (SiloQueryModel) -> Any,
) {
    override fun toString() =
        when (requestedDataFormat) {
            null -> endpoint
            else -> "$endpoint as $requestedDataFormat"
        }

    companion object {
        fun forEndpoint(route: SampleRoute) =
            when (route) {
                AGGREGATED -> forDataFormats(
                    endpoint = AGGREGATED_ROUTE,
                    expectedFilename = "aggregated",
                    expectedJsonContent = """[{"count":1,"country":"Switzerland"}]""",
                    expectedCsvContent = "country,count\nSwitzerland,1",
                    expectedTsvContent = "country\tcount\nSwitzerland\t1",
                    setupMock = { every { it.getAggregated(any()) } returns aggregationData },
                )

                DETAILS -> forDataFormats(
                    endpoint = DETAILS_ROUTE,
                    expectedFilename = "details",
                    expectedJsonContent = """[{"country":"Switzerland","age":"42"}]""",
                    expectedCsvContent = "country,age\nSwitzerland,42",
                    expectedTsvContent = "country\tage\nSwitzerland\t42",
                    setupMock = { every { it.getDetails(any()) } returns detailsData },
                )

                NUCLEOTIDE_MUTATIONS -> forDataFormats(
                    endpoint = NUCLEOTIDE_MUTATIONS_ROUTE,
                    expectedFilename = "nucleotideMutations",
                    expectedJsonContent = """[{"mutation":"5G","count":1,"proportion":0.5}]""",
                    expectedCsvContent = "mutation,count,proportion\n5G,1,0.5",
                    expectedTsvContent = "mutation\tcount\tproportion\n5G\t1\t0.5",
                    setupMock =
                        { every { it.computeNucleotideMutationProportions(any()) } returns nucleotideMutationData },
                )

                AMINO_ACID_MUTATIONS -> forDataFormats(
                    endpoint = AMINO_ACID_MUTATIONS_ROUTE,
                    expectedFilename = "aminoAcidMutations",
                    expectedJsonContent = """[{"mutation":"5G","count":1,"proportion":0.5}]""",
                    expectedCsvContent = "mutation,count,proportion\n5G,1,0.5",
                    expectedTsvContent = "mutation\tcount\tproportion\n5G\t1\t0.5",
                    setupMock =
                        { every { it.computeAminoAcidMutationProportions(any()) } returns aminoAcidMutationData },
                )

                NUCLEOTIDE_INSERTIONS -> forDataFormats(
                    endpoint = NUCLEOTIDE_INSERTIONS_ROUTE,
                    expectedFilename = "nucleotideInsertions",
                    expectedJsonContent = """[{"insertion":"123:GGA","count":1}]""",
                    expectedCsvContent = "insertion,count\n123:GGA,1",
                    expectedTsvContent = "insertion\tcount\n123:GGA\t1",
                    setupMock = { every { it.getNucleotideInsertions(any()) } returns nucleotideInsertionData },
                )

                AMINO_ACID_INSERTIONS -> forDataFormats(
                    endpoint = AMINO_ACID_INSERTIONS_ROUTE,
                    expectedFilename = "aminoAcidInsertions",
                    expectedJsonContent = """[{"insertion":"123:GGA","count":1}]""",
                    expectedCsvContent = "insertion,count\n123:GGA,1",
                    expectedTsvContent = "insertion\tcount\n123:GGA\t1",
                    setupMock = { every { it.getAminoAcidInsertions(any()) } returns aminoAcidInsertionData },
                )

                ALIGNED_NUCLEOTIDE_SEQUENCES -> listOf(
                    DownloadAsFileScenario(
                        endpoint = "$ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/segmentName",
                        requestedDataFormat = null,
                        expectedFilename = "alignedNucleotideSequences.fasta",
                        expectedFileContent = SEQUENCE_DATA,
                        setupMock = { every { it.getGenomicSequence(any(), any(), any()) } returns SEQUENCE_DATA },
                    ),
                )

                ALIGNED_AMINO_ACID_SEQUENCES -> listOf(
                    DownloadAsFileScenario(
                        endpoint = "$ALIGNED_AMINO_ACID_SEQUENCES_ROUTE/S",
                        requestedDataFormat = null,
                        expectedFilename = "alignedAminoAcidSequences.fasta",
                        expectedFileContent = SEQUENCE_DATA,
                        setupMock = { every { it.getGenomicSequence(any(), any(), any()) } returns SEQUENCE_DATA },
                    ),
                )

                UNALIGNED_NUCLEOTIDE_SEQUENCES -> listOf(
                    DownloadAsFileScenario(
                        endpoint = "$UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/segmentName",
                        requestedDataFormat = null,
                        expectedFilename = "unalignedNucleotideSequences.fasta",
                        expectedFileContent = SEQUENCE_DATA,
                        setupMock = { every { it.getGenomicSequence(any(), any(), any()) } returns SEQUENCE_DATA },
                    ),
                )

                else -> throw IllegalArgumentException(
                    "There is no ${DownloadAsFileScenario::class} defined for endpoint $route",
                )
            }

        private fun forDataFormats(
            endpoint: String,
            expectedFilename: String,
            expectedJsonContent: String,
            expectedCsvContent: String,
            expectedTsvContent: String,
            setupMock: (SiloQueryModel) -> Any,
        ) = listOf(
            DownloadAsFileScenario(
                endpoint = endpoint,
                requestedDataFormat = "json",
                expectedFilename = "$expectedFilename.json",
                expectedFileContent = expectedJsonContent,
                setupMock = setupMock,
            ),
            DownloadAsFileScenario(
                endpoint = endpoint,
                requestedDataFormat = "csv",
                expectedFilename = "$expectedFilename.csv",
                expectedFileContent = expectedCsvContent,
                setupMock = setupMock,
            ),
            DownloadAsFileScenario(
                endpoint = endpoint,
                requestedDataFormat = "tsv",
                expectedFilename = "$expectedFilename.tsv",
                expectedFileContent = expectedTsvContent,
                setupMock = setupMock,
            ),
        )

        private val aggregationData = listOf(
            AggregationData(
                1,
                mapOf("country" to TextNode("Switzerland")),
            ),
        )

        private val detailsData = listOf(
            DetailsData(
                mapOf(
                    "country" to TextNode("Switzerland"),
                    "age" to TextNode("42"),
                ),
            ),
        )

        private val nucleotideMutationData = listOf(NucleotideMutationResponse("5G", 1, 0.5))
        private val aminoAcidMutationData = listOf(AminoAcidMutationResponse("5G", 1, 0.5))
        private val nucleotideInsertionData = listOf(NucleotideInsertionResponse("123:GGA", 1))
        private val aminoAcidInsertionData = listOf(AminoAcidInsertionResponse("123:GGA", 1))
        private const val SEQUENCE_DATA = ">dummyFastaHeader\ntheSequence\n"
    }
}
