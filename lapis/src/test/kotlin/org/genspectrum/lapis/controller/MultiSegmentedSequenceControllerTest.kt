package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.config.REFERENCE_GENOME_GENES_APPLICATION_ARG_PREFIX
import org.genspectrum.lapis.config.REFERENCE_GENOME_SEGMENTS_APPLICATION_ARG_PREFIX
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.response.SequenceData
import org.genspectrum.lapis.silo.DataVersion
import org.genspectrum.lapis.silo.SequenceType
import org.hamcrest.Matchers.startsWith
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.stream.Stream

private const val SEGMENT_NAME = "otherSegment"

@SpringBootTest(
    properties = [
        "$REFERENCE_GENOME_SEGMENTS_APPLICATION_ARG_PREFIX=someSegment,$SEGMENT_NAME",
        "$REFERENCE_GENOME_GENES_APPLICATION_ARG_PREFIX=gene1,gene2",
    ],
)
@AutoConfigureMockMvc
class MultiSegmentedSequenceControllerTest(
    @Autowired val mockMvc: MockMvc,
) {
    val returnedValue: Stream<SequenceData> = MockDataForEndpoints
        .sequenceEndpointMockData(SEGMENT_NAME)
        .sequenceData
        .stream()

    val expectedFasta = MockDataForEndpoints
        .sequenceEndpointMockData(SEGMENT_NAME)
        .expectedFasta

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
    fun `GIVEN nucleotide sequences request with dataFormat csv THEN returns not acceptable`() {
        mockMvc
            .perform(
                getSample("/${SampleRoute.ALIGNED_NUCLEOTIDE_SEQUENCES.pathSegment}/$SEGMENT_NAME?dataFormat=csv"),
            ).andExpect(status().isNotAcceptable)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(jsonPath("\$.detail", startsWith("Acceptable representations:")))
    }

    @ParameterizedTest(name = "should {0} alignedNucleotideSequences with empty filter")
    @MethodSource("getRequestsWithoutFilter")
    fun `should call alignedNucleotideSequences with empty filter`(
        description: String,
        request: (String) -> MockHttpServletRequestBuilder,
    ) {
        every {
            siloQueryModelMock.getGenomicSequence(
                sequenceFiltersRequest(emptyMap()),
                SequenceType.ALIGNED,
                SEGMENT_NAME,
            )
        } returns returnedValue

        mockMvc.perform(request("$ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/$SEGMENT_NAME"))
            .andExpect(status().isOk)
            .andExpect(content().string(expectedFasta))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getAlignedRequestsWithFilter")
    fun `should call alignedNucleotideSequences with filter`(scenario: SequenceEndpointTestScenario) {
        every {
            siloQueryModelMock.getGenomicSequence(
                sequenceFiltersRequest(mapOf("country" to "Switzerland")),
                SequenceType.ALIGNED,
                SEGMENT_NAME,
            )
        } returns returnedValue

        val responseContent = mockMvc.perform(scenario.request)
            .andExpect(status().isOk)
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
            .andReturn()
            .response
            .contentAsString

        scenario.mockData.assertDataMatches(responseContent)
    }

    @ParameterizedTest(name = "should not {0} alignedNucleotideSequence without segment")
    @MethodSource("getRequestsWithoutFilter")
    fun `should not call alignedNucleotideSequence without segment`(
        description: String,
        request: (String) -> MockHttpServletRequestBuilder,
    ) {
        every {
            siloQueryModelMock.getGenomicSequence(
                sequenceFiltersRequest(emptyMap()),
                SequenceType.ALIGNED,
                "someSegment",
            )
        } returns returnedValue

        mockMvc.perform(request(ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE))
            .andExpect(status().isNotFound)
    }

    @ParameterizedTest(name = "should {0} unalignedNucleotideSequences with empty filter")
    @MethodSource("getRequestsWithoutFilter")
    fun `should call unalignedNucleotideSequences with empty filter`(
        description: String,
        request: (String) -> MockHttpServletRequestBuilder,
    ) {
        every {
            siloQueryModelMock.getGenomicSequence(
                sequenceFiltersRequest(emptyMap()),
                SequenceType.UNALIGNED,
                SEGMENT_NAME,
            )
        } returns returnedValue

        mockMvc.perform(request("$UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/$SEGMENT_NAME"))
            .andExpect(status().isOk)
            .andExpect(content().string(expectedFasta))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getUnalignedRequestsWithFilter")
    fun `should call unalignedNucleotideSequences with filter`(scenario: SequenceEndpointTestScenario) {
        every {
            siloQueryModelMock.getGenomicSequence(
                sequenceFiltersRequest(mapOf("country" to "Switzerland")),
                SequenceType.UNALIGNED,
                SEGMENT_NAME,
            )
        } returns returnedValue

        val responseContent = mockMvc.perform(scenario.request)
            .andExpect(status().isOk)
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
            .andReturn()
            .response
            .contentAsString

        scenario.mockData.assertDataMatches(responseContent)
    }

    @ParameterizedTest(name = "should not {0} unalignedNucleotideSequences without segment")
    @MethodSource("getRequestsWithoutFilter")
    fun `should not call unalignedNucleotideSequences without segment`(
        description: String,
        request: (String) -> MockHttpServletRequestBuilder,
    ) {
        every {
            siloQueryModelMock.getGenomicSequence(
                sequenceFiltersRequest(emptyMap()),
                SequenceType.ALIGNED,
                "someSegment",
            )
        } returns returnedValue

        mockMvc.perform(request(UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE))
            .andExpect(status().isNotFound)
    }

    companion object {
        @JvmStatic
        val alignedRequestsWithFilter = SequenceEndpointTestScenario.createScenarios(
            route = "$ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/$SEGMENT_NAME",
            sequenceName = SEGMENT_NAME,
        )

        @JvmStatic
        val unalignedRequestsWithFilter = SequenceEndpointTestScenario.createScenarios(
            route = "$UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/$SEGMENT_NAME",
            sequenceName = SEGMENT_NAME,
        )

        @JvmStatic
        val requestsWithoutFilter = listOf(
            Arguments.of(
                "GET",
                { route: String -> getSample(route) },
            ),
            Arguments.of(
                "POST JSON",
                { route: String ->
                    postSample(route)
                        .content("""{}""")
                        .contentType(MediaType.APPLICATION_JSON)
                },
            ),
            // Spring doesn't support empty form encoded requests
        )
    }
}
