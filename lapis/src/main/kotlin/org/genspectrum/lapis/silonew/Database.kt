package org.genspectrum.lapis.silonew

import org.apache.arrow.memory.BufferAllocator
import org.apache.arrow.memory.RootAllocator
import org.apache.arrow.vector.ipc.ArrowStreamReader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.Spliterator
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.function.Consumer
import java.util.stream.Stream
import java.util.stream.StreamSupport
import kotlin.concurrent.read
import kotlin.concurrent.write

const val SILO_ARROW_STREAM_MEDIA_TYPE = "application/vnd.apache.arrow.stream"

private const val MAX_ERROR_BODY_BYTES = 64 * 1024
private const val DEFAULT_TABLE_NAME = "default"
private const val SCHEMA_FIELD_NAME = "fieldName"
private const val SCHEMA_TYPE_FIELD_NAME = "type"

data class DatabaseOptions(
    val httpThreadCount: Int = 64,
) {
    init {
        require(httpThreadCount > 0) { "HTTP thread count must be positive" }
    }
}

/**
 * A reusable client for a SILO database.
 *
 * [destinationUrl] is the SILO base URL; queries are sent to its `/query` endpoint. The database owns its HTTP and
 * Arrow resources and closes any outstanding [QueryResult] instances when it is closed.
 *
 * ```kotlin
 * Database("https://silo.example").use { database ->
 *     database.sendQuery(query).use { result ->
 *         println(result.dataVersion)
 *         result.rows.forEach { row -> println(row["country"]) }
 *     }
 * }
 * ```
 *
 * Formatted results can be written directly without materializing the complete response:
 * ```kotlin
 * database.sendQuery(query, SiloDataFormat.Tsv()).use { result ->
 *     result.writeTo(System.out)
 * }
 * ```
 *
 * Apache Arrow requires the JVM option `--add-opens=java.base/java.nio=ALL-UNNAMED` when using its Netty allocator.
 */
