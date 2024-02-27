package org.genspectrum.lapis.silo

import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.TextNode
import org.genspectrum.lapis.logging.RequestIdContext
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.MutationData
import org.genspectrum.lapis.response.SequenceData
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockserver.client.MockServerClient
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

private const val MOCK_SERVER_PORT = 1080

private const val REQUEST_ID_VALUE = "someRequestId"

@SpringBootTest(properties = ["silo.url=http://localhost:$MOCK_SERVER_PORT"])
class SiloClientTest(
    @Autowired private val underTest: SiloClient,
    @Autowired private val requestIdContext: RequestIdContext,
) {
    private lateinit var mockServer: ClientAndServer

    private val someQuery = SiloQuery(SiloAction.aggregated(), StringEquals("theColumn", "theValue"))

    @BeforeEach
    fun setupMockServer() {
        mockServer = ClientAndServer.startClientAndServer(MOCK_SERVER_PORT)
        requestIdContext.requestId = REQUEST_ID_VALUE
    }

    @AfterEach
    fun stopServer() {
        mockServer.stop()
    }

    @Test
    fun `given server returns aggregated response then response can be deserialized`() {
        expectQueryRequestAndRespondWith(
            response()
                .withContentType(MediaType.APPLICATION_JSON_UTF_8)
                .withBody(
                    """{
                        "queryResult": [
                            {
                              "count": 6,
                              "division": "Aargau"
                            },
                            {
                              "count": 8,
                              "division": "Basel-Land"
                            }
                        ]
                    }""",
                ),
        )

        val query = SiloQuery(SiloAction.aggregated(), StringEquals("theColumn", "theValue"))
        val result = underTest.sendQuery(query)

        assertThat(
            result,
            equalTo(
                listOf(
                    AggregationData(6, mapOf("division" to TextNode("Aargau"))),
                    AggregationData(8, mapOf("division" to TextNode("Basel-Land"))),
                ),
            ),
        )
    }

    @Test
    fun `given server returns amino acid mutations response then response can be deserialized`() {
        expectQueryRequestAndRespondWith(
            response()
                .withContentType(MediaType.APPLICATION_JSON_UTF_8)
                .withBody(
                    """{
                        "queryResult": [
                            {
                                "count": 45,
                                "mutation": "first mutation",
                                "proportion": 0.9,
                                "sequenceName": "S"
                            },
                            {
                                "count": 44,
                                "mutation": "second mutation",
                                "proportion": 0.7,
                                "sequenceName": "ORF"
                            }
                        ]
                    }""",
                ),
        )

        val query = SiloQuery(SiloAction.aminoAcidMutations(), StringEquals("theColumn", "theValue"))
        val result = underTest.sendQuery(query)

        assertThat(result, hasSize(2))
        assertThat(
            result,
            containsInAnyOrder(
                MutationData("first mutation", 45, 0.9, "S"),
                MutationData("second mutation", 44, 0.7, "ORF"),
            ),
        )
    }

    @Test
    fun `given server returns nucleotide response then response can be deserialized`() {
        expectQueryRequestAndRespondWith(
            response()
                .withContentType(MediaType.APPLICATION_JSON_UTF_8)
                .withBody(
                    """{
                        "queryResult": [
                            {
                                "count": 45,
                                "mutation": "first mutation",
                                "proportion": 0.9,
                                "sequenceName": "main"
                            },
                            {
                                "count": 44,
                                "mutation": "second mutation",
                                "proportion": 0.7,
                                "sequenceName": "otherSequence"
                            }
                        ]
                    }""",
                ),
        )

        val query = SiloQuery(SiloAction.mutations(), StringEquals("theColumn", "theValue"))
        val result = underTest.sendQuery(query)

        assertThat(result, hasSize(2))
        assertThat(
            result,
            containsInAnyOrder(
                MutationData("first mutation", 45, 0.9, "main"),
                MutationData("second mutation", 44, 0.7, "otherSequence"),
            ),
        )
    }

    @Test
    fun `given server returns sequence data then response can be deserialized`() {
        expectQueryRequestAndRespondWith(
            response()
                .withContentType(MediaType.APPLICATION_JSON_UTF_8)
                .withBody(
                    """{
                        "queryResult": [
                            {
                              "primaryKey": "key1",
                              "someSequenceName": "ABCD"
                            },
                            {
                              "primaryKey": "key2",
                              "someSequenceName": "DEFG"
                            }
                        ]
                    }""",
                ),
        )

        val query = SiloQuery(
            SiloAction.genomicSequence(SequenceType.ALIGNED, "someSequenceName"),
            StringEquals("theColumn", "theValue"),
        )
        val result = underTest.sendQuery(query)

        assertThat(result, hasSize(2))
        assertThat(
            result,
            containsInAnyOrder(
                SequenceData("key1", "ABCD"),
                SequenceData("key2", "DEFG"),
            ),
        )
    }

    @Test
    fun `given server returns details response then response can be deserialized`() {
        expectQueryRequestAndRespondWith(
            response()
                .withContentType(MediaType.APPLICATION_JSON_UTF_8)
                .withBody(
                    """{
                        "queryResult": [
                            {
                                "age": 50,
                                "country": "Switzerland",
                                "date": "2021-02-23",
                                "pango_lineage": "B.1.1.7",
                                "qc_value": 0.95
                            },
                            {
                                "age": 54,
                                "country": "Switzerland",
                                "date": "2021-03-19",
                                "pango_lineage": "B.1.1.7",
                                "qc_value": 0.94
                            }
                        ]
                    }""",
                ),
        )

        val query = SiloQuery(SiloAction.details(), StringEquals("theColumn", "theValue"))
        val result = underTest.sendQuery(query)

        assertThat(result, hasSize(2))
        assertThat(
            result,
            containsInAnyOrder(
                DetailsData(
                    mapOf(
                        "age" to IntNode(50),
                        "country" to TextNode("Switzerland"),
                        "date" to TextNode("2021-02-23"),
                        "pango_lineage" to TextNode("B.1.1.7"),
                        "qc_value" to DoubleNode(0.95),
                    ),
                ),
                DetailsData(
                    mapOf(
                        "age" to IntNode(54),
                        "country" to TextNode("Switzerland"),
                        "date" to TextNode("2021-03-19"),
                        "pango_lineage" to TextNode("B.1.1.7"),
                        "qc_value" to DoubleNode(0.94),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `given server returns error in unexpected format then throws exception`() {
        expectQueryRequestAndRespondWith(
            response()
                .withContentType(MediaType.APPLICATION_JSON_UTF_8)
                .withStatusCode(432)
                .withBody("""{"unexpectedKey":  "some unexpected message"}"""),
        )

        val exception = assertThrows<SiloException> { underTest.sendQuery(someQuery) }

        assertThat(exception.statusCode, equalTo(500))
        assertThat(
            exception.message,
            equalTo("""Unexpected error from SILO: {"unexpectedKey":  "some unexpected message"}"""),
        )
    }

    @Test
    fun `given server returns SILO error then throws exception with details and response code`() {
        expectQueryRequestAndRespondWith(
            response()
                .withContentType(MediaType.APPLICATION_JSON_UTF_8)
                .withStatusCode(432)
                .withBody("""{"error":  "Test Error", "message": "test message with details"}"""),
        )

        val exception = assertThrows<SiloException> { underTest.sendQuery(someQuery) }
        assertThat(exception.statusCode, equalTo(432))
        assertThat(exception.message, equalTo("Error from SILO: test message with details"))
    }

    @Test
    fun `given server returns unexpected 200 response then throws exception`() {
        expectQueryRequestAndRespondWith(
            response()
                .withContentType(MediaType.APPLICATION_JSON_UTF_8)
                .withStatusCode(200)
                .withBody("""{"unexpectedField":  "some message"}"""),
        )

        val exception = assertThrows<RuntimeException> { underTest.sendQuery(someQuery) }
        assertThat(exception.message, containsString("value failed for JSON property"))
    }

    @Test
    fun `GIVEN server returns 503 with Retry-After header THEN throws exception with retryAfter value`() {
        val retryAfterValue = "60"
        val errorMessage = "test message with details"

        expectQueryRequestAndRespondWith(
            response()
                .withStatusCode(503)
                .withHeader("Retry-After", retryAfterValue)
                .withBody("""{"error":  "Test Error", "message": "$errorMessage"}"""),
        )

        val exception = assertThrows<SiloUnavailableException> { underTest.sendQuery(someQuery) }

        assertThat(exception.message, `is`("SILO is currently unavailable: $errorMessage"))
        assertThat(exception.retryAfter, `is`(retryAfterValue))
    }

    @Test
    fun `GIVEN server returns 503 without Retry-After header THEN throws exception`() {
        val errorMessage = "test message with details"

        expectQueryRequestAndRespondWith(
            response()
                .withStatusCode(503)
                .withBody("""{"error":  "Test Error", "message": "$errorMessage"}"""),
        )

        val exception = assertThrows<SiloUnavailableException> { underTest.sendQuery(someQuery) }

        assertThat(exception.message, `is`("SILO is currently unavailable: $errorMessage"))
        assertThat(exception.retryAfter, `is`(nullValue()))
    }

    private fun expectQueryRequestAndRespondWith(httpResponse: HttpResponse?) {
        MockServerClient("localhost", MOCK_SERVER_PORT)
            .`when`(
                request()
                    .withMethod("POST")
                    .withPath("/query")
                    .withContentType(MediaType.APPLICATION_JSON)
                    .withHeader("X-Request-Id", REQUEST_ID_VALUE),
            )
            .respond(httpResponse)
    }
}
