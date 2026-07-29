package org.genspectrum.lapis.openApi

import io.mockk.mockk
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.web.method.HandlerMethod

class OperationsSorterTest {
    @Test
    fun `given content with multiple media types then sorts application json to the top`() {
        val underTest = OperationsSorter()

        val input = Operation().apply {
            responses = ApiResponses()
            responses["jsonFirst"] = ApiResponse().apply {
                content = Content()
                content["application/json"] = null
                content["text/csv"] = null
                content["text/tab-separated-values"] = null
            }
            responses["csvFirst"] = ApiResponse().apply {
                content = Content()
                content["text/csv"] = null
                content["application/json"] = null
                content["text/tab-separated-values"] = null
            }
        }

        val inputCsvFirstKeys = input.responses["csvFirst"]?.content?.keys
        assertThat(inputCsvFirstKeys?.first(), `is`("text/csv"))

        val result = underTest.customize(input, mockk<HandlerMethod>())

        val jsonFirstKeys = result.responses["jsonFirst"]?.content?.keys
        assertThat(jsonFirstKeys?.first(), `is`("application/json"))
        assertThat(jsonFirstKeys, hasSize(3))

        val csvFirstKeys = result.responses["csvFirst"]?.content?.keys
        assertThat(csvFirstKeys?.first(), `is`("application/json"))
        assertThat(jsonFirstKeys, hasSize(3))
    }

    @Test
    fun `given a response without content then does not throw`() {
        val underTest = OperationsSorter()

        val input = Operation().apply {
            responses = ApiResponses()
            responses["404"] = ApiResponse().apply {
                description = "Not Found"
            }
        }

        val result = underTest.customize(input, mockk<HandlerMethod>())

        assertThat(result.responses["404"]?.content, `is`(nullValue()))
    }
}
