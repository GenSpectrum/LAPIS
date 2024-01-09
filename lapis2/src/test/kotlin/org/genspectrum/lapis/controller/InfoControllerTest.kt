package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.response.InfoData
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class InfoControllerTest(
    @Autowired val mockMvc: MockMvc,
) {
    @MockkBean
    lateinit var siloQueryModelMock: SiloQueryModel

    @Test
    fun `GET info`() {
        every {
            siloQueryModelMock.getInfo()
        } returns InfoData("1234")

        mockMvc.perform(getSample(INFO_ROUTE))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.dataVersion").value("1234"))
    }

    @Test
    fun `GET databaseConfig`() {
        mockMvc.perform(getSample(DATABASE_CONFIG_ROUTE))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.schema.instanceName").value("sars_cov-2_minimal_test_config"))
            .andExpect(jsonPath("\$.schema.metadata[0].name").value("primaryKey"))
            .andExpect(jsonPath("\$.schema.metadata[0].type").value("string"))
    }

    @Test
    fun `GET referenceGenome`() {
        mockMvc.perform(getSample(REFERENCE_GENOME_ROUTE))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.nucleotideSequences[0].name").value("main"))
            .andExpect(jsonPath("\$.nucleotideSequences[0].sequence").value("ATTA"))
            .andExpect(jsonPath("\$.genes[0].name").value("E"))
            .andExpect(jsonPath("\$.genes[0].sequence").value("MYSFVSEET*"))
    }
}
