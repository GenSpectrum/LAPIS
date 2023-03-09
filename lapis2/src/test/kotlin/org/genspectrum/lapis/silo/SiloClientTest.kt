package org.genspectrum.lapis.silo

import org.genspectrum.lapis.response.AggregatedResponse
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockserver.client.MockServerClient
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.*
import org.mockserver.model.MediaType

private const val MOCK_SERVER_PORT = 1080

class SiloClientTest {
    private lateinit var mockServer: ClientAndServer
    private lateinit var underTest: SiloClient

    @BeforeEach
    fun setupMockServer() {
        mockServer = ClientAndServer.startClientAndServer(MOCK_SERVER_PORT)
        underTest = SiloClient("http://localhost:$MOCK_SERVER_PORT")
    }

    @Test
    fun `given server returns aggregated query then response can be deserialized`() {
        expectQueryRequestAndRespondWith(
            """{
                "actionTime": 0,
                "filterTime": 0,
                "parseTime": 0,
                "queryResult": {
                    "count": 1
                }
            }"""
        )

        val query = SiloQuery(SiloAction.aggregated(), StringEquals("theColumn", "theValue"))
        val result = underTest.sendQuery(query)

        assertThat(result, equalTo(AggregatedResponse(1)))
    }

    private fun expectQueryRequestAndRespondWith(response: String) {
        MockServerClient("localhost", MOCK_SERVER_PORT)
            .`when`(
                request()
                    .withMethod("POST")
                    .withPath("/query")
                    .withContentType(MediaType.APPLICATION_JSON)
            )
            .respond(
                response()
                    .withContentType(MediaType.APPLICATION_JSON_UTF_8)
                    .withBody(response)
            )
    }
}