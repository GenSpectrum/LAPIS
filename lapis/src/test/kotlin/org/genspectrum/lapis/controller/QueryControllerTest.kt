package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.genspectrum.lapis.response.InfoData
import org.genspectrum.lapis.silo.DataVersion
import org.genspectrum.lapis.silo.SiloClient
import org.hamcrest.Matchers.containsString
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

@SpringBootTest
@AutoConfigureMockMvc
class QueryControllerTest(
    @param:Autowired val mockMvc: MockMvc,
) {
    @MockkBean
    lateinit var siloClient: SiloClient

    @Autowired
    lateinit var dataVersion: DataVersion

    private val route = "/query/parse"

    @BeforeEach
    fun setup() {
        every {
            siloClient.callInfo()
        } answers {
            dataVersion.dataVersion = "1234"
            InfoData("1234", null)
        }
    }

    @Test
    fun `single valid query returns filter`() {
        mockMvc.perform(
            post(route)
                .content("""{"queries": ["country = 'USA'"]}""")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(header().stringValues("Lapis-Data-Version", "1234"))
            .andExpect(jsonPath("$.data[0].filter").exists())
            .andExpect(jsonPath("$.data[0].filter.type").value("StringEquals"))
            .andExpect(jsonPath("$.data[0].filter.column").value("country"))
            .andExpect(jsonPath("$.data[0].filter.value").value("USA"))
            .andExpect(jsonPath("$.data[0].error").doesNotExist())
            .andExpect(jsonPath("$.info.dataVersion").value(1234))
    }

    @Test
    fun `multiple valid queries return filters`() {
        mockMvc.perform(
            post(route)
                .content("""{"queries": ["country = 'USA'", "age >= 30"]}""")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].filter").exists())
            .andExpect(jsonPath("$.data[0].error").doesNotExist())
            .andExpect(jsonPath("$.data[1].filter").exists())
            .andExpect(jsonPath("$.data[1].error").doesNotExist())
    }

    @Test
    fun `invalid query returns error message`() {
        mockMvc.perform(
            post(route)
                .content("""{"queries": ["invalid syntax !!!"]}""")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].filter").doesNotExist())
            .andExpect(jsonPath("$.data[0].error").exists())
            .andExpect(jsonPath("$.info.dataVersion").value(1234))
    }

    @Test
    fun `mixed valid and invalid queries return partial results`() {
        mockMvc.perform(
            post(route)
                .content("""{"queries": ["country = 'USA'", "bad query", "age >= 30"]}""")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[0].filter").exists())
            .andExpect(jsonPath("$.data[0].error").doesNotExist())
            .andExpect(jsonPath("$.data[1].filter").doesNotExist())
            .andExpect(jsonPath("$.data[1].error").exists())
            .andExpect(jsonPath("$.data[2].filter").exists())
            .andExpect(jsonPath("$.data[2].error").doesNotExist())
    }

    @Test
    fun `all queries fail returns all errors`() {
        mockMvc.perform(
            post(route)
                .content("""{"queries": ["bad1", "bad2", "bad3"]}""")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[0].filter").doesNotExist())
            .andExpect(jsonPath("$.data[0].error").exists())
            .andExpect(jsonPath("$.data[1].filter").doesNotExist())
            .andExpect(jsonPath("$.data[1].error").exists())
            .andExpect(jsonPath("$.data[2].filter").doesNotExist())
            .andExpect(jsonPath("$.data[2].error").exists())
    }

    @Test
    fun `malformed JSON returns 400 Bad Request`() {
        mockMvc.perform(
            post(route)
                .content("""{"queries": [invalid json}""")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value(containsString("Failed to read request")))
    }

    @Test
    fun `wrong type for queries field returns 400 Bad Request`() {
        mockMvc.perform(
            post(route)
                .content("""{"queries": "should be array"}""")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").exists())
    }

    @Test
    fun `missing queries field returns 400 Bad Request`() {
        mockMvc.perform(
            post(route)
                .content("""{"wrongField": []}""")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `empty query list returns empty data array`() {
        mockMvc.perform(
            post(route)
                .content("""{"queries": []}""")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data").isEmpty())
            .andExpect(jsonPath("$.info.dataVersion").value(1234))
    }

    @Test
    fun `complex filter expression with And is serialized correctly`() {
        mockMvc.perform(
            post(route)
                .content("""{"queries": ["country = 'USA' & age >= 30"]}""")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].filter.type").value("And"))
            .andExpect(jsonPath("$.data[0].filter.children").isArray)
            .andExpect(jsonPath("$.data[0].filter.children.length()").value(2))
    }
}
