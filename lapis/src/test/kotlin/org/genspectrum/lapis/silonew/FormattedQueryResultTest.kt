package org.genspectrum.lapis.silonew

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockserver.client.MockServerClient
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.stream.Stream

private const val FORMATTED_DATA_VERSION = "test-data-version"

class FormattedQueryResultTest {
    private lateinit var mockServer: ClientAndServer
    private lateinit var mockServerClient: MockServerClient
    private lateinit var database: Database
    private val defaultTable = siloSchema {
        table("default") {
            column("text", SiloColumnType.STRING)
        }
    }.table("default")
    private val query = defaultTable.limit(2)

    @BeforeEach
    fun setUp() {
        mockServer = ClientAndServer.startClientAndServer(0)
        mockServerClient = MockServerClient("127.0.0.1", mockServer.localPort)
        database = Database("http://127.0.0.1:${mockServer.localPort}/silo")
    }

    @AfterEach
    fun tearDown() {
        database.close()
        mockServer.stop()
    }

    @Test
    fun `GIVEN multiple Arrow batches THEN writes JSON array directly`() {
        expectSuccessfulQuery(
            buildTestArrowStream(
                listOf(
                    listOf(testRow("first")),
                    listOf(testRow("second", optional = "value")),
                ),
            ),
        )
        val output = CloseTrackingOutputStream()
        val result = database.sendQuery(query, SiloDataFormat.Json)

        result.use { it.writeTo(output) }

        assertThat(
            output.toString(Charsets.UTF_8),
            equalTo(
                "[" +
                    "{\"text\":\"first\",\"int32\":12,\"int64\":34,\"float32\":1.5," +
                    "\"float64\":2.5,\"flag\":true,\"date\":\"2024-02-03\",\"optional\":null}," +
                    "{\"text\":\"second\",\"int32\":12,\"int64\":34,\"float32\":1.5," +
                    "\"float64\":2.5,\"flag\":true,\"date\":\"2024-02-03\",\"optional\":\"value\"}" +
                    "]",
            ),
        )
        assertThat(output.closed, `is`(false))
        assertThat(result.contentType, equalTo("application/json"))
        assertThat(result.dataVersion, equalTo(FORMATTED_DATA_VERSION))
        assertThrows<UnsupportedOperationException> {
            (result.headers as MutableMap)["another-header"] = emptyList()
        }
        assertThat(database.openResultCount, equalTo(0))
        assertThat(database.allocatedArrowMemory, equalTo(0L))
        assertThrows<IllegalStateException> { result.writeTo(ByteArrayOutputStream()) }
    }

    @Test
    fun `GIVEN Arrow rows THEN writes NDJSON`() {
        expectSuccessfulQuery(
            buildTextArrowStream(
                fieldNames = listOf("name", "value"),
                batches = listOf(
                    listOf(mapOf("name" to "first", "value" to null)),
                    listOf(mapOf("name" to "second", "value" to "x")),
                ),
            ),
        )
        val result = database.sendQuery(query, SiloDataFormat.Ndjson)

        val output = ByteArrayOutputStream()
        result.use { it.writeTo(output) }

        assertThat(
            output.toString(Charsets.UTF_8),
            equalTo("{\"name\":\"first\",\"value\":null}\n{\"name\":\"second\",\"value\":\"x\"}\n"),
        )
        assertThat(result.contentType, equalTo("application/x-ndjson;charset=UTF-8"))
        assertThat(database.openResultCount, equalTo(0))
        assertThat(database.allocatedArrowMemory, equalTo(0L))
    }

    @ParameterizedTest
    @MethodSource("delimitedFormats")
    fun `GIVEN special values THEN writes LAPIS delimited formats`(
        format: SiloDataFormat,
        expected: String,
        expectedContentType: String,
    ) {
        expectSuccessfulQuery(delimitedArrowStream())

        val result = database.sendQuery(query, format)
        val output = ByteArrayOutputStream()
        result.use { it.writeTo(output) }

        assertThat(output.toString(Charsets.UTF_8), equalTo(expected))
        assertThat(result.contentType, equalTo(expectedContentType))
        assertThat(database.allocatedArrowMemory, equalTo(0L))
    }

