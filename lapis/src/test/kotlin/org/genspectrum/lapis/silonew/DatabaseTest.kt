package org.genspectrum.lapis.silonew

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockserver.client.MockServerClient
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType
import org.mockserver.verify.VerificationTimes
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

private const val DATA_VERSION = "test-data-version"

class DatabaseTest {
    private lateinit var mockServer: ClientAndServer
    private lateinit var mockServerClient: MockServerClient
    private lateinit var database: Database
    private val defaultTable = siloSchema {
        table("default") {
            column("country", SiloColumnType.INDEXED_STRING)
        }
    }.table("default")
    private lateinit var query: SiloRelation

    @BeforeEach
    fun setUp() {
        mockServer = ClientAndServer.startClientAndServer(0)
        mockServerClient = MockServerClient("127.0.0.1", mockServer.localPort)
        database = Database("http://127.0.0.1:${mockServer.localPort}/silo/")
        query = defaultTable.filter(defaultTable["country"] eq literal("CH"))
    }

    @AfterEach
    fun tearDown() {
        database.close()
        mockServer.stop()
    }

    @Test
    fun `GIVEN multiple Arrow batches THEN streams normalized rows and response metadata`() {
        expectSuccessfulQuery(
            buildTestArrowStream(
                listOf(
                    listOf(testRow(text = "first", optional = null), testRow(text = "second", optional = "value")),
                    listOf(testRow(text = "third", optional = "other")),
                ),
            ),
        )

        val result = database.sendQuery(query)

        assertThat(result.dataVersion, equalTo(DATA_VERSION))
        assertThat(result.headers["x-test-header"], equalTo(listOf("test-header-value")))
        assertThrows<UnsupportedOperationException> {
            (result.headers as MutableMap)["another-header"] = emptyList()
        }
        val rows = result.use { it.rows.toList() }
        assertThat(rows, hasSize(3))
        assertThat(
            rows.first().values,
            equalTo(
                mapOf(
                    "text" to "first",
                    "int32" to 12,
                    "int64" to 34L,
                    "float32" to 1.5f,
                    "float64" to 2.5,
                    "flag" to true,
                    "date" to LocalDate.of(2024, 2, 3),
                    "optional" to null,
                ),
            ),
        )
        assertThat(rows.first().getString("text"), equalTo("first"))
        assertThat(rows.first().getInt("int32"), equalTo(12))
        assertThat(rows.first().getLong("int64"), equalTo(34L))
        assertThat(rows.first().getFloat("float32"), equalTo(1.5f))
        assertThat(rows.first().getDouble("float64"), equalTo(2.5))
        assertThat(rows.first().getBoolean("flag"), equalTo(true))
        assertThat(rows.first().getDate("date"), equalTo(LocalDate.of(2024, 2, 3)))
        assertThat(rows[1]["optional"], equalTo("value"))
        assertThat(rows[2]["text"], equalTo("third"))
        assertThrows<UnsupportedOperationException> {
            (rows.first().values as MutableMap)["text"] = "changed"
        }
        assertThat(database.openResultCount, equalTo(0))
        assertThat(database.allocatedArrowMemory, equalTo(0L))

        mockServerClient.verify(expectedQueryRequest(), VerificationTimes.once())
    }

    @Test
    fun `GIVEN short circuit THEN closing result releases resources`() {
        expectSuccessfulQuery(
            buildTestArrowStream(
                listOf(
                    listOf(testRow("first"), testRow("second")),
                    listOf(testRow("third")),
                ),
            ),
        )
        val result = database.sendQuery(query)

        assertThat(database.openResultCount, equalTo(1))

        assertThat(result.rows.findFirst().orElseThrow().getString("text"), equalTo("first"))
        assertThat(database.openResultCount, equalTo(1))

        result.close()

        assertThat(database.openResultCount, equalTo(0))
        assertThat(database.allocatedArrowMemory, equalTo(0L))
    }

    @Test
    fun `GIVEN fully consumed stream THEN closes result automatically`() {
        expectSuccessfulQuery(buildTestArrowStream(listOf(listOf(testRow("only")))))
        val result = database.sendQuery(query)

        assertThat(result.rows.count(), equalTo(1L))

        assertThat(database.openResultCount, equalTo(0))
        assertThat(database.allocatedArrowMemory, equalTo(0L))
    }

