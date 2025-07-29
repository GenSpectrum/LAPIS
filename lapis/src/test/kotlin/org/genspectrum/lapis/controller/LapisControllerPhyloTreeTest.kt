package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.response.LapisInfo
import org.genspectrum.lapis.response.MostCommonAncestorData
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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

    @Test
    fun `GIVEN call to mostRecentCommonAncestor endpoint without PhyloTreeField returns error`() {
        mockMvc.perform(getSample("/mostRecentCommonAncestor"))
            .andExpect(status().isBadRequest)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(jsonPath("\$.detail", startsWith("Required parameter 'phyloTreeField' is not present")))

        mockMvc.perform(
            postSample("/mostRecentCommonAncestor")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(header().string("Content-Type", "application/json"))
            .andExpect(
                jsonPath(
                    "\$.error.detail",
                    startsWith("phyloTreeField is required and must be a string representing a phylo tree field"),
                ),
            )
    }

    @Test
    fun `GIVEN call to mostRecentCommonAncestor endpoint with miscapitalized PhyloTreeField returns ok`() {
        every {
            siloQueryModelMock.getMostRecentCommonAncestor(
                phyloTreeSequenceFiltersRequest(
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
    fun `GIVEN call to mostRecentCommonAncestor endpoint with invalid PhyloTreeField returns 400 error`() {
        listOf(
            mockMvc.perform(getSample("/mostRecentCommonAncestor?phyloTreeField=floatValue")),
            mockMvc.perform(
                postSample("/mostRecentCommonAncestor")
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