    @Test
    fun `GIVEN empty result THEN delimited format still writes its header`() {
        expectSuccessfulQuery(buildTextArrowStream(listOf("first", "second"), emptyList()))

        val result = database.sendQuery(query, SiloDataFormat.Csv())
        val output = ByteArrayOutputStream()
        result.use { it.writeTo(output) }

        assertThat(output.toString(Charsets.UTF_8), equalTo("first,second\n"))
        assertThat(database.allocatedArrowMemory, equalTo(0L))
    }

    @Test
    fun `GIVEN sequence fields and header template THEN writes FASTA records`() {
        expectSuccessfulQuery(
            buildTextArrowStream(
                fieldNames = listOf("primaryKey", "main", "S"),
                batches = listOf(
                    listOf(
                        mapOf("primaryKey" to "id-1", "main" to "ACGT", "S" to null),
                        mapOf("primaryKey" to "id-2", "main" to null, "S" to "MPEP"),
                    ),
                ),
            ),
        )
        val format = SiloDataFormat.Fasta(
            sequenceFields = listOf(field("main"), field("S")),
            headerTemplate = "{PRIMARYKEY}|{.segment}",
        )

        val result = database.sendQuery(query, format)
        val output = ByteArrayOutputStream()
        result.use { it.writeTo(output) }

        assertThat(output.toString(Charsets.UTF_8), equalTo(">id-1|main\nACGT\n>id-2|S\nMPEP\n"))
        assertThat(result.contentType, equalTo("text/x-fasta;charset=UTF-8"))
    }

    @Test
    fun `GIVEN invalid FASTA field THEN fails and releases resources`() {
        expectSuccessfulQuery(buildTextArrowStream(listOf("main"), listOf(listOf(mapOf("main" to "ACGT")))))
        val result = database.sendQuery(
            query,
            SiloDataFormat.Fasta(listOf(field("main")), headerTemplate = "{missing}"),
        )

        val exception = assertThrows<SiloResultEncodingException> {
            result.use { it.writeTo(ByteArrayOutputStream()) }
        }

        assertThat(exception.message, containsString("missing"))
        assertThat(database.openResultCount, equalTo(0))
        assertThat(database.allocatedArrowMemory, equalTo(0L))
    }

    @Test
    fun `GIVEN non-string FASTA sequence field THEN fails and releases resources`() {
        expectSuccessfulQuery(buildTestArrowStream(listOf(listOf(testRow("value")))))
        val result = database.sendQuery(
            query,
            SiloDataFormat.Fasta(listOf(field("int32")), headerTemplate = "sequence"),
        )

        val exception = assertThrows<SiloResultEncodingException> {
            result.use { it.writeTo(ByteArrayOutputStream()) }
        }

        assertThat(exception.message, containsString("must contain strings"))
        assertThat(database.openResultCount, equalTo(0))
        assertThat(database.allocatedArrowMemory, equalTo(0L))
    }

    @Test
    fun `GIVEN one Newick value THEN writes it without additional framing`() {
        expectSuccessfulQuery(
            buildTextArrowStream(
                listOf("subtreeNewick"),
                listOf(listOf(mapOf("subtreeNewick" to "(A:1,B:1);"))),
            ),
        )
        val result = database.sendQuery(query, SiloDataFormat.Newick(field("subtreeNewick")))
        val output = ByteArrayOutputStream()

        result.use { it.writeTo(output) }

        assertThat(output.toString(Charsets.UTF_8), equalTo("(A:1,B:1);"))
        assertThat(result.contentType, equalTo("text/x-nh;charset=UTF-8"))
    }

    @Test
    fun `GIVEN invalid Newick cardinality THEN fails and releases resources`() {
        expectSuccessfulQuery(
            buildTextArrowStream(
                listOf("tree"),
                listOf(listOf(mapOf("tree" to "(A);"), mapOf("tree" to "(B);"))),
            ),
        )
        val result = database.sendQuery(query, SiloDataFormat.Newick(field("tree")))

        val exception = assertThrows<SiloResultEncodingException> {
            result.use { it.writeTo(ByteArrayOutputStream()) }
        }

        assertThat(exception.message, containsString("exactly one"))
        assertThat(database.openResultCount, equalTo(0))
        assertThat(database.allocatedArrowMemory, equalTo(0L))
    }

