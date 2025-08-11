package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.response.LapisInfo
import org.genspectrum.lapis.response.MostCommonAncestorData
import org.genspectrum.lapis.response.PhyloSubtreeData
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.stream.Stream

private const val DATA_VERSION = "1234"

@SpringBootTest
@AutoConfigureMockMvc
class LapisControllerPhyloTreeTest(
    @param:Autowired private val mockMvc: MockMvc,
) {
    @MockkBean
    lateinit var siloQueryModelMock: SiloQueryModel

    @MockkBean
    lateinit var lapisInfo: LapisInfo

    @BeforeEach
    fun setup() {
        every { lapisInfo.dataVersion } returns DATA_VERSION
    }

    @ParameterizedTest
    @ValueSource(strings = ["/mostRecentCommonAncestor", "/phyloSubtree"])
    fun `GIVEN missing phyloTreeField param THEN GET and POST return 400`(endpoint: String) {
        mockMvc.perform(getSample(endpoint))
            .andExpect(status().isBadRequest)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(jsonPath("$.detail", startsWith("Required parameter 'phyloTreeField' is not present")))

        mockMvc.perform(
            postSample(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(header().string("Content-Type", "application/json"))
            .andExpect(
                jsonPath(
                    "$.error.detail",
                    startsWith("phyloTreeField is required and must be a string representing a phylo tree field"),
                ),
            )
    }

    @Test
    fun `GIVEN call to mostRecentCommonAncestor endpoint with miscapitalized PhyloTreeField returns ok`() {
        every {
            siloQueryModelMock.getMostRecentCommonAncestor(
                mrcaSequenceFiltersRequest(
                    phyloTreeField = "primaryKey",
                    sequenceFilters = emptyMap(),
                ),
            )
        } returns Stream.of(
            MostCommonAncestorData(
                "ancestor",
                0,
                "missing",
            ),
        )
        mockMvc.perform(getSample("/mostRecentCommonAncestor?phyloTreeField=PrImArYkEy"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GIVEN call to phyloSubtree endpoint with miscapitalized PhyloTreeField returns ok`() {
        every {
            siloQueryModelMock.getNewick(
                phyloTreeSequenceFiltersRequest(
                    sequenceFilters = emptyMap(),
                    phyloTreeField = "primaryKey",
                ),
            )
        } returns Stream.of(
            PhyloSubtreeData(
                "(node1,node2)root;",
                0,
                "missing",
            ),
        )
        mockMvc.perform(getSample("/phyloSubtree?phyloTreeField=PrImArYkEy"))
            .andExpect(status().isOk)
    }

    @ParameterizedTest
    @ValueSource(strings = ["/mostRecentCommonAncestor", "/phyloSubtree"])
    fun `GIVEN invalid phyloTreeField THEN GET and POST return 400`(endpoint: String) {
        listOf(
            mockMvc.perform(getSample("$endpoint?phyloTreeField=floatValue")),
            mockMvc.perform(
                postSample(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"phyloTreeField": "floatValue"}"""),
            ),
        ).forEach {
            it.andExpect(status().isBadRequest)
                .andExpect(
                    jsonPath(
                        "$.error.detail",
                        containsString(
                            "'floatValue' is not a phylo tree field, known phylo tree fields are [primaryKey]",
                        ),
                    ),
                )
        }
    }
}
