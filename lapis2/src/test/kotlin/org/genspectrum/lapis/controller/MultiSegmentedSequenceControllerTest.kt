package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.config.REFERENCE_GENOME_GENES_APPLICATION_ARG_PREFIX
import org.genspectrum.lapis.config.REFERENCE_GENOME_SEGMENTS_APPLICATION_ARG_PREFIX
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.DataVersion
import org.genspectrum.lapis.request.SequenceFiltersRequest
import org.genspectrum.lapis.silo.SequenceType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
    properties = [
        "$REFERENCE_GENOME_SEGMENTS_APPLICATION_ARG_PREFIX=someSegment,otherSegment",
        "$REFERENCE_GENOME_GENES_APPLICATION_ARG_PREFIX=gene1,gene2",
    ],
)
@AutoConfigureMockMvc
class MultiSegmentedSequenceControllerTest(
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
    fun `should GET alignedNucleotideSequences with empty filter`() {
        val returnedValue = "TestSequenceContent"
        every {
            siloQueryModelMock.getGenomicSequence(
                sequenceFiltersRequest(emptyMap()),
                SequenceType.ALIGNED,
                "otherSegment",
            )
        } returns returnedValue

        mockMvc.perform(get("$ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/otherSegment"))
            .andExpect(status().isOk)
            .andExpect(content().string(returnedValue))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
    }

    @Test
    fun `should GET alignedNucleotideSequences with filter`() {
        val returnedValue = "TestSequenceContent"
        every {
            siloQueryModelMock.getGenomicSequence(
                sequenceFiltersRequest(mapOf("country" to "Switzerland")),
                SequenceType.ALIGNED,
                "otherSegment",
            )
        } returns returnedValue

        mockMvc.perform(get("$ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/otherSegment?country=Switzerland"))
            .andExpect(status().isOk)
            .andExpect(content().string(returnedValue))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
    }

    @Test
    fun `should POST alignedNucleotideSequences with empty filter`() {
        val returnedValue = "TestSequenceContent"
        every {
            siloQueryModelMock.getGenomicSequence(
                sequenceFiltersRequest(emptyMap()),
                SequenceType.ALIGNED,
                "otherSegment",
            )
        } returns returnedValue

        val request = post("$ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/otherSegment")
            .content("""{}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(content().string(returnedValue))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
    }

    @Test
    fun `should POST alignedNucleotideSequences with filter`() {
        val returnedValue = "TestSequenceContent"
        every {
            siloQueryModelMock.getGenomicSequence(
                sequenceFiltersRequest(mapOf("country" to "Switzerland")),
                SequenceType.ALIGNED,
                "otherSegment",
            )
        } returns returnedValue

        val request = post("$ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/otherSegment")
            .content("""{"country":"Switzerland"}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(content().string(returnedValue))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
    }

    @Test
    fun `should not GET alignedNucleotideSequence without segment`() {
        val returnedValue = "TestSequenceContent"
        every {
            siloQueryModelMock.getGenomicSequence(
                sequenceFiltersRequest(emptyMap()),
                SequenceType.ALIGNED,
                "someSegment",
            )
        } returns returnedValue

        mockMvc.perform(get(ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should not POST alignedNucleotideSequences without segment`() {
        val returnedValue = "TestSequenceContent"
        every {
            siloQueryModelMock.getGenomicSequence(
                sequenceFiltersRequest(emptyMap()),
                SequenceType.ALIGNED,
                "otherSegment",
            )
        } returns returnedValue

        val request = post(ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE)
            .content("""{}""")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(request)
            .andExpect(status().isNotFound)
    }

    private fun sequenceFiltersRequest(sequenceFilters: Map<String, String>) =
        SequenceFiltersRequest(
            sequenceFilters,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
        )
}