    @Test
    fun `GIVEN empty Newick result THEN fails and releases resources`() {
        expectSuccessfulQuery(buildTextArrowStream(listOf("tree"), emptyList()))
        val result = database.sendQuery(query, SiloDataFormat.Newick(field("tree")))

        val exception = assertThrows<SiloResultEncodingException> {
            result.use { it.writeTo(ByteArrayOutputStream()) }
        }

        assertThat(exception.message, containsString("exactly one"))
        assertThat(database.openResultCount, equalTo(0))
        assertThat(database.allocatedArrowMemory, equalTo(0L))
    }

    @Test
    fun `GIVEN null Newick value THEN fails and releases resources`() {
        expectSuccessfulQuery(buildTextArrowStream(listOf("tree"), listOf(listOf(mapOf("tree" to null)))))
        val result = database.sendQuery(query, SiloDataFormat.Newick(field("tree")))

        val exception = assertThrows<SiloResultEncodingException> {
            result.use { it.writeTo(ByteArrayOutputStream()) }
        }

        assertThat(exception.message, containsString("exactly one"))
        assertThat(database.openResultCount, equalTo(0))
        assertThat(database.allocatedArrowMemory, equalTo(0L))
    }

    @Test
    fun `GIVEN encoding failure THEN write reports it and releases resources`() {
        expectSuccessfulQuery(buildUnsupportedArrowStream())
        val result = database.sendQuery(query, SiloDataFormat.Json)

        val exception = assertThrows<SiloResultEncodingException> {
            result.use { it.writeTo(ByteArrayOutputStream()) }
        }

        assertThat(exception.message, containsString("VarBinaryVector"))
        assertThat(database.openResultCount, equalTo(0))
        assertThat(database.allocatedArrowMemory, equalTo(0L))
    }

    @Test
    fun `GIVEN outstanding formatted result WHEN database closes THEN result resources close`() {
        expectSuccessfulQuery(buildTextArrowStream(listOf("value"), emptyList()))
        database.sendQuery(query, SiloDataFormat.Json)
        assertThat(database.openResultCount, equalTo(1))

        database.close()

        assertThat(database.openResultCount, equalTo(0))
        assertThat(database.allocatedArrowMemory, equalTo(0L))
    }

    private fun expectSuccessfulQuery(body: ByteArray) {
        mockServerClient
            .`when`(expectedQueryRequest(), Times.once())
            .respond(
                response()
                    .withStatusCode(200)
                    .withHeader("Content-Type", SILO_ARROW_STREAM_MEDIA_TYPE)
                    .withHeader("data-version", FORMATTED_DATA_VERSION)
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

    private fun delimitedArrowStream() =
        buildTextArrowStream(
            fieldNames = listOf("a", "b"),
            batches = listOf(
                listOf(
                    mapOf("a" to "comma,value", "b" to "line1\nline2"),
                    mapOf("a" to "tab\tvalue", "b" to null),
                ),
            ),
        )

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

    private class CloseTrackingOutputStream : ByteArrayOutputStream() {
        var closed = false

        override fun close() {
            closed = true
            super.close()
        }
    }

    private companion object {
        @JvmStatic
        fun delimitedFormats(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    SiloDataFormat.Csv(),
                    "a,b\n\"comma,value\",\"line1\nline2\"\ntab\tvalue,\n",
                    "text/csv;charset=UTF-8",
                ),
                Arguments.of(
                    SiloDataFormat.Csv(includeHeader = false),
                    "\"comma,value\",\"line1\nline2\"\ntab\tvalue,\n",
                    "text/plain",
                ),
                Arguments.of(
                    SiloDataFormat.Tsv(),
                    "a\tb\ncomma,value\t\"line1\nline2\"\n\"tab\tvalue\"\t\n",
                    "text/tab-separated-values;charset=UTF-8",
                ),
                Arguments.of(
                    SiloDataFormat.Tsv(escapeSpecialCharacters = true),
                    "a\tb\ncomma,value\tline1\\nline2\ntab\\tvalue\t\n",
                    "text/tab-separated-values;charset=UTF-8",
                ),
            )
    }
}