class Database private constructor(
    private val state: DatabaseState,
) : AutoCloseable {
    constructor(
        destinationUrl: String,
        options: DatabaseOptions = DatabaseOptions(),
    ) : this(createDatabaseState(destinationUrl = URI.create(destinationUrl), options = options))

    constructor(
        destinationUrl: URI,
        options: DatabaseOptions = DatabaseOptions(),
    ) : this(createDatabaseState(destinationUrl = destinationUrl, options = options))

    val destinationUrl: URI = state.destinationUrl

    private val lifecycleLock = ReentrantReadWriteLock()
    private val closed = AtomicBoolean(false)
    private val queryCounter = AtomicLong()
    private val openResults = ConcurrentHashMap.newKeySet<ArrowQueryResources>()

    /** Sends [query] and lazily exposes each result row as a detached [SiloRow]. */
    fun sendQuery(
        query: SiloRelation,
        headers: Map<String, String> = emptyMap(),
    ): QueryResult = createQueryResult(openResponse(query = query, headers = headers))

    /** Sends [query] and lazily transforms its Arrow response into [format]. */
    fun sendQuery(
        query: SiloRelation,
        format: SiloDataFormat,
        headers: Map<String, String> = emptyMap(),
    ): FormattedQueryResult =
        createFormattedQueryResult(
            response = openResponse(query = query, headers = headers),
            format = format,
        )

    private fun openResponse(
        query: SiloRelation,
        headers: Map<String, String>,
    ): ArrowResponse =
        lifecycleLock.read {
            ensureOpen()
            val response = send(query = query, headers = headers)
            handleErrorResponse(response)
            validateContentType(response)
            openArrowResponse(response)
        }

    internal val openResultCount: Int
        get() = openResults.size

    internal val allocatedArrowMemory: Long
        get() = state.rootAllocator.allocatedMemory

    override fun close(): Unit =
        lifecycleLock.write {
            if (!closed.compareAndSet(false, true)) {
                return@write
            }

            closeAll(
                openResults.toList() +
                    listOf(
                        AutoCloseable(state.httpClient::close),
                        AutoCloseable(state.executor::shutdownNow),
                        state.rootAllocator,
                    ),
            ) { failure -> SiloConnectionException("Could not close SILO database resources", failure) }
        }

    private fun ensureOpen() {
        if (closed.get()) {
            throw SiloConnectionException("The SILO database is closed")
        }
    }

    /** Fetches the ordered schema reported by `default.schema()`. */
    fun getSchema(headers: Map<String, String> = emptyMap()): SiloSchema {
        val defaultTable = SiloTable(name = DEFAULT_TABLE_NAME, columns = emptyList())
        val columns = sendQuery(query = defaultTable.schema(), headers = headers).use { result ->
            result.rows.map(::parseSchemaColumn).toList()
        }
        val duplicateColumn = columns.groupingBy { (name, _) -> name }
            .eachCount()
            .entries
            .firstOrNull { it.value > 1 }
            ?.key
        if (duplicateColumn != null) {
            throw SiloProtocolException("SILO schema contains duplicate field '$duplicateColumn'")
        }
        return SiloSchema(listOf(SiloTable(name = DEFAULT_TABLE_NAME, columns = columns)))
    }

    private fun send(
        query: SiloRelation,
        headers: Map<String, String>,
    ): HttpResponse<InputStream> {
        validateRequestHeaders(headers)
        val requestBuilder = HttpRequest.newBuilder(state.queryUrl)
            .header("Content-Type", "text/plain")
            .header("Accept", SILO_ARROW_STREAM_MEDIA_TYPE)
        headers.forEach { (name, value) -> requestBuilder.header(name, value) }
        val request = requestBuilder
            .POST(HttpRequest.BodyPublishers.ofString(query.render(), StandardCharsets.UTF_8))
            .build()

        repeat(2) { attempt ->
            try {
                return state.httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
            } catch (exception: InterruptedException) {
                Thread.currentThread().interrupt()
                throw SiloConnectionException("Interrupted while querying SILO at ${state.queryUrl}", exception)
            } catch (exception: IOException) {
                if (attempt == 1) {
                    throw SiloConnectionException("Could not query SILO at ${state.queryUrl}", exception)
                }
            }
        }
        error("Unreachable")
    }

    private fun handleErrorResponse(response: HttpResponse<InputStream>) {
        if (response.statusCode() == 200) {
            return
        }

        val errorBody = try {
            response.body().use(::readBoundedErrorBody)
        } catch (exception: IOException) {
            throw SiloConnectionException("Could not read SILO error response", exception)
        }

        throw SiloHttpException(
            statusCode = response.statusCode(),
            responseBody = errorBody.body,
            responseBodyTruncated = errorBody.truncated,
            retryAfter = response.headers().firstValue("retry-after").orElse(null),
        )
    }

    private fun validateContentType(response: HttpResponse<InputStream>) {
        val contentType = response.headers().firstValue("content-type").orElse(null) ?: return
        if (!contentType.substringBefore(';').trim().equals(SILO_ARROW_STREAM_MEDIA_TYPE, ignoreCase = true)) {
            val exception = SiloProtocolException(
                "Expected SILO to return '$SILO_ARROW_STREAM_MEDIA_TYPE' but received '$contentType'",
            )
            closePreserving(response.body(), exception)
            throw exception
        }
    }

    private fun openArrowResponse(response: HttpResponse<InputStream>): ArrowResponse {
        val allocator = state.rootAllocator.newChildAllocator(
            "query-${queryCounter.incrementAndGet()}",
            0,
            Long.MAX_VALUE,
        )
        val inputStream = response.body()
        val reader = try {
            ArrowStreamReader(inputStream, allocator)
        } catch (exception: Exception) {
            val decodingException = SiloResultDecodingException(
                "Could not open the Arrow IPC response from SILO",
                exception,
            )
            closePreserving(inputStream, decodingException)
            closePreserving(allocator, decodingException)
            throw decodingException
        }

        val resources = ArrowQueryResources(
            inputStream = inputStream,
            reader = reader,
            allocator = allocator,
            onClose = openResults::remove,
        )
        openResults.add(resources)

        return ArrowResponse(
            resources = resources,
            dataVersion = response.headers().firstValue("data-version").orElse(null),
            headers = immutableHeaders(response),
        )
    }

    private fun createQueryResult(response: ArrowResponse): QueryResult {
        val spliterator = ArrowRowSpliterator(response.resources)
        val rows = StreamSupport.stream(spliterator, false).onClose(response.resources::close)

        return QueryResult(
            rows = rows,
            dataVersion = response.dataVersion,
            headers = response.headers,
        )
    }

    private fun createFormattedQueryResult(
        response: ArrowResponse,
        format: SiloDataFormat,
    ) = FormattedQueryResult(
        resources = response.resources,
        format = format,
        dataVersion = response.dataVersion,
        headers = response.headers,
    )
}

