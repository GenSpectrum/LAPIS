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
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.ACCEPT_ENCODING
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
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
        scenario.mockData.mockWithData(siloQueryModelMock)

        var queryString = "$DOWNLOAD_AS_FILE_PROPERTY=true"
        if (scenario.requestedDataFormat != null) {
            queryString += "&$FORMAT_PROPERTY=${scenario.requestedDataFormat}"
        }

        mockMvc.perform(getSample("${scenario.endpoint}?$queryString"))
            .andExpect(status().isOk)
            .andExpectAttachmentWithContent(
                expectedFilename = scenario.expectedFilename,
                assertFileContentMatches = scenario.mockData.assertDataMatches,
            )
    }

    @ParameterizedTest(name = "POST data from {0} as file")
    @MethodSource("getDownloadAsFileScenarios")
    fun `POST data as file`(scenario: DownloadAsFileScenario) {
        scenario.mockData.mockWithData(siloQueryModelMock)

        val maybeDataFormat = when {
            scenario.requestedDataFormat != null -> """, "$FORMAT_PROPERTY": "${scenario.requestedDataFormat}" """
            else -> ""
        }
        val request = """{ "$DOWNLOAD_AS_FILE_PROPERTY": true $maybeDataFormat }"""

        mockMvc.perform(postSample(scenario.endpoint).content(request).contentType(APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpectAttachmentWithContent(
                expectedFilename = scenario.expectedFilename,
                assertFileContentMatches = scenario.mockData.assertDataMatches,
            )
    }

    @ParameterizedTest(name = "{0} should return compressed file")
    @MethodSource("getCompressedFileScenarios")
    fun `WHEN I request compressed files THEN the filenames have a corresponding suffix`(
        scenario: DownloadCompressedFileScenario,
    ) {
        scenario.mockData.mockToReturnEmptyData(siloQueryModelMock)

        mockMvc.perform(scenario.request)
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", attachmentWithFilename(scenario.expectedFilename)))
            .andExpect(header().string("Content-Encoding", scenario.compressionFormat))
    }

    private fun ResultActions.andExpectAttachmentWithContent(
        expectedFilename: String,
        assertFileContentMatches: (String) -> Unit,
    ) {
        this.andExpect(header().string("Content-Disposition", attachmentWithFilename(expectedFilename)))
            .andReturn()
            .response
            .contentAsString
            .apply(assertFileContentMatches)
    }

    private fun attachmentWithFilename(filename: String) = "attachment; filename=$filename"

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

        private val dataFormatsSequence = generateSequence {
            listOf(
                MockDataCollection.DataFormat.PLAIN_JSON,
                MockDataCollection.DataFormat.CSV,
                MockDataCollection.DataFormat.TSV,
            )
        }.flatten()

        @JvmStatic
        val compressedFileScenarios = dataFormatsSequence.zip(SampleRoute.entries.asSequence())
            .flatMap { (dataFormat, route) -> DownloadCompressedFileScenario.scenariosFor(dataFormat, route) }
            .toList()
    }
}

fun SampleRoute.getExpectedFilename() =
    when (this) {
        AGGREGATED -> "aggregated"
        DETAILS -> "details"
        NUCLEOTIDE_MUTATIONS -> "nucleotideMutations"
        AMINO_ACID_MUTATIONS -> "aminoAcidMutations"
        NUCLEOTIDE_INSERTIONS -> "nucleotideInsertions"
        AMINO_ACID_INSERTIONS -> "aminoAcidInsertions"
        ALIGNED_NUCLEOTIDE_SEQUENCES -> "alignedNucleotideSequences"
        ALIGNED_AMINO_ACID_SEQUENCES -> "alignedAminoAcidSequences"
        UNALIGNED_NUCLEOTIDE_SEQUENCES -> "unalignedNucleotideSequences"
    }

