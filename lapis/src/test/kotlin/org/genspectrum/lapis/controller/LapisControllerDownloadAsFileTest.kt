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
import org.genspectrum.lapis.controller.SampleRoute.NUCLEOTIDE_INSERTIONS
import org.genspectrum.lapis.controller.SampleRoute.NUCLEOTIDE_MUTATIONS
import org.genspectrum.lapis.controller.SampleRoute.UNALIGNED_NUCLEOTIDE_SEQUENCES
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.COMPRESSION_PROPERTY
import org.genspectrum.lapis.request.DOWNLOAD_AS_FILE_PROPERTY
import org.genspectrum.lapis.request.FORMAT_PROPERTY
import org.genspectrum.lapis.response.LapisInfo
import org.junit.jupiter.api.BeforeEach
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

    @ParameterizedTest(name = "POST JSON data from {0} as file")
    @MethodSource("getDownloadAsFileScenarios")
    fun `POST JSON data as file`(scenario: DownloadAsFileScenario) {
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

    @ParameterizedTest(name = "POST form url encoded data from {0} as file")
    @MethodSource("getDownloadAsFileScenarios")
    fun `POST form url encoded data as file`(scenario: DownloadAsFileScenario) {
        scenario.mockData.mockWithData(siloQueryModelMock)

        val request = postSample(scenario.endpoint)
            .param(DOWNLOAD_AS_FILE_PROPERTY, "true")
            .also {
                if (scenario.requestedDataFormat != null) {
                    it.param(FORMAT_PROPERTY, scenario.requestedDataFormat)
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
                    .expecting(PLAIN_JSON),
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

            val fileEnding = when (compressionFormat) {
                COMPRESSION_FORMAT_ZSTD -> "zst"
                COMPRESSION_FORMAT_GZIP -> "gz"
                else -> throw Exception("Test issue: unknown compression format $compressionFormat")
            }
            val expectedFilename = "${route.getExpectedFilename()}.$dataFileFormat.$fileEnding"
            val expectedContentType = getContentTypeForCompressionFormat(compressionFormat)

            return listOf(
                DownloadCompressedFileScenario(
                    description = "GET $endpoint as $compressionFormat ${dataFormat.fileFormat}",
                    mockData = mockData,
                    request = getSample(
                        "$endpoint?$DOWNLOAD_AS_FILE_PROPERTY=true&$COMPRESSION_PROPERTY=$compressionFormat",
                    )
                        .header(ACCEPT, acceptHeader),
                    expectedFilename = expectedFilename,
                    expectedContentType = expectedContentType,
                ),
                DownloadCompressedFileScenario(
                    description = "POST JSON $endpoint as $compressionFormat ${dataFormat.fileFormat}",
                    mockData = mockData,
                    request = postSample(endpoint).content(
                        """{ "$DOWNLOAD_AS_FILE_PROPERTY": true, "$COMPRESSION_PROPERTY": "$compressionFormat" }""",
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
                        .contentType(APPLICATION_FORM_URLENCODED)
                        .header(ACCEPT, acceptHeader),
                    expectedFilename = expectedFilename,
                    expectedContentType = expectedContentType,
                ),
            )
        }
    }
}
