package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.CoOccurrencePosition
import org.genspectrum.lapis.request.CoOccurrenceRequest
import org.genspectrum.lapis.request.OrderBySpec
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.silo.DataVersion
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.node.StringNode
import java.util.stream.Stream

/**
 * The default test reference genome is multi-segmented (segments: main, other_segment; genes: gene1, gene2),
 * see application-test.properties. This covers [MultiSegmentedCoOccurrenceController] and
 * [AminoAcidCoOccurrenceController]. [SingleSegmentedCoOccurrenceController] is covered by a dedicated test class.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CoOccurrenceControllerTest(
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
    fun `GET nucleotideCoOccurrence with segment and positions returns relabeled fields`() {
        every {
            siloQueryModelMock.getCoOccurrence(
                CoOccurrenceRequest(
                    sequenceFilters = mapOf("country" to listOf("Switzerland")),
                    nucleotideMutations = emptyList(),
                    aminoAcidMutations = emptyList(),
                    nucleotideInsertions = emptyList(),
                    aminoAcidInsertions = emptyList(),
                    positions = listOf(CoOccurrencePosition.Single(1), CoOccurrencePosition.Single(421)),
                ),
                "main",
                "main",
            )
        } returns Stream.of(
            AggregationData(48, mapOf("main:1" to StringNode("A"), "main:421" to StringNode("T"))),
        )

        mockMvc.perform(get("/component/nucleotideCoOccurrence/main?positions=1,421&country=Switzerland"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(48))
            .andExpect(jsonPath("\$.data[0]['main:1']").value("A"))
            .andExpect(jsonPath("\$.data[0]['main:421']").value("T"))
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
    }

    @Test
    fun `GET nucleotideCoOccurrence with multiple discrete positions`() {
        every {
            siloQueryModelMock.getCoOccurrence(
                CoOccurrenceRequest(
                    sequenceFilters = emptyMap(),
                    nucleotideMutations = emptyList(),
                    aminoAcidMutations = emptyList(),
                    nucleotideInsertions = emptyList(),
                    aminoAcidInsertions = emptyList(),
                    positions = listOf(CoOccurrencePosition.Single(1), CoOccurrencePosition.Single(100)),
                ),
                "main",
                "main",
            )
        } returns Stream.empty()

        mockMvc.perform(get("/component/nucleotideCoOccurrence/main?positions=1,100"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET nucleotideCoOccurrence with a range string in positions returns bad request`() {
        // 'positions' is now List<Int>, so Spring's own type conversion rejects non-integer values before
        // reaching our code - see the comment on the "without positions" test below for the resulting shape.
        mockMvc.perform(get("/component/nucleotideCoOccurrence/main?positions=1,100-102"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.detail", containsString("positions")))
    }

    @Test
    fun `GET nucleotideCoOccurrence without positions returns bad request`() {
        // 'positions' is a required @RequestParam, so this is rejected by Spring itself before reaching our
        // code, which is why the error body has Spring's default ProblemDetail shape instead of LAPIS's usual
        // wrapped {error, info} shape.
        mockMvc.perform(get("/component/nucleotideCoOccurrence/main"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.detail", containsString("positions")))
    }

    @Test
    fun `GET nucleotideCoOccurrence as csv returns csv with dynamic header`() {
        every {
            siloQueryModelMock.getCoOccurrence(
                CoOccurrenceRequest(
                    sequenceFilters = emptyMap(),
                    nucleotideMutations = emptyList(),
                    aminoAcidMutations = emptyList(),
                    nucleotideInsertions = emptyList(),
                    aminoAcidInsertions = emptyList(),
                    positions = listOf(CoOccurrencePosition.Single(1), CoOccurrencePosition.Single(2)),
                ),
                "main",
                "main",
            )
        } returns Stream.of(
            AggregationData(5, mapOf("main:1" to StringNode("A"), "main:2" to StringNode("C"))),
        )

        mockMvc.perform(get("/component/nucleotideCoOccurrence/main?positions=1,2").header("Accept", "text/csv"))
            .andExpect(status().isOk)
            .andExpect(content().string("main:1,main:2,count\nA,C,5\n"))
    }

    @Test
    fun `POST aminoAcidCoOccurrence returns relabeled fields`() {
        every {
            siloQueryModelMock.getCoOccurrence(
                CoOccurrenceRequest(
                    sequenceFilters = mapOf("country" to listOf("Switzerland")),
                    nucleotideMutations = emptyList(),
                    aminoAcidMutations = emptyList(),
                    nucleotideInsertions = emptyList(),
                    aminoAcidInsertions = emptyList(),
                    positions = listOf(CoOccurrencePosition.Single(1), CoOccurrencePosition.Single(2)),
                ),
                "gene1",
                "gene1",
            )
        } returns Stream.of(
            AggregationData(3, mapOf("gene1:1" to StringNode("M"), "gene1:2" to StringNode("K"))),
        )

        mockMvc.perform(
            post("/component/aminoAcidCoOccurrence/gene1")
                .content(
                    """{
                        "country": "Switzerland",
                        "positions": [1, 2]
                    }
                    """.trimIndent(),
                )
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.data[0].count").value(3))
            .andExpect(jsonPath("\$.data[0]['gene1:1']").value("M"))
            .andExpect(jsonPath("\$.data[0]['gene1:2']").value("K"))
    }

    @Test
    fun `POST aminoAcidCoOccurrence with position range`() {
        every {
            siloQueryModelMock.getCoOccurrence(
                CoOccurrenceRequest(
                    sequenceFilters = emptyMap(),
                    nucleotideMutations = emptyList(),
                    aminoAcidMutations = emptyList(),
                    nucleotideInsertions = emptyList(),
                    aminoAcidInsertions = emptyList(),
                    positions = listOf(CoOccurrencePosition.Range(1, 3)),
                    orderByFields = OrderBySpec.EMPTY,
                ),
                "gene1",
                "gene1",
            )
        } returns Stream.empty()

        mockMvc.perform(
            post("/component/aminoAcidCoOccurrence/gene1")
                .content("""{"positions": [{"from": 1, "to": 3}]}""")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify {
            siloQueryModelMock.getCoOccurrence(
                CoOccurrenceRequest(
                    sequenceFilters = emptyMap(),
                    nucleotideMutations = emptyList(),
                    aminoAcidMutations = emptyList(),
                    nucleotideInsertions = emptyList(),
                    aminoAcidInsertions = emptyList(),
                    positions = listOf(CoOccurrencePosition.Range(1, 3)),
                ),
                "gene1",
                "gene1",
            )
        }
    }

    @Test
    fun `POST aminoAcidCoOccurrence without positions returns bad request`() {
        mockMvc.perform(
            post("/component/aminoAcidCoOccurrence/gene1")
                .content("{}")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.error.detail", containsString("positions")))
    }
}