data class DownloadAsFileScenario(
    val endpoint: String,
    val mockData: MockData,
    val requestedDataFormat: String?,
    val expectedFilename: String,
) {
    override fun toString() =
        when (requestedDataFormat) {
            null -> endpoint
            else -> "$endpoint as $requestedDataFormat"
        }

    companion object {
        fun forEndpoint(route: SampleRoute): List<DownloadAsFileScenario> {
            val expectedFilename = route.getExpectedFilename()

            if (route.servesFasta) {
                return listOf(
                    DownloadAsFileScenario(
                        endpoint = "${route.pathSegment}/segmentName",
                        mockData = MockDataForEndpoints.fastaMockData,
                        requestedDataFormat = null,
                        expectedFilename = "$expectedFilename.fasta",
                    ),
                )
            }

            return forDataFormats(route.pathSegment, expectedFilename)
        }

        private fun forDataFormats(
            endpoint: String,
            expectedFilename: String,
        ) = listOf(
            DownloadAsFileScenario(
                mockData = MockDataForEndpoints.getMockData(endpoint)
                    .expecting(MockDataCollection.DataFormat.PLAIN_JSON),
                expectedFilename = "$expectedFilename.json",
                endpoint = endpoint,
                requestedDataFormat = "json",
            ),
            DownloadAsFileScenario(
                mockData = MockDataForEndpoints.getMockData(endpoint).expecting(MockDataCollection.DataFormat.CSV),
                expectedFilename = "$expectedFilename.csv",
                endpoint = endpoint,
                requestedDataFormat = "csv",
            ),
            DownloadAsFileScenario(
                mockData = MockDataForEndpoints.getMockData(endpoint).expecting(MockDataCollection.DataFormat.TSV),
                expectedFilename = "$expectedFilename.tsv",
                endpoint = endpoint,
                requestedDataFormat = "tsv",
            ),
        )
    }
}

data class DownloadCompressedFileScenario(
    val description: String,
    val mockData: MockData,
    val request: MockHttpServletRequestBuilder,
    val expectedFilename: String,
    val compressionFormat: String,
) {
    override fun toString() = description

    companion object {
        fun scenariosFor(
            dataFormat: MockDataCollection.DataFormat,
            route: SampleRoute,
        ) = scenariosFor(
            dataFormat = dataFormat,
            route = route,
            compressionFormat = "gzip",
        ) +
            scenariosFor(
                dataFormat = dataFormat,
                route = route,
                compressionFormat = "zstd",
            )

        private fun scenariosFor(
            dataFormat: MockDataCollection.DataFormat,
            route: SampleRoute,
            compressionFormat: String,
        ): List<DownloadCompressedFileScenario> {
            val (mockData, dataFileFormat) = if (route.servesFasta) {
                MockDataForEndpoints.fastaMockData to "fasta"
            } else {
                MockDataForEndpoints.getMockData(route.pathSegment).expecting(dataFormat) to dataFormat.fileFormat
            }

            val endpoint = when (route) {
                ALIGNED_NUCLEOTIDE_SEQUENCES -> "${route.pathSegment}/main"
                ALIGNED_AMINO_ACID_SEQUENCES -> "${route.pathSegment}/main"
                UNALIGNED_NUCLEOTIDE_SEQUENCES -> "${route.pathSegment}/gene1"
                else -> route.pathSegment
            }
            val acceptHeader = when (route.servesFasta) {
                true -> "*/*"
                false -> dataFormat.acceptHeader
            }

            val expectedFilename = "${route.getExpectedFilename()}.$dataFileFormat.$compressionFormat"

            return listOf(
                DownloadCompressedFileScenario(
                    description = "GET $endpoint as $compressionFormat ${dataFormat.fileFormat}",
                    mockData = mockData,
                    request = getSample("$endpoint?$DOWNLOAD_AS_FILE_PROPERTY=true")
                        .header(ACCEPT_ENCODING, compressionFormat)
                        .header(ACCEPT, acceptHeader),
                    expectedFilename = expectedFilename,
                    compressionFormat = compressionFormat,
                ),
                DownloadCompressedFileScenario(
                    description = "POST $endpoint as $compressionFormat ${dataFormat.fileFormat}",
                    mockData = mockData,
                    request = postSample(endpoint).content("""{ "$DOWNLOAD_AS_FILE_PROPERTY": true }""")
                        .contentType(APPLICATION_JSON)
                        .header(ACCEPT_ENCODING, compressionFormat)
                        .header(ACCEPT, acceptHeader),
                    expectedFilename = expectedFilename,
                    compressionFormat = compressionFormat,
                ),
            )
        }
    }
}
