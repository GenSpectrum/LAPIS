package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.luben.zstd.ZstdInputStream
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.genspectrum.lapis.model.mutationsOverTime.DateRange
import org.genspectrum.lapis.model.mutationsOverTime.MutationsOverTimeCell
import org.genspectrum.lapis.model.mutationsOverTime.MutationsOverTimeModel
import org.genspectrum.lapis.model.mutationsOverTime.MutationsOverTimeResult
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.BaseSequenceFilters
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.silo.DataVersion
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
class NucleotideMutationsOverTimeControllerTest(
    @param:Autowired val mockMvc: MockMvc,
    @param:Autowired val objectMapper: ObjectMapper,
) {
    @MockkBean
    lateinit var modelMock: MutationsOverTimeModel

    @MockkBean
    lateinit var dataVersion: DataVersion

    private val route = "/component/nucleotideMutationsOverTime"

    @BeforeEach
    fun setup() {
        every { dataVersion.dataVersion } returns "1234"
    }

    @Test
    fun `POST mutationsOverTime returns expected response`() {
        val resultMock = MutationsOverTimeResult(
            mutations = listOf("A123T", "A456G"),
            dateRanges = listOf(DateRange(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"))),
            data = listOf(
                listOf(MutationsOverTimeCell(count = 10, coverage = 100)),
                listOf(MutationsOverTimeCell(count = 5, coverage = 50)),
            ),
            totalCountsByDateRange = listOf(300),
        )

        val mutationsSlot = slot<List<NucleotideMutation>>()
        val dateRangesSlot = slot<List<DateRange>>()
        val filtersSlot = slot<BaseSequenceFilters>()
        val dateFieldSlot = slot<String>()

        every {
            modelMock.evaluateNucleotideMutations(
                capture(mutationsSlot),
                capture(dateRangesSlot),
                capture(filtersSlot),
                capture(dateFieldSlot),
            )
        } returns resultMock

        mockMvc.perform(
            post(route)
                .content(
                    """{
                        "filters": {
                            "country":"Switzerland"
                        },
                        "includeMutations": ["123T", "456G"],
                        "dateRanges": [{"dateFrom": "2025-01-01", "dateTo": "2025-01-31"}],
                        "dateField": "date"
                    }
                    """.trimIndent(),
                )
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
            .andExpect(jsonPath("$.data.mutations[0]").value("A123T"))
            .andExpect(jsonPath("$.data.mutations[1]").value("A456G"))
            .andExpect(jsonPath("$.data.dateRanges[0].dateFrom").value("2025-01-01"))
            .andExpect(jsonPath("$.data.dateRanges[0].dateTo").value("2025-01-31"))
            .andExpect(jsonPath("$.data.totalCountsByDateRange[0]").value("300"))
            .andExpect(jsonPath("$.data.data[0][0].count").value(10))
            .andExpect(jsonPath("$.data.data[0][0].coverage").value(100))
            .andExpect(jsonPath("$.data.data[1][0].count").value(5))
            .andExpect(jsonPath("$.data.data[1][0].coverage").value(50))
            .andExpect(jsonPath("$.info.dataVersion").value(1234))

        verify(exactly = 1) { modelMock.evaluateNucleotideMutations(any(), any(), any(), any()) }
        assertThat(dateFieldSlot.captured, `is`("date"))
        assertThat(mutationsSlot.captured, hasSize(2))
        assertThat(mutationsSlot.captured[0].sequenceName, `is`(nullValue()))
        assertThat(mutationsSlot.captured[0].position, `is`(123))
        assertThat(mutationsSlot.captured[0].symbol, `is`("T"))
        assertThat(mutationsSlot.captured[1].sequenceName, `is`(nullValue()))
        assertThat(mutationsSlot.captured[1].position, `is`(456))
        assertThat(mutationsSlot.captured[1].symbol, `is`("G"))
        assertThat(dateRangesSlot.captured, hasSize(1))
        assertThat(dateRangesSlot.captured.first().dateFrom, `is`(LocalDate.parse("2025-01-01")))
        assertThat(dateRangesSlot.captured.first().dateTo, `is`(LocalDate.parse("2025-01-31")))
        assertThat(filtersSlot.captured.sequenceFilters["country"], `is`(listOf("Switzerland")))
    }

    @Test
    fun `POST mutationsOverTime compresses result with zstd when requested`() {
        every { modelMock.evaluateNucleotideMutations(any(), any(), any(), any()) } returns MutationsOverTimeResult(
            mutations = listOf("123T"),
            dateRanges = listOf(DateRange(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"))),
            data = listOf(listOf(MutationsOverTimeCell(count = 1, coverage = 2))),
            totalCountsByDateRange = listOf(300),
        )

        val mvcResult = mockMvc.perform(
            post(route)
                .content(
                    """{
                        "filters": {
                            "country":"Switzerland"
                        },
                        "includeMutations": ["123T"],
                        "dateRanges": [{"dateFrom": "2025-01-01", "dateTo": "2025-01-31"}],
                        "dateField": "date",
                        "compression": "zstd",
                        "downloadAsFile": true,
                        "downloadFileBasename": "my-favorite-mutations"
                    }
                    """.trimIndent(),
                )
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "application/zstd"))
            .andExpect(
                header().string(
                    "Content-Disposition",
                    containsString("attachment; filename=my-favorite-mutations.json.zst"),
                ),
            )
            .andReturn()

        val decompressed = ZstdInputStream(ByteArrayInputStream(mvcResult.response.contentAsByteArray))
            .readBytes()
            .toString(StandardCharsets.UTF_8)

        assert(decompressed.contains("\"coverage\":2"))
    }

    @Test
    fun `invalid includeMutations returns bad request`() {
        mockMvc.perform(
            post(route)
                .content(
                    """{
                        "filters": {
                            "country":"Switzerland"
                        },
                        "includeMutations":"123T",
                        "dateRanges":[{"dateFrom":"2020-01-01","dateTo":"2020-06-30"}],
                        "dateField":"collectionDate"
                    }
                    """.trimIndent(),
                )
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.detail").value(containsString("Failed to read request")))
    }

    @Test
    fun `invalid dateRanges returns bad request`() {
        mockMvc.perform(
            post(route)
                .content(
                    """{
                        "filters": {
                            "country":"Switzerland"
                        },
                        "includeMutations": ["123"],
                        "dateRanges": "2025-01-01",
                        "dateField":"date"
                    }
                    """.trimIndent(),
                )
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.detail").value(containsString("Failed to read request")))
    }
}

@SpringBootTest
@AutoConfigureMockMvc
class AminoAcidMutationsOverTimeControllerTest(
    @param:Autowired val mockMvc: MockMvc,
    @param:Autowired val objectMapper: ObjectMapper,
) {
    @MockkBean
    lateinit var modelMock: MutationsOverTimeModel

    @MockkBean
    lateinit var dataVersion: DataVersion

    private val route = "/component/aminoAcidMutationsOverTime"

    @BeforeEach
    fun setup() {
        every { dataVersion.dataVersion } returns "1234"
    }

    @Test
    fun `POST mutationsOverTime returns expected response`() {
        val resultMock = MutationsOverTimeResult(
            mutations = listOf("gene1:A123T", "gene1:A456G"),
            dateRanges = listOf(DateRange(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"))),
            data = listOf(
                listOf(MutationsOverTimeCell(count = 10, coverage = 100)),
                listOf(MutationsOverTimeCell(count = 5, coverage = 50)),
            ),
            totalCountsByDateRange = listOf(300),
        )

        val mutationsSlot = slot<List<AminoAcidMutation>>()
        val dateRangesSlot = slot<List<DateRange>>()
        val filtersSlot = slot<BaseSequenceFilters>()
        val dateFieldSlot = slot<String>()

        every {
            modelMock.evaluateAminoAcidMutations(
                capture(mutationsSlot),
                capture(dateRangesSlot),
                capture(filtersSlot),
                capture(dateFieldSlot),
            )
        } returns resultMock

        mockMvc.perform(
            post(route)
                .content(
                    """{
                        "filters": {
                            "country":"Switzerland"
                        },
                        "includeMutations": ["gene1:A123T", "gene1:A456G"],
                        "dateRanges": [{"dateFrom": "2025-01-01", "dateTo": "2025-01-31"}],
                        "dateField": "date"
                    }
                    """.trimIndent(),
                )
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
            .andExpect(jsonPath("$.data.mutations[0]").value("gene1:A123T"))
            .andExpect(jsonPath("$.data.mutations[1]").value("gene1:A456G"))
            .andExpect(jsonPath("$.data.dateRanges[0].dateFrom").value("2025-01-01"))
            .andExpect(jsonPath("$.data.dateRanges[0].dateTo").value("2025-01-31"))
            .andExpect(jsonPath("$.data.totalCountsByDateRange[0]").value("300"))
            .andExpect(jsonPath("$.data.data[0][0].count").value(10))
            .andExpect(jsonPath("$.data.data[0][0].coverage").value(100))
            .andExpect(jsonPath("$.data.data[1][0].count").value(5))
            .andExpect(jsonPath("$.data.data[1][0].coverage").value(50))
            .andExpect(jsonPath("$.info.dataVersion").value(1234))

        verify(exactly = 1) { modelMock.evaluateAminoAcidMutations(any(), any(), any(), any()) }
        assertThat(dateFieldSlot.captured, `is`("date"))
        assertThat(mutationsSlot.captured, hasSize(2))
        assertThat(mutationsSlot.captured[0].gene, `is`("gene1"))
        assertThat(mutationsSlot.captured[0].position, `is`(123))
        assertThat(mutationsSlot.captured[0].symbol, `is`("T"))
        assertThat(mutationsSlot.captured[1].gene, `is`("gene1"))
        assertThat(mutationsSlot.captured[1].position, `is`(456))
        assertThat(mutationsSlot.captured[1].symbol, `is`("G"))
        assertThat(dateRangesSlot.captured, hasSize(1))
        assertThat(dateRangesSlot.captured.first().dateFrom, `is`(LocalDate.parse("2025-01-01")))
        assertThat(dateRangesSlot.captured.first().dateTo, `is`(LocalDate.parse("2025-01-31")))
        assertThat(filtersSlot.captured.sequenceFilters["country"], `is`(listOf("Switzerland")))
    }

    @Test
    fun `POST mutationsOverTime compresses result with zstd when requested`() {
        every { modelMock.evaluateAminoAcidMutations(any(), any(), any(), any()) } returns MutationsOverTimeResult(
            mutations = listOf("123T"),
            dateRanges = listOf(DateRange(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"))),
            data = listOf(listOf(MutationsOverTimeCell(count = 1, coverage = 2))),
            totalCountsByDateRange = listOf(3),
        )

        val mvcResult = mockMvc.perform(
            post(route)
                .content(
                    """{
                        "filters": {
                            "country":"Switzerland"
                        },
                        "includeMutations": ["gene1:123T"],
                        "dateRanges": [{"dateFrom": "2025-01-01", "dateTo": "2025-01-31"}],
                        "dateField": "date",
                        "compression": "zstd",
                        "downloadAsFile": true,
                        "downloadFileBasename": "my-favorite-mutations"
                    }
                    """.trimIndent(),
                )
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "application/zstd"))
            .andExpect(
                header().string(
                    "Content-Disposition",
                    containsString("attachment; filename=my-favorite-mutations.json.zst"),
                ),
            )
            .andReturn()

        val decompressed = ZstdInputStream(ByteArrayInputStream(mvcResult.response.contentAsByteArray))
            .readBytes()
            .toString(StandardCharsets.UTF_8)

        assert(decompressed.contains("\"coverage\":2"))
    }

    @Test
    fun `invalid includeMutations returns bad request`() {
        mockMvc.perform(
            post(route)
                .content(
                    """{
                        "filters": {
                            "country":"Switzerland"
                        },
                        "includeMutations":"123T",
                        "dateRanges":[{"dateFrom":"2020-01-01","dateTo":"2020-06-30"}],
                        "dateField":"collectionDate"
                    }
                    """.trimIndent(),
                )
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.detail").value(containsString("Failed to read request")))
    }

    @Test
    fun `invalid dateRanges returns bad request`() {
        mockMvc.perform(
            post(route)
                .content(
                    """{
                        "filters": {
                            "country":"Switzerland"
                        },
                        "includeMutations": ["123"],
                        "dateRanges": "2025-01-01",
                        "dateField":"date"
                    }
                    """.trimIndent(),
                )
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.detail").value(containsString("Failed to read request")))
    }
}