/**
 * A streaming SILO response and its metadata.
 *
 * Close the result after short-circuiting the stream. Full exhaustion and failures close it automatically.
 */
class QueryResult internal constructor(
    val rows: Stream<SiloRow>,
    val dataVersion: String?,
    headers: Map<String, List<String>>,
) : AutoCloseable {
    val headers: Map<String, List<String>> = Collections.unmodifiableMap(
        headers.mapValues { (_, values) -> Collections.unmodifiableList(values.toList()) },
    )

    override fun close() = rows.close()
}

sealed class SiloClientException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class SiloConnectionException(
    message: String,
    cause: Throwable? = null,
) : SiloClientException(message, cause)

class SiloHttpException(
    val statusCode: Int,
    val responseBody: String,
    val responseBodyTruncated: Boolean,
    val retryAfter: String?,
) : SiloClientException(
        buildString {
            append("SILO query failed with HTTP $statusCode")
            if (responseBody.isNotBlank()) {
                append(": $responseBody")
                if (responseBodyTruncated) append("…")
            }
        },
    )

class SiloProtocolException(
    message: String,
    cause: Throwable? = null,
) : SiloClientException(message, cause)

class SiloResultDecodingException(
    message: String,
    cause: Throwable? = null,
) : SiloClientException(message, cause)

class SiloResultEncodingException(
    message: String,
    cause: Throwable? = null,
) : SiloClientException(message, cause)

private data class DatabaseState(
    val destinationUrl: URI,
    val queryUrl: URI,
    val executor: ExecutorService,
    val httpClient: HttpClient,
    val rootAllocator: RootAllocator,
)

private data class ArrowResponse(
    val resources: ArrowQueryResources,
    val dataVersion: String?,
    val headers: Map<String, List<String>>,
)

private fun parseSchemaColumn(row: SiloRow): Pair<String, SiloColumnType> {
    val fieldName = row.values[SCHEMA_FIELD_NAME] as? String
        ?: throw SiloProtocolException("SILO schema row has no string '$SCHEMA_FIELD_NAME' field: $row")
    val rawType = row.values[SCHEMA_TYPE_FIELD_NAME] as? String
        ?: throw SiloProtocolException("SILO schema row has no string '$SCHEMA_TYPE_FIELD_NAME' field: $row")
    val type = try {
        SiloColumnType.valueOf(rawType)
    } catch (exception: IllegalArgumentException) {
        throw SiloProtocolException("SILO schema row contains unsupported column type '$rawType'", exception)
    }
    return fieldName to type
}

private fun validateRequestHeaders(headers: Map<String, String>) {
    headers.keys.firstOrNull { name ->
        name.equals("Accept", ignoreCase = true) || name.equals("Content-Type", ignoreCase = true)
    }?.let { name ->
        throw IllegalArgumentException("Request header '$name' is controlled by the SILO protocol")
    }
}

private fun createDatabaseState(
    destinationUrl: URI,
    options: DatabaseOptions,
): DatabaseState {
    val normalizedDestinationUrl = validateAndNormalizeDestinationUrl(destinationUrl)
    val executor = Executors.newFixedThreadPool(options.httpThreadCount, SiloHttpThreadFactory())
    return try {
        DatabaseState(
            destinationUrl = normalizedDestinationUrl,
            queryUrl = URI.create("${normalizedDestinationUrl.toString().trimEnd('/')}/query"),
            executor = executor,
            httpClient = HttpClient.newBuilder().executor(executor).build(),
            rootAllocator = RootAllocator(),
        )
    } catch (exception: Exception) {
        executor.shutdownNow()
        throw exception
    }
}

