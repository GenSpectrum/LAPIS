package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.awaitility.Awaitility.await
import org.genspectrum.lapis.controller.MockDataCollection.DataFormat.PLAIN_JSON
import org.genspectrum.lapis.controller.SampleRoute.AGGREGATED
import org.genspectrum.lapis.controller.SampleRoute.ALIGNED_AMINO_ACID_SEQUENCES
import org.genspectrum.lapis.controller.SampleRoute.ALIGNED_NUCLEOTIDE_SEQUENCES
import org.genspectrum.lapis.controller.SampleRoute.AMINO_ACID_INSERTIONS
import org.genspectrum.lapis.controller.SampleRoute.AMINO_ACID_MUTATIONS
import org.genspectrum.lapis.controller.SampleRoute.DETAILS
import org.genspectrum.lapis.controller.SampleRoute.MOST_RECENT_COMMON_ANCESTOR
import org.genspectrum.lapis.controller.SampleRoute.NUCLEOTIDE_INSERTIONS
import org.genspectrum.lapis.controller.SampleRoute.NUCLEOTIDE_MUTATIONS
import org.genspectrum.lapis.controller.SampleRoute.UNALIGNED_NUCLEOTIDE_SEQUENCES
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.COMPRESSION_PROPERTY
import org.genspectrum.lapis.request.DOWNLOAD_AS_FILE_PROPERTY
import org.genspectrum.lapis.request.DOWNLOAD_FILE_BASENAME_PROPERTY
import org.genspectrum.lapis.request.FORMAT_PROPERTY
import org.genspectrum.lapis.response.LapisInfo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.ACCEPT_ENCODING
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class LapisControllerDownloadAsFileTest(
    @param:Autowired val mockMvc: MockMvc,
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

    @ParameterizedTest(name = "GET data from {0} as file")
    @MethodSource("getDownloadAsFileScenarios")
    fun `GET data as file`(scenario: DownloadAsFileScenario) {
        scenario.mockData.mockWithData(siloQueryModelMock)

        var queryString = "$DOWNLOAD_AS_FILE_PROPERTY=true"
        if (scenario.requestedDataFormat != null) {
            queryString += "&$FORMAT_PROPERTY=${scenario.requestedDataFormat}"
        }
        if (scenario.downloadFileBasename != null) {
            queryString += "&$DOWNLOAD_FILE_BASENAME_PROPERTY=${scenario.downloadFileBasename}"
        }

        mockMvc.perform(
            getSample("${scenario.endpoint}?$queryString")
                .withFieldsQuery(scenario.mockData.fields)
                .withPhyloTreeFieldQuery(scenario.mockData.phyloTreeField),
        )
            .andExpect(status().isOk)
            .andExpectAttachmentWithContent(
                expectedFilename = scenario.expectedFilename,
                assertFileContentMatches = scenario.mockData.assertDataMatches,
            )
    }

    @ParameterizedTest(name = "POST JSON data from {0} as file")
    @MethodSource("getDownloadAsFileScenarios")
    fun `POST JSON data as file`(scenario: DownloadAsFileScenario) {
        scenario.mockData.mockWithData(siloQueryModelMock)

        val maybeDataFormat = when {
            scenario.requestedDataFormat != null -> """, "$FORMAT_PROPERTY": "${scenario.requestedDataFormat}" """
            else -> ""
        }
        val maybeFileBasename = when {
            scenario.downloadFileBasename != null ->
                """, "$DOWNLOAD_FILE_BASENAME_PROPERTY": "${scenario.downloadFileBasename}" """

            else -> ""
        }
        val maybeFields = getFieldsAsJsonPart(scenario.mockData.fields)
        val maybePhyloTreeField = getPhyloTreeFieldAsJsonPart(scenario.mockData.phyloTreeField)
        val request = """
            {
            "$DOWNLOAD_AS_FILE_PROPERTY": true
            $maybeDataFormat $maybeFileBasename $maybeFields $maybePhyloTreeField
            }
        """.trimIndent()

        mockMvc.perform(postSample(scenario.endpoint).content(request).contentType(APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpectAttachmentWithContent(
                expectedFilename = scenario.expectedFilename,
                assertFileContentMatches = scenario.mockData.assertDataMatches,
            )
    }

    @ParameterizedTest(name = "POST form url encoded data from {0} as file")
    @MethodSource("getDownloadAsFileScenarios")
    fun `POST form url encoded data as file`(scenario: DownloadAsFileScenario) {
        val mockData = scenario.mockData
        mockData.mockWithData(siloQueryModelMock)

        val request = postSample(scenario.endpoint)
            .param(DOWNLOAD_AS_FILE_PROPERTY, "true")
            .withFieldsParam(mockData.fields)
            .withPhyloTreeFieldParam(mockData.phyloTreeField)
            .also {
                if (scenario.requestedDataFormat != null) {
                    it.param(FORMAT_PROPERTY, scenario.requestedDataFormat)
                }
                if (scenario.downloadFileBasename != null) {
                    it.param(DOWNLOAD_FILE_BASENAME_PROPERTY, scenario.downloadFileBasename)
                }
            }
            .contentType(APPLICATION_FORM_URLENCODED)

        mockMvc.perform(request)
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
            .andExpect(header().string(CONTENT_DISPOSITION, attachmentWithFilename(scenario.expectedFilename)))
            .andExpect(header().string(CONTENT_TYPE, scenario.expectedContentType))
    }

    @ParameterizedTest
    @ValueSource(strings = [COMPRESSION_FORMAT_GZIP, COMPRESSION_FORMAT_ZSTD, "$COMPRESSION_FORMAT_GZIP, br, deflate"])
    fun `WHEN I request data as file and accept encoding THEN it should return uncompressed file`(
        acceptEncodingHeader: String,
    ) {
        val mockData = MockDataForEndpoints.getMockData(AGGREGATED.pathSegment).expecting(PLAIN_JSON)
        mockData.mockWithData(siloQueryModelMock)

        mockMvc.perform(
            getSample("${AGGREGATED.pathSegment}?$DOWNLOAD_AS_FILE_PROPERTY=true")
                .header(ACCEPT_ENCODING, acceptEncodingHeader),
        )
            .andExpect(status().isOk)
            .andExpectAttachmentWithContent(
                expectedFilename = "aggregated.json",
                assertFileContentMatches = mockData.assertDataMatches,
            )
    }

    @Test
    fun `GIVEN accept header contains several media types THEN picks the first one that matches`() {
        val mockDataCollection = MockDataForEndpoints.getMockData(MOST_RECENT_COMMON_ANCESTOR_ROUTE)
        val mockData = mockDataCollection.expecting(MockDataCollection.DataFormat.PLAIN_JSON)
        mockData.mockWithData(siloQueryModelMock)

        mockMvc.perform(
            getSample("${MOST_RECENT_COMMON_ANCESTOR_ROUTE}?$DOWNLOAD_AS_FILE_PROPERTY=true")
                .withFieldsQuery(mockDataCollection.fields)
                .withPhyloTreeFieldQuery(mockDataCollection.phyloTreeField)
                .header(ACCEPT, "text/plain,application/json"),
        )
            .andExpect(status().isOk)
            .andExpectAttachmentWithContent(
                expectedFilename = "mostRecentCommonAncestor.json",
                assertFileContentMatches = mockData.assertDataMatches,
            )
    }

    @Test
    fun `GIVEN accept headers with quality values THEN picks the matching one with the highest quality`() {
        val mockDataCollection = MockDataForEndpoints.getMockData(AGGREGATED.pathSegment)
        val mockData = mockDataCollection.expecting(MockDataCollection.DataFormat.TSV)
        mockData.mockWithData(siloQueryModelMock)

        mockMvc.perform(
            getSample("${AGGREGATED.pathSegment}?$DOWNLOAD_AS_FILE_PROPERTY=true")
                .withFieldsQuery(mockDataCollection.fields)
                .withPhyloTreeFieldQuery(mockDataCollection.phyloTreeField)
                .header(ACCEPT, "text/plain;q=1,text/csv;q=0.8,text/tab-separated-values;q=0.9"),
        )
            .andExpect(status().isOk)
            .andExpectAttachmentWithContent(
                expectedFilename = "aggregated.tsv",
                assertFileContentMatches = mockData.assertDataMatches,
            )
    }

    private fun ResultActions.andExpectAttachmentWithContent(
        expectedFilename: String,
        assertFileContentMatches: (String) -> Unit,
    ) {
        this.andExpect(header().string("Content-Disposition", attachmentWithFilename(expectedFilename)))
            .andReturn()
            .response
            .also { response ->
                await().until {
                    response.isCommitted
                }
            }
            .contentAsString
            .apply(assertFileContentMatches)
    }

    private fun attachmentWithFilename(filename: String) = "attachment; filename=$filename"

    private companion object {
        @JvmStatic
        val downloadAsFileScenarios = SampleRoute.entries.flatMap { DownloadAsFileScenario.forEndpoint(it) }

        private val dataFormatsSequence = generateSequence {
            listOf(
                PLAIN_JSON,
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
        MOST_RECENT_COMMON_ANCESTOR -> "mostRecentCommonAncestor"
    }

data class DownloadAsFileScenario(
    val endpoint: String,
    val mockData: MockData,
    val requestedDataFormat: String?,
    val expectedFilename: String,
    val downloadFileBasename: String?,
) {
    override fun toString(): String {
        val fileBasename = when (downloadFileBasename) {
            null -> ""
            else -> " with basename $downloadFileBasename"
        }

        val dataFormat = when (requestedDataFormat) {
            null -> ""
            else -> " as $requestedDataFormat"
        }

        return "$endpoint$dataFormat$fileBasename"
    }

    companion object {
        fun forEndpoint(route: SampleRoute): List<DownloadAsFileScenario> {
            val expectedFilename = route.getExpectedFilename()

            if (route.servesFasta) {
                return forSequenceEndpointDataFormats(route, expectedFilename)
            }

            return forDataFormats(route.pathSegment, expectedFilename)
        }

        private fun forSequenceEndpointDataFormats(
            route: SampleRoute,
            expectedFilename: String,
        ) = listOf(
            DownloadAsFileScenario(
                endpoint = "${route.pathSegment}/segmentName",
                mockData = MockDataForEndpoints.sequenceEndpointMockData().expecting(
                    SequenceEndpointMockDataCollection.DataFormat.FASTA,
                ),
                requestedDataFormat = null,
                expectedFilename = "$expectedFilename.fasta",
                downloadFileBasename = null,
            ),
            DownloadAsFileScenario(
                endpoint = "${route.pathSegment}/segmentName",
                mockData = MockDataForEndpoints.sequenceEndpointMockData().expecting(
                    SequenceEndpointMockDataCollection.DataFormat.JSON,
                ),
                requestedDataFormat = SequenceEndpointMockDataCollection.DataFormat.JSON.fileFormat,
                expectedFilename = "$expectedFilename.json",
                downloadFileBasename = null,
            ),
            DownloadAsFileScenario(
                endpoint = "${route.pathSegment}/segmentName",
                mockData = MockDataForEndpoints.sequenceEndpointMockData().expecting(
                    SequenceEndpointMockDataCollection.DataFormat.NDJSON,
                ),
                requestedDataFormat = SequenceEndpointMockDataCollection.DataFormat.NDJSON.fileFormat,
                expectedFilename = "$expectedFilename.ndjson",
                downloadFileBasename = null,
            ),
            DownloadAsFileScenario(
                endpoint = "${route.pathSegment}/segmentName",
                mockData = MockDataForEndpoints.sequenceEndpointMockData().expecting(
                    SequenceEndpointMockDataCollection.DataFormat.FASTA,
                ),
                requestedDataFormat = null,
                expectedFilename = "my_sequence.fasta",
                downloadFileBasename = "my_sequence",
            ),
            DownloadAsFileScenario(
                endpoint = "${route.pathSegment}/segmentName",
                mockData = MockDataForEndpoints.sequenceEndpointMockData().expecting(
                    SequenceEndpointMockDataCollection.DataFormat.JSON,
                ),
                requestedDataFormat = SequenceEndpointMockDataCollection.DataFormat.JSON.fileFormat,
                expectedFilename = "my_sequence.json",
                downloadFileBasename = "my_sequence",
            ),
            DownloadAsFileScenario(
                endpoint = "${route.pathSegment}/segmentName",
                mockData = MockDataForEndpoints.sequenceEndpointMockData().expecting(
                    SequenceEndpointMockDataCollection.DataFormat.NDJSON,
                ),
                requestedDataFormat = SequenceEndpointMockDataCollection.DataFormat.NDJSON.fileFormat,
                expectedFilename = "my_sequence.ndjson",
                downloadFileBasename = "my_sequence",
            ),
        ) +
            listOf(
                DownloadAsFileScenario(
                    endpoint = route.pathSegment,
                    mockData = MockDataForEndpoints.sequenceEndpointMockDataForAllSequences().expecting(
                        SequenceEndpointMockDataCollection.DataFormat.FASTA,
                    ),
                    requestedDataFormat = null,
                    expectedFilename = "$expectedFilename.fasta",
                    downloadFileBasename = null,
                ),
                DownloadAsFileScenario(
                    endpoint = route.pathSegment,
                    mockData = MockDataForEndpoints.sequenceEndpointMockDataForAllSequences().expecting(
                        SequenceEndpointMockDataCollection.DataFormat.JSON,
                    ),
                    requestedDataFormat = SequenceEndpointMockDataCollection.DataFormat.JSON.fileFormat,
                    expectedFilename = "$expectedFilename.json",
                    downloadFileBasename = null,
                ),
                DownloadAsFileScenario(
                    endpoint = route.pathSegment,
                    mockData = MockDataForEndpoints.sequenceEndpointMockDataForAllSequences().expecting(
                        SequenceEndpointMockDataCollection.DataFormat.NDJSON,
                    ),
                    requestedDataFormat = SequenceEndpointMockDataCollection.DataFormat.NDJSON.fileFormat,
                    expectedFilename = "$expectedFilename.ndjson",
                    downloadFileBasename = null,
                ),
                DownloadAsFileScenario(
                    endpoint = route.pathSegment,
                    mockData = MockDataForEndpoints.sequenceEndpointMockDataForAllSequences().expecting(
                        SequenceEndpointMockDataCollection.DataFormat.FASTA,
                    ),
                    requestedDataFormat = null,
                    expectedFilename = "my_sequence.fasta",
                    downloadFileBasename = "my_sequence",
                ),
                DownloadAsFileScenario(
                    endpoint = route.pathSegment,
                    mockData = MockDataForEndpoints.sequenceEndpointMockDataForAllSequences().expecting(
                        SequenceEndpointMockDataCollection.DataFormat.JSON,
                    ),
                    requestedDataFormat = SequenceEndpointMockDataCollection.DataFormat.JSON.fileFormat,
                    expectedFilename = "my_sequence.json",
                    downloadFileBasename = "my_sequence",
                ),
                DownloadAsFileScenario(
                    endpoint = route.pathSegment,
                    mockData = MockDataForEndpoints.sequenceEndpointMockDataForAllSequences().expecting(
                        SequenceEndpointMockDataCollection.DataFormat.NDJSON,
                    ),
                    requestedDataFormat = SequenceEndpointMockDataCollection.DataFormat.NDJSON.fileFormat,
                    expectedFilename = "my_sequence.ndjson",
                    downloadFileBasename = "my_sequence",
                ),
            )

        private fun forDataFormats(
            endpoint: String,
            expectedFilename: String,
        ) = listOf(
            DownloadAsFileScenario(
                mockData = MockDataForEndpoints.getMockData(endpoint)
                    .expecting(PLAIN_JSON),
                expectedFilename = "$expectedFilename.json",
                endpoint = endpoint,
                requestedDataFormat = "json",
                downloadFileBasename = null,
            ),
            DownloadAsFileScenario(
                mockData = MockDataForEndpoints.getMockData(endpoint).expecting(MockDataCollection.DataFormat.CSV),
                expectedFilename = "$expectedFilename.csv",
                endpoint = endpoint,
                requestedDataFormat = "csv",
                downloadFileBasename = null,
            ),
            DownloadAsFileScenario(
                mockData = MockDataForEndpoints.getMockData(endpoint)
                    .expecting(MockDataCollection.DataFormat.CSV_WITHOUT_HEADERS),
                expectedFilename = "$expectedFilename.csv",
                endpoint = endpoint,
                requestedDataFormat = "csv-without-headers",
                downloadFileBasename = null,
            ),
            DownloadAsFileScenario(
                mockData = MockDataForEndpoints.getMockData(endpoint).expecting(MockDataCollection.DataFormat.TSV),
                expectedFilename = "$expectedFilename.tsv",
                endpoint = endpoint,
                requestedDataFormat = "tsv",
                downloadFileBasename = null,
            ),
            DownloadAsFileScenario(
                mockData = MockDataForEndpoints.getMockData(endpoint)
                    .expecting(PLAIN_JSON),
                expectedFilename = "my_file.json",
                endpoint = endpoint,
                requestedDataFormat = "json",
                downloadFileBasename = "my_file",
            ),
            DownloadAsFileScenario(
                mockData = MockDataForEndpoints.getMockData(endpoint).expecting(MockDataCollection.DataFormat.CSV),
                expectedFilename = "my_file.csv",
                endpoint = endpoint,
                requestedDataFormat = "csv",
                downloadFileBasename = "my_file",
            ),
            DownloadAsFileScenario(
                mockData = MockDataForEndpoints.getMockData(endpoint).expecting(MockDataCollection.DataFormat.TSV),
                expectedFilename = "my_file.tsv",
                endpoint = endpoint,
                requestedDataFormat = "tsv",
                downloadFileBasename = "my_file",
            ),
        )
    }
}

data class DownloadCompressedFileScenario(
    val description: String,
    val mockData: MockData,
    val request: MockHttpServletRequestBuilder,
    val expectedFilename: String,
    val expectedContentType: String,
) {
    override fun toString() = description

    companion object {
        fun scenariosFor(
            dataFormat: MockDataCollection.DataFormat,
            route: SampleRoute,
        ) = scenariosFor(
            dataFormat = dataFormat,
            route = route,
            compressionFormat = COMPRESSION_FORMAT_GZIP,
        ) +
            scenariosFor(
                dataFormat = dataFormat,
                route = route,
                compressionFormat = COMPRESSION_FORMAT_ZSTD,
            )

        private fun scenariosFor(
            dataFormat: MockDataCollection.DataFormat,
            route: SampleRoute,
            compressionFormat: String,
        ): List<DownloadCompressedFileScenario> {
            val (mockData, dataFileFormat) = if (route.servesFasta) {
                MockDataForEndpoints.sequenceEndpointMockData()
                    .expecting(SequenceEndpointMockDataCollection.DataFormat.FASTA) to "fasta"
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

            val fileEnding = when (compressionFormat) {
                COMPRESSION_FORMAT_ZSTD -> "zst"
                COMPRESSION_FORMAT_GZIP -> "gz"
                else -> throw Exception("Test issue: unknown compression format $compressionFormat")
            }
            val expectedFilename = "${route.getExpectedFilename()}.$dataFileFormat.$fileEnding"
            val expectedContentType = getContentTypeForCompressionFormat(compressionFormat)
            val maybePhyloTreeField = getPhyloTreeFieldAsJsonPart(mockData.phyloTreeField)
            val maybePhyloTreeFieldParam: String =
                mockData.phyloTreeField
                    ?.let { "&phyloTreeField=$it" }
                    ?: ""

            return listOf(
                DownloadCompressedFileScenario(
                    description = "GET $endpoint as $compressionFormat ${dataFormat.fileFormat}",
                    mockData = mockData,
                    request = getSample(
                        "$endpoint?$DOWNLOAD_AS_FILE_PROPERTY=true&$COMPRESSION_PROPERTY=$compressionFormat$maybePhyloTreeFieldParam",
                    )
                        .header(ACCEPT, acceptHeader),
                    expectedFilename = expectedFilename,
                    expectedContentType = expectedContentType,
                ),
                DownloadCompressedFileScenario(
                    description = "GET $endpoint as $compressionFormat ${dataFormat.fileFormat} with basename",
                    mockData = mockData,
                    request = getSample(
                        "$endpoint?$DOWNLOAD_AS_FILE_PROPERTY=true&$COMPRESSION_PROPERTY=$compressionFormat" +
                            "&$DOWNLOAD_FILE_BASENAME_PROPERTY=my_file$maybePhyloTreeFieldParam",
                    )
                        .header(ACCEPT, acceptHeader),
                    expectedFilename = "my_file.$dataFileFormat.$fileEnding",
                    expectedContentType = expectedContentType,
                ),
                DownloadCompressedFileScenario(
                    description = "POST JSON $endpoint as $compressionFormat ${dataFormat.fileFormat}",
                    mockData = mockData,
                    request = postSample(endpoint).content(
                        """{ "$DOWNLOAD_AS_FILE_PROPERTY": true, "$COMPRESSION_PROPERTY": "$compressionFormat" $maybePhyloTreeField}""",
                    )
                        .contentType(APPLICATION_JSON)
                        .header(ACCEPT, acceptHeader),
                    expectedFilename = expectedFilename,
                    expectedContentType = expectedContentType,
                ),
                DownloadCompressedFileScenario(
                    description = "POST form url encoded $endpoint as $compressionFormat ${dataFormat.fileFormat}",
                    mockData = mockData,
                    request = postSample(endpoint)
                        .param(DOWNLOAD_AS_FILE_PROPERTY, "true")
                        .param(COMPRESSION_PROPERTY, compressionFormat)
                        .withPhyloTreeFieldParam(mockData.phyloTreeField)
                        .contentType(APPLICATION_FORM_URLENCODED)
                        .header(ACCEPT, acceptHeader),
                    expectedFilename = expectedFilename,
                    expectedContentType = expectedContentType,
                ),
            )
        }
    }
}