    @Test
    fun `GIVEN outstanding result WHEN database closes THEN closes the result`() {
        expectSuccessfulQuery(buildTestArrowStream(listOf(listOf(testRow("only")))))
        database.sendQuery(query)
        assertThat(database.openResultCount, equalTo(1))

        database.close()

        assertThat(database.openResultCount, equalTo(0))
        assertThat(database.allocatedArrowMemory, equalTo(0L))
        val exception = assertThrows<SiloConnectionException> { database.sendQuery(query) }
        assertThat(exception.message, equalTo("The SILO database is closed"))
    }

    @Test
    fun `GIVEN unsupported Arrow vector THEN closes resources and reports the field`() {
        expectSuccessfulQuery(buildUnsupportedArrowStream())
        val result = database.sendQuery(query)

        val exception = assertThrows<SiloResultDecodingException> { result.rows.toList() }

        assertThat(exception.message, containsString("VarBinaryVector"))
        assertThat(exception.message, containsString("binary"))
        assertThat(database.openResultCount, equalTo(0))
    }

    @Test
    fun `GIVEN malformed Arrow response THEN reports decoding failure without retaining memory`() {
        expectSuccessfulQuery("not Arrow".toByteArray())
        val result = database.sendQuery(query)

        val exception = assertThrows<SiloResultDecodingException> { result.rows.toList() }

        assertThat(exception.message, containsString("Could not decode the Arrow IPC response"))
        assertThat(database.openResultCount, equalTo(0))
        assertThat(database.allocatedArrowMemory, equalTo(0L))
    }

    @Test
    fun `GIVEN non-success response THEN exposes bounded error details and retry header`() {
        val errorBody = "x".repeat(70_000)
        mockServerClient
            .`when`(expectedQueryRequest(), Times.once())
            .respond(
                response()
                    .withStatusCode(503)
                    .withHeader("Retry-After", "30")
                    .withBody(errorBody),
            )

        val exception = assertThrows<SiloHttpException> { database.sendQuery(query) }

        assertThat(exception.statusCode, equalTo(503))
        assertThat(exception.responseBody.length, equalTo(64 * 1024))
        assertThat(exception.responseBodyTruncated, `is`(true))
        assertThat(exception.retryAfter, equalTo("30"))
        mockServerClient.verify(expectedQueryRequest(), VerificationTimes.once())
    }

    @Test
    fun `GIVEN unexpected content type THEN reports protocol failure`() {
        mockServerClient
            .`when`(expectedQueryRequest(), Times.once())
            .respond(
                response()
                    .withStatusCode(200)
                    .withHeader("Content-Type", "application/x-ndjson")
                    .withBody("{}"),
            )

        val exception = assertThrows<SiloProtocolException> { database.sendQuery(query) }

        assertThat(exception.message, containsString("application/x-ndjson"))
        assertThat(database.openResultCount, equalTo(0))
    }

