package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.config.REFERENCE_GENOME_GENES_APPLICATION_ARG_PREFIX
import org.genspectrum.lapis.config.REFERENCE_GENOME_SEGMENTS_APPLICATION_ARG_PREFIX
import org.genspectrum.lapis.controller.SequenceEndpointTestScenario.Mode.AllSequences
import org.genspectrum.lapis.controller.SequenceEndpointTestScenario.Mode.SingleSequence
import org.genspectrum.lapis.model.FastaHeaderTemplate
import org.genspectrum.lapis.model.SequenceSymbolType
import org.genspectrum.lapis.model.SequencesResponse
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.SEGMENTS_PROPERTY
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
import org.springframework.http.MediaType.APPLICATION_JSON
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
    @param:Autowired val mockMvc: MockMvc,
) {
    val returnedValue = MockDataForEndpoints
        .sequenceEndpointMockData(SEGMENT_NAME)
        .getSequencesResponse()

    val expectedFasta = MockDataForEndpoints
        .sequenceEndpointMockData(SEGMENT_NAME)
        .expectedFasta

    val otherSegment = "otherSegment"

    val arbitraryOkResponse = SequencesResponse(
        sequenceData = Stream.empty(),
        requestedSequenceNames = listOf(SEGMENT_NAME, otherSegment),
        fastaHeaderTemplate = FastaHeaderTemplate("", emptySet()),
    )

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
                sequenceFilters = sequenceFiltersRequest(emptyMap()),
                sequenceType = SequenceType.ALIGNED,
                sequenceNames = listOf(SEGMENT_NAME),
                rawFastaHeaderTemplate = "{primaryKey}",
                sequenceSymbolType = SequenceSymbolType.NUCLEOTIDE,
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
                sequenceFilters = sequenceFiltersRequest(mapOf("country" to "Switzerland")),
                sequenceType = SequenceType.ALIGNED,
                sequenceNames = listOf(SEGMENT_NAME),
                rawFastaHeaderTemplate = "{primaryKey}",
                sequenceSymbolType = SequenceSymbolType.NUCLEOTIDE,
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("getAlignedRequestsForAllSequencesWithFilter")
    fun `should call allAlignedNucleotideSequences with filter`(scenario: SequenceEndpointTestScenario) {
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
    fun `WHEN getting all aligned sequences with segment THEN calls model with correct arguments`() {
        every {
            siloQueryModelMock.getGenomicSequence(
                sequenceFilters = sequenceFiltersRequest(mapOf("country" to "Switzerland")),
                sequenceType = SequenceType.ALIGNED,
                sequenceNames = listOf(SEGMENT_NAME),
                rawFastaHeaderTemplate = "{primaryKey}|{.segment}",
                sequenceSymbolType = SequenceSymbolType.NUCLEOTIDE,
            )
        } returns SequencesResponse(
            sequenceData = Stream.empty(),
            requestedSequenceNames = listOf(SEGMENT_NAME),
            fastaHeaderTemplate = FastaHeaderTemplate("", emptySet()),
        )

        mockMvc.perform(
            getSample(ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE)
                .param(SEGMENTS_PROPERTY, SEGMENT_NAME)
                .param("country", "Switzerland"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `WHEN posting all aligned sequences with segment THEN calls model with correct arguments`() {
        every {
            siloQueryModelMock.getGenomicSequence(
                sequenceFilters = sequenceFiltersRequestWithSegments(
                    sequenceFilters = mapOf("country" to "Switzerland"),
                    segments = listOf(SEGMENT_NAME),
                ),
                sequenceType = SequenceType.ALIGNED,
                sequenceNames = listOf(SEGMENT_NAME),
                rawFastaHeaderTemplate = "{primaryKey}|{.segment}",
                sequenceSymbolType = SequenceSymbolType.NUCLEOTIDE,
            )
        } returns SequencesResponse(
            sequenceData = Stream.empty(),
            requestedSequenceNames = listOf(SEGMENT_NAME),
            fastaHeaderTemplate = FastaHeaderTemplate("", emptySet()),
        )

        mockMvc.perform(
            postSample(ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE)
                .contentType(APPLICATION_JSON)
                .content("""{"country": "Switzerland", "$SEGMENTS_PROPERTY": ["$SEGMENT_NAME"]}"""),
        )
            .andExpect(status().isOk)
    }

    @ParameterizedTest(name = "should {0} unalignedNucleotideSequences with empty filter")
    @MethodSource("getRequestsWithoutFilter")
    fun `should call unalignedNucleotideSequences with empty filter`(
        description: String,
        request: (String) -> MockHttpServletRequestBuilder,
    ) {
        every {
            siloQueryModelMock.getGenomicSequence(
                sequenceFilters = sequenceFiltersRequest(emptyMap()),
                sequenceType = SequenceType.UNALIGNED,
                sequenceNames = listOf(SEGMENT_NAME),
                rawFastaHeaderTemplate = "{primaryKey}",
                sequenceSymbolType = SequenceSymbolType.NUCLEOTIDE,
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
                sequenceFilters = sequenceFiltersRequest(mapOf("country" to "Switzerland")),
                sequenceType = SequenceType.UNALIGNED,
                sequenceNames = listOf(SEGMENT_NAME),
                rawFastaHeaderTemplate = "{primaryKey}",
                sequenceSymbolType = SequenceSymbolType.NUCLEOTIDE,
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("getUnalignedRequestsForAllSequencesWithFilter")
    fun `should call allUnalignedNucleotideSequences with filter`(scenario: SequenceEndpointTestScenario) {
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
    fun `WHEN getting all unaligned sequences with segment THEN calls model with correct arguments`() {
        every {
            siloQueryModelMock.getGenomicSequence(
                sequenceFilters = sequenceFiltersRequest(mapOf("country" to "Switzerland")),
                sequenceType = SequenceType.UNALIGNED,
                sequenceNames = listOf(SEGMENT_NAME, otherSegment),
                rawFastaHeaderTemplate = "{primaryKey}|{.segment}",
                sequenceSymbolType = SequenceSymbolType.NUCLEOTIDE,
            )
        } returns arbitraryOkResponse

        mockMvc.perform(
            getSample(UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE)
                .param(SEGMENTS_PROPERTY, SEGMENT_NAME)
                .param(SEGMENTS_PROPERTY, otherSegment)
                .param("country", "Switzerland"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `WHEN posting all unaligned sequences with segment THEN calls model with correct arguments`() {
        every {
            siloQueryModelMock.getGenomicSequence(
                sequenceFilters = sequenceFiltersRequestWithSegments(
                    sequenceFilters = mapOf("country" to "Switzerland"),
                    segments = listOf(SEGMENT_NAME, otherSegment),
                ),
                sequenceType = SequenceType.UNALIGNED,
                sequenceNames = listOf(SEGMENT_NAME, otherSegment),
                rawFastaHeaderTemplate = "{primaryKey}|{.segment}",
                sequenceSymbolType = SequenceSymbolType.NUCLEOTIDE,
            )
        } returns arbitraryOkResponse

        mockMvc.perform(
            postSample(UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE)
                .contentType(APPLICATION_JSON)
                .content(
                    """{"country": "Switzerland", "$SEGMENTS_PROPERTY": ["$SEGMENT_NAME", "$otherSegment"]}""",
                ),
        )
            .andExpect(status().isOk)
    }

    companion object {
        @JvmStatic
        val alignedRequestsWithFilter = SequenceEndpointTestScenario.createScenarios(
            route = "$ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/$SEGMENT_NAME",
            mode = SingleSequence(SEGMENT_NAME),
        )

        @JvmStatic
        val unalignedRequestsWithFilter = SequenceEndpointTestScenario.createScenarios(
            route = "$UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/$SEGMENT_NAME",
            mode = SingleSequence(SEGMENT_NAME),
        )

        @JvmStatic
        val alignedRequestsForAllSequencesWithFilter = SequenceEndpointTestScenario.createScenarios(
            route = ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE,
            mode = AllSequences,
        )

        @JvmStatic
        val unalignedRequestsForAllSequencesWithFilter = SequenceEndpointTestScenario.createScenarios(
            route = UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE,
            mode = AllSequences,
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
                        .contentType(APPLICATION_JSON)
                },
            ),
            // Spring doesn't support empty form encoded requests
        )
    }
}