private fun validateAndNormalizeDestinationUrl(destinationUrl: URI): URI {
    require(destinationUrl.scheme.equals("http", ignoreCase = true) || destinationUrl.scheme.equals("https", true)) {
        "SILO destination URL must use http or https"
    }
    require(destinationUrl.host != null) { "SILO destination URL must include a host" }
    require(destinationUrl.rawQuery == null) { "SILO destination URL must not include a query string" }
    require(destinationUrl.rawFragment == null) { "SILO destination URL must not include a fragment" }
    return URI.create(destinationUrl.toString().trimEnd('/'))
}

private class SiloHttpThreadFactory : java.util.concurrent.ThreadFactory {
    private val counter = AtomicInteger()

    override fun newThread(runnable: Runnable) =
        Thread(runnable, "silo-database-${counter.incrementAndGet()}").apply {
            isDaemon = true
        }
}

internal class ArrowQueryResources(
    val inputStream: InputStream,
    val reader: ArrowStreamReader,
    val allocator: BufferAllocator,
    private val onClose: (ArrowQueryResources) -> Unit,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)

    override fun close() {
        if (!closed.compareAndSet(false, true)) {
            return
        }

        try {
            closeAll(
                listOf(reader, inputStream, allocator),
            ) { failure -> SiloResultDecodingException("Could not close Arrow query resources", failure) }
        } finally {
            onClose(this)
        }
    }
}

private class ArrowRowSpliterator(
    private val resources: ArrowQueryResources,
    private val mapper: SiloRowMapper = SiloRowMapper(),
) : Spliterator<SiloRow> {
    private var rowIndex = 0

    override fun tryAdvance(action: Consumer<in SiloRow>): Boolean {
        val value = try {
            val root = resources.reader.vectorSchemaRoot
            while (rowIndex >= root.rowCount) {
                if (!resources.reader.loadNextBatch()) {
                    resources.close()
                    return false
                }
                rowIndex = 0
            }
            mapper.map(root, rowIndex++)
        } catch (exception: Throwable) {
            closePreserving(resources, exception)
            if (exception is Error) {
                throw exception
            }
            if (exception is SiloResultDecodingException) {
                throw exception
            }
            throw SiloResultDecodingException("Could not decode the Arrow IPC response from SILO", exception)
        }

        try {
            action.accept(value)
        } catch (exception: Throwable) {
            closePreserving(resources, exception)
            throw exception
        }
        return true
    }

    override fun trySplit(): Spliterator<SiloRow>? = null

    override fun estimateSize() = Long.MAX_VALUE

    override fun characteristics() = Spliterator.ORDERED

    override fun getComparator(): Comparator<in SiloRow>? =
        throw IllegalStateException("This spliterator is not sorted")
}

private data class BoundedErrorBody(
    val body: String,
    val truncated: Boolean,
)

private fun readBoundedErrorBody(inputStream: InputStream): BoundedErrorBody {
    val output = ByteArrayOutputStream(MAX_ERROR_BODY_BYTES)
    val buffer = ByteArray(8192)
    var remaining = MAX_ERROR_BODY_BYTES
    while (remaining > 0) {
        val read = inputStream.read(buffer, 0, minOf(buffer.size, remaining))
        if (read < 0) {
            return BoundedErrorBody(output.toString(StandardCharsets.UTF_8), truncated = false)
        }
        output.write(buffer, 0, read)
        remaining -= read
    }
    return BoundedErrorBody(
        body = output.toString(StandardCharsets.UTF_8),
        truncated = inputStream.read() >= 0,
    )
}

private fun immutableHeaders(response: HttpResponse<*>): Map<String, List<String>> =
    response.headers().map().mapValues { (_, values) -> values.toList() }.toMap()

private fun closeAll(
    closeables: Iterable<AutoCloseable>,
    toException: (Throwable) -> SiloClientException,
) {
    var failure: Throwable? = null
    closeables.forEach { closeable ->
        try {
            closeable.close()
        } catch (exception: Throwable) {
            if (failure == null) {
                failure = exception
            } else {
                failure.addSuppressed(exception)
            }
        }
    }
    failure?.let { throw toException(it) }
}

private fun closePreserving(
    closeable: AutoCloseable,
    originalFailure: Throwable,
) {
    try {
        closeable.close()
    } catch (closeFailure: Throwable) {
        originalFailure.addSuppressed(closeFailure)
    }
}