    @Test
    fun `GIVEN interrupted thread THEN preserves interruption and reports connection failure`() {
        expectSuccessfulQuery(buildTestArrowStream(listOf(listOf(testRow("only")))))
        Thread.currentThread().interrupt()
        try {
            val exception = assertThrows<SiloConnectionException> { database.sendQuery(query) }
            assertThat(exception.message, containsString("Interrupted while querying SILO"))
            assertThat(Thread.currentThread().isInterrupted, `is`(true))
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun `GIVEN schema response THEN getSchema preserves field order and maps all supported types`() {
        mockServerClient
            .`when`(expectedSchemaRequest(), Times.once())
            .respond(
                response()
                    .withStatusCode(200)
                    .withHeader("Content-Type", SILO_ARROW_STREAM_MEDIA_TYPE)
                    .withBody(
                        buildTextArrowStream(
                            fieldNames = listOf("fieldName", "type"),
                            batches = listOf(
                                listOf(
                                    mapOf("fieldName" to "country", "type" to "INDEXED_STRING"),
                                    mapOf("fieldName" to "date", "type" to "DATE32"),
                                    mapOf("fieldName" to "flag", "type" to "BOOL"),
                                    mapOf("fieldName" to "i32", "type" to "INT32"),
                                    mapOf("fieldName" to "i64", "type" to "INT64"),
                                    mapOf("fieldName" to "score", "type" to "FLOAT"),
                                    mapOf("fieldName" to "note", "type" to "STRING"),
                                ),
                            ),
                        ),
                    ),
            )

        val schema = database.getSchema()

        assertThat(schema.tables.map(SiloTable::name), equalTo(listOf("default")))
        assertThat(
            schema["default"].columns.map(SiloColumn::name),
            equalTo(listOf("country", "date", "flag", "i32", "i64", "score", "note")),
        )
        assertThat(
            schema["default"].columns.map(SiloColumn::type),
            equalTo(
                listOf(
                    SiloColumnType.INDEXED_STRING,
                    SiloColumnType.DATE32,
                    SiloColumnType.BOOL,
                    SiloColumnType.INT32,
                    SiloColumnType.INT64,
                    SiloColumnType.FLOAT,
                    SiloColumnType.STRING,
                ),
            ),
        )
    }

    @Test
    fun `GIVEN malformed schema row THEN getSchema reports the missing fields`() {
        mockServerClient
            .`when`(expectedSchemaRequest(), Times.once())
            .respond(
                response()
                    .withStatusCode(200)
                    .withHeader("Content-Type", SILO_ARROW_STREAM_MEDIA_TYPE)
                    .withBody(
                        buildTextArrowStream(
                            fieldNames = listOf("fieldName"),
                            batches = listOf(listOf(mapOf("fieldName" to "country"))),
                        ),
                    ),
            )

        val exception = assertThrows<SiloProtocolException> { database.getSchema() }

        assertThat(exception.message, containsString("type"))
    }

    @Test
    fun `GIVEN unsupported schema type THEN getSchema reports it`() {
        mockServerClient
            .`when`(expectedSchemaRequest(), Times.once())
            .respond(
                response()
                    .withStatusCode(200)
                    .withHeader("Content-Type", SILO_ARROW_STREAM_MEDIA_TYPE)
                    .withBody(
                        buildTextArrowStream(
                            fieldNames = listOf("fieldName", "type"),
                            batches = listOf(listOf(mapOf("fieldName" to "country", "type" to "UUID"))),
                        ),
                    ),
            )

        val exception = assertThrows<SiloProtocolException> { database.getSchema() }

        assertThat(exception.message, containsString("unsupported column type"))
    }

    @Test
    fun `GIVEN duplicate schema field names THEN getSchema rejects them`() {
        mockServerClient
            .`when`(expectedSchemaRequest(), Times.once())
            .respond(
                response()
                    .withStatusCode(200)
                    .withHeader("Content-Type", SILO_ARROW_STREAM_MEDIA_TYPE)
                    .withBody(
                        buildTextArrowStream(
                            fieldNames = listOf("fieldName", "type"),
                            batches = listOf(
                                listOf(
                                    mapOf("fieldName" to "country", "type" to "STRING"),
                                    mapOf("fieldName" to "country", "type" to "INDEXED_STRING"),
                                ),
                            ),
                        ),
                    ),
            )

        val exception = assertThrows<SiloProtocolException> { database.getSchema() }

        assertThat(exception.message, containsString("duplicate field 'country'"))
    }

    @Test
    fun `GIVEN request headers THEN sendQuery forwards them while keeping protocol headers fixed`() {
        expectSuccessfulQuery(buildTestArrowStream(listOf(listOf(testRow("only")))))

        database.sendQuery(query = query, headers = mapOf("X-Request-Id" to "abc-123")).use { result ->
            assertThat(result.rows.count(), equalTo(1L))
        }

        mockServerClient.verify(
            expectedQueryRequest().withHeader("X-Request-Id", "abc-123"),
            VerificationTimes.once(),
        )
    }

    @Test
    fun `GIVEN reserved request headers THEN sendQuery rejects them locally`() {
        val acceptException = assertThrows<IllegalArgumentException> {
            database.sendQuery(query = query, headers = mapOf("Accept" to "application/json"))
        }
        val contentTypeException = assertThrows<IllegalArgumentException> {
            database.sendQuery(query = query, headers = mapOf("content-type" to "application/json"))
        }

        assertThat(acceptException.message, containsString("Accept"))
        assertThat(contentTypeException.message, containsString("content-type"))
    }

    @Test
    fun `GIVEN transient transport failure THEN database retries exactly once`() {
        val server = ServerSocket(0)
        val retryDatabase = Database("http://127.0.0.1:${server.localPort}/silo")
        val requestCount = AtomicInteger()
        val body = buildTestArrowStream(listOf(listOf(testRow("retried"))))
        val responder = thread(start = true, name = "retry-test-server") {
            repeat(2) { attempt ->
                server.accept().use { socket ->
                    requestCount.incrementAndGet()
                    readHttpRequest(socket)
                    if (attempt == 0) {
                        return@use
                    }
                    writeArrowResponse(socket, body)
                }
            }
        }

        try {
            retryDatabase.sendQuery(query).use { result ->
                assertThat(result.rows.toList().single().getString("text"), equalTo("retried"))
            }
            responder.join(5_000)
            assertThat(responder.isAlive, `is`(false))
            assertThat(requestCount.get(), equalTo(2))
        } finally {
            retryDatabase.close()
            server.close()
        }
    }

    @Test
    fun `GIVEN invalid base URL THEN rejects it during construction`() {
        assertThrows<IllegalArgumentException> { Database(URI("ftp://example.com/silo")) }
        assertThrows<IllegalArgumentException> { Database(URI("https://example.com/silo?token=value")) }
        assertThrows<IllegalArgumentException> { Database(URI("https://example.com/silo#fragment")) }
    }

    @Test
    fun `GIVEN invalid thread count THEN rejects database options`() {
        val exception = assertThrows<IllegalArgumentException> { DatabaseOptions(httpThreadCount = 0) }

        assertThat(exception.message, containsString("positive"))
    }

    private fun expectSuccessfulQuery(body: ByteArray) {
        mockServerClient
            .`when`(expectedQueryRequest(), Times.once())
            .respond(
                response()
                    .withStatusCode(200)
                    .withHeader("Content-Type", SILO_ARROW_STREAM_MEDIA_TYPE)
                    .withHeader("data-version", DATA_VERSION)
                    .withHeader("X-Test-Header", "test-header-value")
                    .withBody(body),
            )
    }

    private fun expectedQueryRequest() =
        request()
            .withMethod("POST")
            .withPath("/silo/query")
            .withContentType(MediaType.TEXT_PLAIN)
            .withHeader("Accept", SILO_ARROW_STREAM_MEDIA_TYPE)
            .withBody(query.render())

    private fun expectedSchemaRequest() =
        request()
            .withMethod("POST")
            .withPath("/silo/query")
            .withContentType(MediaType.TEXT_PLAIN)
            .withHeader("Accept", SILO_ARROW_STREAM_MEDIA_TYPE)
            .withBody("default.schema()")

    private fun testRow(
        text: String,
        optional: String? = null,
    ) = TestArrowRow(
        text = text,
        int32 = 12,
        int64 = 34,
        float32 = 1.5f,
        float64 = 2.5,
        flag = true,
        date = LocalDate.of(2024, 2, 3),
        optional = optional,
    )
}

private fun readHttpRequest(socket: Socket) {
    socket.soTimeout = 5_000
    val input = socket.getInputStream()
    val headerBytes = ByteArrayOutputStream()
    while (!headerBytes.endsWithHeaderTerminator()) {
        val nextByte = input.read()
        if (nextByte < 0) {
            return
        }
        headerBytes.write(nextByte)
    }
    val headerText = headerBytes.toString(StandardCharsets.US_ASCII)
    val contentLength = headerText.lineSequence()
        .firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.trim()
        ?.toInt()
        ?: 0
    repeat(contentLength) {
        if (input.read() < 0) {
            return
        }
    }
}

private fun ByteArrayOutputStream.endsWithHeaderTerminator(): Boolean {
    val bytes = toByteArray()
    return bytes.size >= 4 &&
        bytes[bytes.size - 4] == '\r'.code.toByte() &&
        bytes[bytes.size - 3] == '\n'.code.toByte() &&
        bytes[bytes.size - 2] == '\r'.code.toByte() &&
        bytes[bytes.size - 1] == '\n'.code.toByte()
}

private fun writeArrowResponse(
    socket: Socket,
    body: ByteArray,
) {
    socket.getOutputStream().use { output ->
        output.write(
            (
                "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: $SILO_ARROW_STREAM_MEDIA_TYPE\r\n" +
                    "Content-Length: ${body.size}\r\n" +
                    "data-version: $DATA_VERSION\r\n" +
                    "\r\n"
            ).toByteArray(StandardCharsets.US_ASCII),
        )
        output.write(body)
        output.flush()
    }
}
