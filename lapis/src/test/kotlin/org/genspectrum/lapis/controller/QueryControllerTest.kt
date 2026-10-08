package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.InfoData
import org.genspectrum.lapis.rhydb.DataVersion
import org.genspectrum.lapis.rhydb.RhyDbClient
import org.genspectrum.lapis.rhydb.RhyDbException
import org.genspectrum.lapis.rhydb.RhyDbQuery
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.stream.Stream

@SpringBootTest
@AutoConfigureMockMvc
class QueryControllerTest(
    @param:Autowired val mockMvc: MockMvc,
) {
    @MockkBean
    lateinit var rhyDbClient: RhyDbClient

    @Autowired
    lateinit var dataVersion: DataVersion

    private val route = "/query/parse"

    @BeforeEach
    fun setup() {
        every {
            rhyDbClient.callInfo()
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
            .andExpect(jsonPath("$.data[0].type").value("success"))
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
            .andExpect(jsonPath("$.data[0].type").value("success"))
            .andExpect(jsonPath("$.data[0].filter").exists())
            .andExpect(jsonPath("$.data[0].error").doesNotExist())
            .andExpect(jsonPath("$.data[1].type").value("success"))
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
            .andExpect(jsonPath("$.data[0].type").value("failure"))
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
            .andExpect(jsonPath("$.data[0].type").value("success"))
            .andExpect(jsonPath("$.data[0].filter").exists())
            .andExpect(jsonPath("$.data[0].error").doesNotExist())
            .andExpect(jsonPath("$.data[1].type").value("failure"))
            .andExpect(jsonPath("$.data[1].filter").doesNotExist())
            .andExpect(jsonPath("$.data[1].error").exists())
            .andExpect(jsonPath("$.data[2].type").value("success"))
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
            .andExpect(jsonPath("$.data[0].type").value("failure"))
            .andExpect(jsonPath("$.data[0].filter").doesNotExist())
            .andExpect(jsonPath("$.data[0].error").exists())
            .andExpect(jsonPath("$.data[1].type").value("failure"))
            .andExpect(jsonPath("$.data[1].filter").doesNotExist())
            .andExpect(jsonPath("$.data[1].error").exists())
            .andExpect(jsonPath("$.data[2].type").value("failure"))
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
            .andExpect(jsonPath("$.data[0].type").value("success"))
            .andExpect(jsonPath("$.data[0].filter.type").value("And"))
            .andExpect(jsonPath("$.data[0].filter.children").isArray)
            .andExpect(jsonPath("$.data[0].filter.children.length()").value(2))
    }

    @Test
    fun `doFullValidation defaults to false when not specified`() {
        mockMvc.perform(
            post(route)
                .content("""{"queries": ["country = 'USA'"]}""")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].type").value("success"))

        verify(exactly = 0) { rhyDbClient.sendQuery(query = any<RhyDbQuery<AggregationData>>(), any()) }
        verify(exactly = 1) { rhyDbClient.callInfo() }
    }

    @Test
    fun `doFullValidation false does not call RHYDB for validation`() {
        mockMvc.perform(
            post(route)
                .content("""{"queries": ["country = 'USA'"], "doFullValidation": false}""")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].type").value("success"))

        verify(exactly = 0) { rhyDbClient.sendQuery(query = any<RhyDbQuery<AggregationData>>(), any()) }
        verify(exactly = 1) { rhyDbClient.callInfo() }
    }

    @Test
    fun `doFullValidation true calls RHYDB for validation and succeeds for valid query`() {
        every {
            rhyDbClient.sendQuery(query = any<RhyDbQuery<AggregationData>>(), any())
        } returns Stream.empty()

        mockMvc.perform(
            post(route)
                .content("""{"queries": ["country = 'USA'"], "doFullValidation": true}""")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].type").value("success"))
            .andExpect(jsonPath("$.data[0].filter").exists())
            .andExpect(jsonPath("$.data[0].error").doesNotExist())

        verify(atLeast = 1) { rhyDbClient.sendQuery(query = any<RhyDbQuery<AggregationData>>(), any()) }
    }

    @Test
    fun `doFullValidation true returns failure when RHYDB rejects query`() {
        every {
            rhyDbClient.sendQuery(query = any<RhyDbQuery<AggregationData>>(), any())
        } throws RhyDbException(400, "Bad Request", "Unknown field: invalidField")

        mockMvc.perform(
            post(route)
                .content("""{"queries": ["main:123456789T"], "doFullValidation": true}""")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].type").value("failure"))
            .andExpect(jsonPath("$.data[0].filter").doesNotExist())
            .andExpect(jsonPath("$.data[0].error").value(containsString("invalidField")))
    }

    @Test
    fun `doFullValidation true returns failure when RHYDB throws 500 error`() {
        every {
            rhyDbClient.sendQuery(query = any<RhyDbQuery<AggregationData>>(), any())
        } throws RhyDbException(500, "Internal Server Error", "Something unexpected happened")

        mockMvc.perform(
            post(route)
                .content("""{"queries": ["main:123"], "doFullValidation": true}""")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].type").value("failure"))
            .andExpect(jsonPath("$.data[0].filter").doesNotExist())
            .andExpect(jsonPath("$.data[0].error").value(containsString("Unexpected error parsing query.")))
    }

    @Test
    fun `doFullValidation true returns partial results for mixed valid and invalid queries`() {
        every {
            rhyDbClient.sendQuery(
                query = match<RhyDbQuery<AggregationData>> { query ->
                    query.filterExpression.toString().contains("USA")
                },
                any(),
            )
        } returns Stream.empty()

        every {
            rhyDbClient.sendQuery(
                query = match<RhyDbQuery<AggregationData>> { query ->
                    query.filterExpression.toString().contains("invalidField")
                },
                any(),
            )
        } throws RhyDbException(400, "Bad Request", "Unknown field: invalidField")

        every {
            rhyDbClient.sendQuery(
                query = match<RhyDbQuery<AggregationData>> { query ->
                    query.filterExpression.toString().contains("age")
                },
                any(),
            )
        } returns Stream.empty()

        mockMvc.perform(
            post(route)
                .content(
                    """{"queries": ["country = 'USA'", "invalidField = 'value'", "age >= 30"], "doFullValidation": true}""",
                )
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[0].type").value("success"))
            .andExpect(jsonPath("$.data[0].filter").exists())
            .andExpect(jsonPath("$.data[1].type").value("failure"))
            .andExpect(jsonPath("$.data[1].error").value(containsString("invalidField")))
            .andExpect(jsonPath("$.data[2].type").value("success"))
            .andExpect(jsonPath("$.data[2].filter").exists())
    }
}
