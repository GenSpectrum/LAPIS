package org.genspectrum.lapis.silo

import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import org.genspectrum.lapis.config.SiloVersion
import org.genspectrum.lapis.logging.RequestIdContext
import org.genspectrum.lapis.request.Order
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.InsertionData
import org.genspectrum.lapis.response.MostCommonAncestorData
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
    @param:Autowired private val underTest: SiloClient,
    @param:Autowired private val requestIdContext: RequestIdContext,
    @param:Autowired private val dataVersion: DataVersion,
) {
    private lateinit var mockServer: ClientAndServer

    private lateinit var someQuery: SiloQuery<*>

    private var counter = 0

    private val columnName = "test_column"

    @BeforeEach
    fun setup() {
        mockServer = ClientAndServer.startClientAndServer(MOCK_SERVER_PORT)
        requestIdContext.requestId = REQUEST_ID_VALUE

        someQuery = SiloQuery(
            SiloAction.aggregated(),
            StringEquals("theColumn", "a value that is different for each test method: $counter"),
        )
        counter++
    }

    @AfterEach
    fun stopServer() {
        mockServer.stop()
    }

    @Test
    fun `GIVEN server returns aggregated response THEN response can be deserialized`() {
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
    fun `GIVEN server returns mutations response THEN response can be deserialized`(action: SiloAction<MutationData>) {
        expectQueryRequestAndRespondWith(
            response()
                .withContentType(MediaType.APPLICATION_JSON_UTF_8)
                .withBody(
                    """
{"count": 51,"mutation": "C3037T","mutationFrom": "C","mutationTo": "T","position": 3037,"proportion": 1,"sequenceName": "main","coverage":100}
{"count": 52,"mutation": "C14408T","mutationFrom": "C","mutationTo": "T","position": 14408,"proportion": 1,"sequenceName": "main","coverage":101}
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
                    coverage = 100,
                ),
                MutationData(
                    mutation = "C14408T",
                    count = 52,
                    proportion = 1.0,
                    sequenceName = "main",
                    mutationFrom = "C",
                    mutationTo = "T",
                    position = 14408,
                    coverage = 101,
                ),
            ),
        )
    }

    @Test
    fun `GIVEN server returns sequence data THEN response can be deserialized`() {
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
            SiloAction.genomicSequence(SequenceType.ALIGNED, listOf("someSequenceName")),
            StringEquals("theColumn", "theValue"),
        )
        val result = underTest.sendQuery(query).toList()

        assertThat(result, hasSize(3))
        assertThat(
            result,
            containsInAnyOrder(
                SequenceData(mapOf("primaryKey" to TextNode("key1"), "someSequenceName" to TextNode("ABCD"))),
                SequenceData(mapOf("primaryKey" to TextNode("key2"), "someSequenceName" to TextNode("DEFG"))),
                SequenceData(mapOf("primaryKey" to TextNode("key3"), "someSequenceName" to NullNode.instance)),
            ),
        )
    }

    @Test
    fun `GIVEN server returns unaligned sequence data THEN response can be deserialized`() {
        expectQueryRequestAndRespondWith(
            response()
                .withContentType(MediaType.APPLICATION_JSON_UTF_8)
                .withBody(
                    """
                        {"primaryKey": "key1","unaligned_someSequenceName": "ABCD"}
                        {"primaryKey": "key2","unaligned_someSequenceName": "DEFG"}
                        {"primaryKey": "key3","unaligned_someSequenceName": null}
                    """,
                ),
        )

        val query = SiloQuery(
            SiloAction.genomicSequence(SequenceType.ALIGNED, listOf("someSequenceName")),
            StringEquals("theColumn", "theValue"),
        )
        val result = underTest.sendQuery(query).toList()

        assertThat(result, hasSize(3))
        assertThat(
            result,
            containsInAnyOrder(
                SequenceData(mapOf("primaryKey" to TextNode("key1"), "someSequenceName" to TextNode("ABCD"))),
                SequenceData(mapOf("primaryKey" to TextNode("key2"), "someSequenceName" to TextNode("DEFG"))),
                SequenceData(mapOf("primaryKey" to TextNode("key3"), "someSequenceName" to NullNode.instance)),
            ),
        )
    }

    @Test
    fun `GIVEN server returns details response THEN response can be deserialized`() {
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

    @Test
    fun `GIVEN server returns most recent common ancestor response THEN response can be deserialized`() {
        expectQueryRequestAndRespondWith(
            response()
                .withContentType(MediaType.APPLICATION_JSON_UTF_8)
                .withBody(
                    """
{ "mrcaNode": "node", "missingNodeCount": 5, "missingFromTree": "node1,node2,node3,node4,node5" }
                    """,
                ),
        )

        val query = SiloQuery(
            SiloAction.mostRecentCommonAncestor(
                phyloTreeField = "phyloTreeField",
                printNodesNotInTree = true,
            ),
            StringEquals("theColumn", "theValue"),
        )
        val result = underTest.sendQuery(query).toList()

        assertThat(result, hasSize(1))
        assertThat(
            result[0],
            `is`(
                MostCommonAncestorData(
                    mrcaNode = "node",
                    missingNodeCount = 5,
                    missingFromTree = "node1,node2,node3,node4,node5",
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
    fun `GIVEN server returns error in unexpected format THEN throws exception`() {
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
    fun `GIVEN server returns SILO error THEN throws exception with details and response code`() {
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
    fun `GIVEN server returns unexpected 200 response THEN throws exception`() {
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
        expectInfoCallAndReturnDataVersion(
            dataVersion = dataVersionValue,
            siloVersion = "1.2.3",
        )

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

    @Test
    fun `get lineage definition`() {
        expectLineageDefinitionRequestAndRespondWith(
            """
                A: {}
                A.1:
                  parents:
                  - A
                B:
                  aliases:
                  - A.1.1
                  parents:
                  - A.1
            """.trimIndent(),
        )

        val actual = underTest.getLineageDefinition(columnName)

        assertThat(
            actual,
            equalTo(
                mapOf(
                    "A" to LineageNode(parents = null, aliases = null),
                    "A.1" to LineageNode(parents = listOf("A"), aliases = null),
                    "B" to LineageNode(parents = listOf("A.1"), aliases = listOf("A.1.1")),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN silo returns empty lineage definition file THEN returns empty object`() {
        expectLineageDefinitionRequestAndRespondWith("")

        val actual = underTest.getLineageDefinition(columnName)

        assertThat(actual, equalTo(emptyMap()))
    }

    @Test
    fun `GIVEN silo returns invalid lineage definition file THEN returns appropriate error`() {
        expectLineageDefinitionRequestAndRespondWith("not an object")

        val exception = assertThrows<RuntimeException> { underTest.getLineageDefinition(columnName) }
        assertThat(exception.message, containsString("Failed to parse lineage definition from SILO: "))
    }

    private fun expectLineageDefinitionRequestAndRespondWith(lineageDefinition: String) {
        MockServerClient("localhost", MOCK_SERVER_PORT)
            .`when`(
                request()
                    .withMethod("GET")
                    .withPath("/lineageDefinition/$columnName")
                    .withHeader("X-Request-Id", REQUEST_ID_VALUE),
            )
            .respond(
                response()
                    .withStatusCode(200)
                    .withBody(lineageDefinition),
            )
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
            SiloQuery(SiloAction.genomicSequence(SequenceType.ALIGNED, listOf("sequenceName")), True),
            SiloQuery(SiloAction.genomicSequence(SequenceType.UNALIGNED, listOf("sequenceName")), True),
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
    @param:Autowired private val siloClient: SiloClient,
    @param:Autowired private val dataVersionCacheInvalidator: DataVersionCacheInvalidator,
    @param:Autowired private val requestIdContext: RequestIdContext,
    @param:Autowired private val dataVersion: DataVersion,
    @param:Autowired private val siloVersion: SiloVersion,
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
        expectInfoCallAndReturnDataVersion(
            dataVersion = firstDataVersion,
            siloVersion = "1.2.3",
            times = Times.once(),
        )
        dataVersionCacheInvalidator.invalidateSiloCache()

        assertThatResultIsCachedOnSecondRequest()
        assertThat(siloVersion.version, `is`("1.2.3"))

        expectInfoCallAndReturnDataVersion(
            dataVersion = secondDataVersion,
            siloVersion = "2.3.4",
            times = Times.once(),
        )
        dataVersionCacheInvalidator.invalidateSiloCache()

        assertThatCacheIsNotHit()
        assertThat(siloVersion.version, `is`("2.3.4"))
    }

    @Test
    fun `GIVEN SILO is restarting WHEN the cache invalidator checks THEN the cache should be cleared`() {
        expectInfoCallAndReturnDataVersion(
            dataVersion = firstDataVersion,
            siloVersion = "1.2.3",
            times = Times.once(),
        )
        dataVersionCacheInvalidator.invalidateSiloCache()

        assertThatResultIsCachedOnSecondRequest()

        expectInfoCallThatReturnsSiloUnavailable()
        dataVersionCacheInvalidator.invalidateSiloCache()

        assertThatCacheIsNotHit()
        assertThat(siloVersion.version, `is`(nullValue()))
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
    siloVersion: String,
    times: Times = Times.unlimited(),
) {
    MockServerClient("localhost", MOCK_SERVER_PORT)
        .`when`(
            request()
                .withMethod("GET")
                .withPath("/info"),
            times,
        )
        .respond(
            response()
                .withStatusCode(200)
                .withHeader(DATA_VERSION_HEADER, dataVersion)
                .withBody(
                    """
                        {
                            "version": "$siloVersion"
                        }
                    """.trimIndent(),
                ),
        )
}
