package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.config.REFERENCE_GENOME_GENES_APPLICATION_ARG_PREFIX
import org.genspectrum.lapis.config.REFERENCE_GENOME_SEGMENTS_APPLICATION_ARG_PREFIX
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.CoOccurrencePosition
import org.genspectrum.lapis.request.CoOccurrenceRequest
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.silo.DataVersion
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.node.StringNode
import java.util.stream.Stream

private const val SEGMENT_NAME = "otherSegment"

@SpringBootTest(
    properties = [
        "$REFERENCE_GENOME_SEGMENTS_APPLICATION_ARG_PREFIX=$SEGMENT_NAME",
        "$REFERENCE_GENOME_GENES_APPLICATION_ARG_PREFIX=gene1,gene2",
    ],
)
@AutoConfigureMockMvc
class SingleSegmentedCoOccurrenceControllerTest(
    @param:Autowired val mockMvc: MockMvc,
) {
    @MockkBean
    lateinit var siloQueryModelMock: SiloQueryModel

    @MockkBean
    lateinit var dataVersion: DataVersion

    @BeforeEach
    fun setup() {
        every { dataVersion.dataVersion } returns "1234"
    }

    @Test
    fun `GET nucleotideCoOccurrence without a segment in the path resolves the sole segment`() {
        every {
            siloQueryModelMock.getCoOccurrence(
                CoOccurrenceRequest(
                    sequenceFilters = emptyMap(),
                    nucleotideMutations = emptyList(),
                    aminoAcidMutations = emptyList(),
                    nucleotideInsertions = emptyList(),
                    aminoAcidInsertions = emptyList(),
                    positions = listOf(CoOccurrencePosition.Single(1)),
                ),
                SEGMENT_NAME,
            )
        } returns Stream.of(AggregationData(1, mapOf("$SEGMENT_NAME:1" to StringNode("A"))))

        mockMvc.perform(get("/component/nucleotideCoOccurrence?positions=1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0]['$SEGMENT_NAME:1']").value("A"))
            .andExpect(jsonPath("\$.data[0].count").value(1))
    }

    @Test
    fun `GET nucleotideCoOccurrence with a segment in the path returns not found`() {
        mockMvc.perform(get("/component/nucleotideCoOccurrence/someSegment?positions=1"))
            .andExpect(status().isNotFound)
    }
}
