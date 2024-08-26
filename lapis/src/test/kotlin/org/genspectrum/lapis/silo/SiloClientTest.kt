package org.genspectrum.lapis.silo

import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.TextNode
import org.genspectrum.lapis.logging.RequestIdContext
import org.genspectrum.lapis.request.Order
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.InsertionData
import org.genspectrum.lapis.response.MutationData
import org.genspectrum.lapis.response.SequenceData
import org.genspectrum.lapis.scheduler.DataVersionCacheInvalidator
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockserver.client.MockServerClient
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

private const val MOCK_SERVER_PORT = 1080

private const val REQUEST_ID_VALUE = "someRequestId"

private const val DATA_VERSION_HEADER = "data-version"

@SpringBootTest(properties = ["silo.url=http://localhost:$MOCK_SERVER_PORT"])
class SiloClientTest(
    @Autowired private val underTest: SiloClient,
    @Autowired private val requestIdContext: RequestIdContext,
    @Autowired private val dataVersion: DataVersion,
) {
    private lateinit var mockServer: ClientAndServer

    private lateinit var someQuery: SiloQuery<*>

    private var counter = 0

    @BeforeEach
    fun setup() {
        mockServer = ClientAndServer.startClientAndServer(MOCK_SERVER_PORT)
        requestIdContext.requestId = REQUEST_ID_VALUE

        someQuery = SiloQuery(
            SiloAction.aggregated(),
            StringEquals("theColumn", "a value that is difference for each test method: $counter"),
        )
        counter++
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
                    """
                        {"count": 6,"division": "Aargau"}
                        {"count": 8,"division": "Basel-Land"}
                    """,
                ),
        )

        val query = SiloQuery(SiloAction.aggregated(), StringEquals("theColumn", "theValue"))
        val result = underTest.sendQuery(query).toList()

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

    @ParameterizedTest
    @MethodSource("getMutationActions")
    fun `given server returns mutations response then response can be deserialized`(action: SiloAction<MutationData>) {
        expectQueryRequestAndRespondWith(
            response()
                .withContentType(MediaType.APPLICATION_JSON_UTF_8)
                .withBody(
                    """
{"count": 51,"mutation": "C3037T","mutationFrom": "C","mutationTo": "T","position": 3037,"proportion": 1,"sequenceName": "main"}
{"count": 52,"mutation": "C14408T","mutationFrom": "C","mutationTo": "T","position": 14408,"proportion": 1,"sequenceName": "main"}
                    """,
                ),
        )

        val query = SiloQuery(action, StringEquals("theColumn", "theValue"))
        val result = underTest.sendQuery(query).toList()

        assertThat(result, hasSize(2))
        assertThat(
            result,
            containsInAnyOrder(
                MutationData(
                    mutation = "C3037T",
                    count = 51,
                    proportion = 1.0,
                    sequenceName = "main",
                    mutationFrom = "C",
                    mutationTo = "T",
                    position = 3037,
                ),
                MutationData(
                    mutation = "C14408T",
                    count = 52,
                    proportion = 1.0,
                    sequenceName = "main",
                    mutationFrom = "C",
                    mutationTo = "T",
                    position = 14408,
                ),
            ),
        )
    }

    @Test
    fun `given server returns sequence data then response can be deserialized`() {
        expectQueryRequestAndRespondWith(
            response()
                .withContentType(MediaType.APPLICATION_JSON_UTF_8)
                .withBody(
                    """
                        {"primaryKey": "key1","someSequenceName": "ABCD"}
                        {"primaryKey": "key2","someSequenceName": "DEFG"}
                        {"primaryKey": "key3","someSequenceName": null}
                    """,
                ),
        )

        val query = SiloQuery(
            SiloAction.genomicSequence(SequenceType.ALIGNED, "someSequenceName"),
            StringEquals("theColumn", "theValue"),
        )
        val result = underTest.sendQuery(query).toList()

        assertThat(result, hasSize(3))
        assertThat(
            result,
            containsInAnyOrder(
                SequenceData(sequenceKey = "key1", sequence = "ABCD"),
                SequenceData(sequenceKey = "key2", sequence = "DEFG"),
                SequenceData(sequenceKey = "key3", sequence = null),
            ),
        )
    }

    @Test
    fun `given server returns details response then response can be deserialized`() {
        expectQueryRequestAndRespondWith(
            response()
                .withContentType(MediaType.APPLICATION_JSON_UTF_8)
                .withBody(
                    """
{ "age": 50, "country": "Switzerland", "date": "2021-02-23", "pango_lineage": "B.1.1.7", "qc_value": 0.95 }
{ "age": 54, "country": "Switzerland", "date": "2021-03-19", "pango_lineage": "B.1.1.7", "qc_value": 0.94 }
                    """,
                ),
        )

        val query = SiloQuery(SiloAction.details(), StringEquals("theColumn", "theValue"))
        val result = underTest.sendQuery(query).toList()

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

    @ParameterizedTest
    @MethodSource("getInsertionActions")
    fun `GIVEN server returns insertions response THEN response can be deserialized`(
        action: SiloAction<InsertionData>,
    ) {
        expectQueryRequestAndRespondWith(
            response()
                .withContentType(MediaType.APPLICATION_JSON_UTF_8)
                .withBody(
                    """
{ "count": 1, "insertedSymbols": "SGE", "position": 143, "insertion": "ins_S:247:SGE", "sequenceName": "S" }
{ "count": 2, "insertedSymbols": "EPE", "position": 214, "insertion": "ins_S:214:EPE", "sequenceName": "S" }
                    """,
                ),
        )

        val query = SiloQuery(action, True)
        val result = underTest.sendQuery(query).toList()

        assertThat(result, hasSize(2))
        assertThat(
            result,
            containsInAnyOrder(
                InsertionData(
                    1,
                    "ins_S:247:SGE",
                    "SGE",
                    143,
                    "S",
                ),
                InsertionData(
                    2,
                    "ins_S:214:EPE",
                    "EPE",
                    214,
                    "S",
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

        val exception = assertThrows<SiloException> { underTest.sendQuery(someQuery).toList() }

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

        val exception = assertThrows<SiloException> { underTest.sendQuery(someQuery).toList() }
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

        val exception = assertThrows<RuntimeException> { underTest.sendQuery(someQuery).toList() }
        assertThat(exception.message, containsString("Could not parse response from silo"))
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

        val exception = assertThrows<SiloUnavailableException> { underTest.sendQuery(someQuery).toList() }

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

        val exception = assertThrows<SiloUnavailableException> { underTest.sendQuery(someQuery).toList() }

        assertThat(exception.message, `is`("SILO is currently unavailable: $errorMessage"))
        assertThat(exception.retryAfter, `is`(nullValue()))
    }

    @ParameterizedTest
    @MethodSource("getQueriesThatShouldNotBeCached")
    fun `GIVEN an action that should not be cached WHEN I send the same request twice THEN server is called twice`(
        query: SiloQuery<*>,
    ) {
        val errorMessage = "make this fail so that we see a difference on the second call"

        expectQueryRequestAndRespondWith(
            response()
                .withStatusCode(200)
                .withBody(""),
            Times.exactly(1),
        )
        expectQueryRequestAndRespondWith(
            response()
                .withStatusCode(500)
                .withBody(errorMessage),
            Times.exactly(1),
        )

        underTest.sendQuery(query).toList()

        val exception = assertThrows<SiloException> { underTest.sendQuery(query).toList() }
        assertThat(exception.message, containsString(errorMessage))
    }

    @ParameterizedTest
    @MethodSource("getQueriesThatShouldBeCached")
    fun `GIVEN an action that should be cached WHEN I send the same request twice THEN second time is cached`(
        query: SiloQuery<*>,
    ) {
        expectQueryRequestAndRespondWith(
            response()
                .withStatusCode(200)
                .withBody(""),
            Times.once(),
        )

        val result1 = underTest.sendQuery(query).toList()
        val result2 = underTest.sendQuery(query).toList()

        assertThat(result1, `is`(result2))
    }

    @Test
    fun `GIVEN an action that should be cached WHEN I send request twice THEN data version is populated`() {
        val dataVersionValue = "someDataVersion"
        expectInfoCallAndReturnDataVersion(dataVersionValue)

        expectQueryRequestAndRespondWith(
            response()
                .withStatusCode(200)
                .withHeader(DATA_VERSION_HEADER, dataVersionValue)
                .withBody(""),
            Times.once(),
        )

        val query = queriesThatShouldBeCached[0]

        assertThat(dataVersion.dataVersion, `is`(nullValue()))
        underTest.sendQuery(query).toList()
        assertThat(dataVersion.dataVersion, `is`(dataVersionValue))

        dataVersion.dataVersion = null
        underTest.sendQuery(query).toList()
        assertThat(dataVersion.dataVersion, `is`(dataVersionValue))
    }

    @Test
    fun `GIVEN a cacheable action with randomize=true THEN is not cached`() {
        val errorMessage = "This error should appear"
        expectQueryRequestAndRespondWith(
            response()
                .withStatusCode(200)
                .withBody(""),
            Times.once(),
        )
        expectQueryRequestAndRespondWith(
            response()
                .withStatusCode(500)
                .withBody(errorMessage),
            Times.exactly(1),
        )

        val orderByRandom = OrderByField(
            ORDER_BY_RANDOM_FIELD_NAME,
            Order.ASCENDING,
        )
        val query = SiloQuery(SiloAction.mutations(orderByFields = listOf(orderByRandom)), True)
        assertThat(query.action.cacheable, `is`(true))

        val result = underTest.sendQuery(query).toList()
        assertThat(result, hasSize(0))

        val exception = assertThrows<SiloException> { underTest.sendQuery(query).toList() }
        assertThat(exception.message, containsString(errorMessage))
    }

    companion object {
        @JvmStatic
        val mutationActions = listOf(
            SiloAction.mutations(),
            SiloAction.aminoAcidMutations(),
        )

        @JvmStatic
        val insertionActions = listOf(
            SiloAction.nucleotideInsertions(),
            SiloAction.aminoAcidInsertions(),
        )

        @JvmStatic
        val queriesThatShouldNotBeCached = listOf(
            SiloQuery(SiloAction.details(), True),
            SiloQuery(SiloAction.genomicSequence(SequenceType.ALIGNED, "sequenceName"), True),
            SiloQuery(SiloAction.genomicSequence(SequenceType.UNALIGNED, "sequenceName"), True),
        )

        @JvmStatic
        val queriesThatShouldBeCached = listOf(
            SiloQuery(SiloAction.aggregated(), True),
            SiloQuery(SiloAction.mutations(), True),
            SiloQuery(SiloAction.aminoAcidMutations(), True),
            SiloQuery(SiloAction.nucleotideInsertions(), True),
            SiloQuery(SiloAction.aminoAcidInsertions(), True),
        )
    }
}

@SpringBootTest(properties = ["silo.url=http://localhost:$MOCK_SERVER_PORT"])
class SiloClientAndCacheInvalidatorTest(
    @Autowired private val siloClient: SiloClient,
    @Autowired private val dataVersionCacheInvalidator: DataVersionCacheInvalidator,
    @Autowired private val requestIdContext: RequestIdContext,
    @Autowired private val dataVersion: DataVersion,
) {
    private lateinit var mockServer: ClientAndServer

    val someQuery = SiloQuery(SiloAction.mutations(), True)
    val firstDataVersion = "1"
    val secondDataVersion = "2"

    @BeforeEach
    fun setup() {
        mockServer = ClientAndServer.startClientAndServer(MOCK_SERVER_PORT)
        requestIdContext.requestId = REQUEST_ID_VALUE
    }

    @AfterEach
    fun stopServer() {
        mockServer.stop()
    }

    @Test
    fun `GIVEN there is a new data version WHEN the cache invalidator checks THEN the cache should be cleared`() {
        expectInfoCallAndReturnDataVersion(firstDataVersion, Times.once())
        dataVersionCacheInvalidator.invalidateSiloCache()

        assertThatResultIsCachedOnSecondRequest()

        expectInfoCallAndReturnDataVersion(secondDataVersion, Times.once())
        dataVersionCacheInvalidator.invalidateSiloCache()

        assertThatCacheIsNotHit()
    }

    @Test
    fun `GIVEN SILO is restarting WHEN the cache invalidator checks THEN the cache should be cleared`() {
        expectInfoCallAndReturnDataVersion(firstDataVersion, Times.once())
        dataVersionCacheInvalidator.invalidateSiloCache()

        assertThatResultIsCachedOnSecondRequest()

        expectInfoCallThatReturnsSiloUnavailable()
        dataVersionCacheInvalidator.invalidateSiloCache()

        assertThatCacheIsNotHit()
    }

    private fun assertThatResultIsCachedOnSecondRequest() {
        expectQueryRequestAndRespondWith(
            response()
                .withStatusCode(200)
                .withHeader(DATA_VERSION_HEADER, firstDataVersion)
                .withBody(""),
            Times.once(),
        )

        siloClient.sendQuery(someQuery).toList()
        siloClient.sendQuery(someQuery).toList()
        assertThat(dataVersion.dataVersion, `is`(firstDataVersion))
    }

    private fun assertThatCacheIsNotHit() {
        val errorMessage = "This error should appear"
        expectQueryRequestAndRespondWith(
            response()
                .withStatusCode(500)
                .withHeader(DATA_VERSION_HEADER, secondDataVersion)
                .withBody(errorMessage),
            Times.once(),
        )

        val exception = assertThrows<SiloException> { siloClient.sendQuery(someQuery).toList() }
        assertThat(exception.message, containsString(errorMessage))
    }

    private fun expectInfoCallThatReturnsSiloUnavailable() {
        MockServerClient("localhost", MOCK_SERVER_PORT)
            .`when`(
                request()
                    .withMethod("GET")
                    .withPath("/info"),
                Times.once(),
            )
            .respond(
                response()
                    .withStatusCode(503)
                    .withBody("""{"error":  "Test Error", "message": "currently not available"}"""),
            )
    }
}

private fun expectQueryRequestAndRespondWith(
    httpResponse: HttpResponse,
    times: Times = Times.unlimited(),
) {
    MockServerClient("localhost", MOCK_SERVER_PORT)
        .`when`(
            request()
                .withMethod("POST")
                .withPath("/query")
                .withContentType(MediaType.APPLICATION_JSON)
                .withHeader("X-Request-Id", REQUEST_ID_VALUE),
            times,
        )
        .respond(httpResponse)
}

private fun expectInfoCallAndReturnDataVersion(
    dataVersion: String,
    times: Times = Times.unlimited(),
) {
    MockServerClient("localhost", MOCK_SERVER_PORT)
        .`when`(
            request()
                .withMethod("GET")
                .withPath("/info"),
            times,
        )
        .respond(response().withStatusCode(200).withHeader(DATA_VERSION_HEADER, dataVersion))
}
