package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
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
import org.genspectrum.lapis.request.LapisInfo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
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
