package org.genspectrum.lapis.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.genspectrum.lapis.model.QueryParseModel
import org.genspectrum.lapis.response.ParsedQueryResult
import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.DataVersion
import org.genspectrum.lapis.silo.IntBetween
import org.genspectrum.lapis.silo.StringEquals
import org.genspectrum.lapis.silo.True
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.`is`
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
    lateinit var queryParseModel: QueryParseModel

    @MockkBean
    lateinit var dataVersion: DataVersion

    private val route = "/query/parse"

    @BeforeEach
    fun setup() {
        every { dataVersion.dataVersion } returns "1234"
    }

    @Test
    fun `single valid query returns filter`() {
        every {
            queryParseModel.parseQueries(listOf("country = 'USA'"))
        } returns listOf(
            ParsedQueryResult(
                filter = StringEquals("country", "USA"),
                error = null,
            ),
        )

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
        every {
            queryParseModel.parseQueries(listOf("country = 'USA'", "age >= 30"))
        } returns listOf(
            ParsedQueryResult(
                filter = StringEquals("country", "USA"),
                error = null,
            ),
            ParsedQueryResult(
                filter = IntBetween("age", 30, null),
                error = null,
            ),
        )

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
        every {
            queryParseModel.parseQueries(listOf("invalid syntax !!!"))
        } returns listOf(
            ParsedQueryResult(
                filter = null,
                error = "Syntax error at position 15: unexpected token '!!!'",
            ),
        )

        mockMvc.perform(
            post(route)
                .content("""{"queries": ["invalid syntax !!!"]}""")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].filter").doesNotExist())
            .andExpect(jsonPath("$.data[0].error").exists())
            .andExpect(jsonPath("$.data[0].error").value(containsString("Syntax error")))
            .andExpect(jsonPath("$.info.dataVersion").value(1234))
    }

    @Test
    fun `mixed valid and invalid queries return partial results`() {
        every {
            queryParseModel.parseQueries(listOf("country = 'USA'", "bad query", "age >= 30"))
        } returns listOf(
            ParsedQueryResult(filter = StringEquals("country", "USA"), error = null),
            ParsedQueryResult(filter = null, error = "Parse error: unknown operator"),
            ParsedQueryResult(filter = IntBetween("age", 30, null), error = null),
        )

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
        every {
            queryParseModel.parseQueries(listOf("bad1", "bad2", "bad3"))
        } returns listOf(
            ParsedQueryResult(filter = null, error = "Error 1"),
            ParsedQueryResult(filter = null, error = "Error 2"),
            ParsedQueryResult(filter = null, error = "Error 3"),
        )

        mockMvc.perform(
            post(route)
                .content("""{"queries": ["bad1", "bad2", "bad3"]}""")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[0].filter").doesNotExist())
            .andExpect(jsonPath("$.data[0].error").value("Error 1"))
            .andExpect(jsonPath("$.data[1].filter").doesNotExist())
            .andExpect(jsonPath("$.data[1].error").value("Error 2"))
            .andExpect(jsonPath("$.data[2].filter").doesNotExist())
            .andExpect(jsonPath("$.data[2].error").value("Error 3"))
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
        every {
            queryParseModel.parseQueries(emptyList())
        } returns emptyList()

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
        every {
            queryParseModel.parseQueries(listOf("country = 'USA' & age >= 30"))
        } returns listOf(
            ParsedQueryResult(
                filter = And(
                    listOf(
                        StringEquals("country", "USA"),
                        IntBetween("age", 30, null),
                    ),
                ),
                error = null,
            ),
        )

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

    @Test
    fun `special characters in query value are handled correctly`() {
        every {
            queryParseModel.parseQueries(listOf("name = 'O\\'Brien'"))
        } returns listOf(
            ParsedQueryResult(
                filter = StringEquals("name", "O'Brien"),
                error = null,
            ),
        )

        mockMvc.perform(
            post(route)
                .content("""{"queries": ["name = 'O\\'Brien'"]}""")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].filter.value").value("O'Brien"))
    }

    @Test
    fun `queryParseModel is called with correct arguments`() {
        val queriesSlot = slot<List<String>>()
        every {
            queryParseModel.parseQueries(capture(queriesSlot))
        } returns listOf(
            ParsedQueryResult(filter = True, error = null),
            ParsedQueryResult(filter = True, error = null),
        )

        mockMvc.perform(
            post(route)
                .content("""{"queries": ["query1", "query2"]}""")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify(exactly = 1) { queryParseModel.parseQueries(any()) }
        assertThat(queriesSlot.captured, hasSize(2))
        assertThat(queriesSlot.captured[0], `is`("query1"))
        assertThat(queriesSlot.captured[1], `is`("query2"))
    }

    @Test
    fun `True filter expression is serialized correctly`() {
        every {
            queryParseModel.parseQueries(listOf("true"))
        } returns listOf(
            ParsedQueryResult(
                filter = True,
                error = null,
            ),
        )

        mockMvc.perform(
            post(route)
                .content("""{"queries": ["true"]}""")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].filter.type").value("True"))
    }
}
