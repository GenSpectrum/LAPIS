package org.genspectrum.lapis.silo

import com.fasterxml.jackson.annotation.JsonInclude
import org.apache.arrow.memory.RootAllocator
import org.apache.arrow.vector.ipc.ArrowStreamReader
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.controller.LapisHeaders.REQUEST_ID
import org.genspectrum.lapis.log
import org.genspectrum.lapis.logging.RequestContext
import org.genspectrum.lapis.logging.RequestIdContext
import org.genspectrum.lapis.response.InfoData
import org.genspectrum.lapis.util.YamlObjectMapper
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.PortUnreachableException
import java.net.URI
import java.net.UnknownHostException
import java.net.http.HttpClient
import java.net.http.HttpConnectTimeoutException
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.HttpTimeoutException
import java.time.Duration
import java.util.concurrent.Executors
import java.util.stream.Stream
import java.util.stream.StreamSupport

@Component
class RhyDbClient(
    private val cachedRhyDbClient: CachedRhyDbClient,
    private val dataVersion: DataVersion,
    private val requestContext: RequestContext,
) {
    fun <ResponseType> sendQuery(
        query: RhyDbQuery<ResponseType>,
        setRequestDataVersion: Boolean = true,
    ): Stream<ResponseType> = sendQueryAndGetDataVersion(query, setRequestDataVersion).queryResult

    fun <ResponseType> sendQueryAndGetDataVersion(
        query: RhyDbQuery<ResponseType>,
        setRequestDataVersion: Boolean = true,
    ): WithDataVersion<Stream<ResponseType>> {
        val response = when (query.action.cacheable) {
            true -> cachedRhyDbClient.sendCachedQuery(query).map { it.stream() }
            else -> cachedRhyDbClient.sendQuery(query)
        }

        if (setRequestDataVersion) {
            dataVersion.dataVersion = response.dataVersion
        }

        if (RequestContextHolder.getRequestAttributes() != null && requestContext.cached == null) {
            requestContext.cached = true
        }

        return response
    }

    /**
     * returns the info object and sets the dataVersion.dataVersion.
     */
    fun callInfo(timeout: Duration? = null): InfoData {
        log.info { "Calling SILO info" }

        val info = cachedRhyDbClient.callInfo(timeout)
        dataVersion.dataVersion = info.dataVersion
        return info
    }

    fun getLineageDefinition(column: String): LineageDefinition {
        log.info { "Calling SILO lineageDefinition for column '$column'" }

        return cachedRhyDbClient.getLineageDefinition(column)
    }
}

const val RHYDB_QUERY_CACHE_NAME = "siloQueryCache"
const val ARROW_STREAM_MEDIA_TYPE = "application/vnd.apache.arrow.stream"

@Component
open class CachedRhyDbClient(
    private val rhyDbUris: RhyDbUris,
    private val objectMapper: ObjectMapper,
    private val yamlObjectMapper: YamlObjectMapper,
    private val requestIdContext: RequestIdContext,
    private val requestContext: RequestContext,
    private val config: DatabaseConfig,
    private val rootAllocator: RootAllocator,
) {
    private val httpClient = HttpClient.newBuilder()
        // Create our own thread pool explicitly to not use the ForkJoinPool.commonPool()
        // Use fixed pool with unbounded queue to prevent RejectedExecutionExeceptions
        .executor(Executors.newFixedThreadPool(config.rhydbClientThreadCount))
        .build()

    @Cacheable(
        RHYDB_QUERY_CACHE_NAME,
        condition =
            "#query.action.cacheable && " +
                "(#query.action.randomize == null || " +
                "#query.action.randomize.class.simpleName == 'Disabled' || " +
                "#query.action.randomize.class.simpleName == 'WithSeed')",
    )
    open fun <ResponseType> sendCachedQuery(query: RhyDbQuery<ResponseType>): WithDataVersion<List<ResponseType>> =
        sendQuery(query)
            .let { WithDataVersion(it.dataVersion, it.queryResult.use { stream -> stream.toList() }) }

    fun <ResponseType> sendQuery(query: RhyDbQuery<ResponseType>): WithDataVersion<Stream<ResponseType>> {
        if (RequestContextHolder.getRequestAttributes() != null) {
            requestContext.cached = false
        }

        val saneQlQuery = query.toSaneQl()

        log.info { "Calling SILO: $saneQlQuery" }

        val response = send(
            uri = rhyDbUris.query,
            bodyHandler = BodyHandlers.ofInputStream(),
            tryToReadRhyDbErrorFromBody = { body ->
                body.use {
                    tryToReadRhyDbErrorFromString(it.readBytes().toString(Charsets.UTF_8))
                }
            },
        ) {
            it.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                .header(HttpHeaders.ACCEPT, ARROW_STREAM_MEDIA_TYPE)
                .POST(HttpRequest.BodyPublishers.ofString(saneQlQuery))
        }

        return WithDataVersion(
            queryResult = parseArrowStream(response.body(), query.action),
            dataVersion = getDataVersion(response),
        )
    }

    private fun <ResponseType> parseArrowStream(
        inputStream: InputStream,
        action: RhyDbAction<ResponseType>,
    ): Stream<ResponseType> {
        val allocator = rootAllocator.newChildAllocator("query-${action.javaClass.simpleName}", 0, Long.MAX_VALUE)
        val reader = ArrowStreamReader(inputStream, allocator)

        val rowSequence = sequence {
            try {
                while (reader.loadNextBatch()) {
                    val root = reader.vectorSchemaRoot
                    for (rowIndex in 0 until root.rowCount) {
                        // make sure that this is lazy and doesn't load the whole response into memory at once
                        yield(action.arrowConverter(root, rowIndex))
                    }
                }
            } catch (exception: Exception) {
                val message = "Could not parse Arrow IPC response from SILO: " +
                    exception::class.toString() + " " + exception.message
                throw RuntimeException(message, exception)
            }
        }

        return StreamSupport.stream(rowSequence.asIterable().spliterator(), false)
            .onClose {
                reader.close()
                allocator.close()
            }
    }

    fun callInfo(timeout: Duration? = null): InfoData {
        val response = send(
            uri = rhyDbUris.info,
            bodyHandler = BodyHandlers.ofString(),
            tryToReadRhyDbErrorFromBody = ::tryToReadRhyDbErrorFromString,
        ) {
            if (timeout != null) {
                it.timeout(timeout)
            }
            it.GET()
        }

        return InfoData(
            dataVersion = getDataVersion(response),
            siloVersion = objectMapper.readValue<RhyDbInfo>(response.body()).version,
        )
    }

    fun getLineageDefinition(column: String): LineageDefinition {
        val response = send(
            uri = rhyDbUris.lineageDefinition(column),
            bodyHandler = BodyHandlers.ofString(),
            tryToReadRhyDbErrorFromBody = ::tryToReadRhyDbErrorFromString,
        ) { it.GET() }

        val body = response.body()

        if (body.isBlank()) {
            return emptyMap()
        }

        try {
            return yamlObjectMapper.objectMapper.readValue(body)
        } catch (e: Exception) {
            log.error {
                val truncateLength = 1000
                val bodyToLog = when {
                    body.length > truncateLength -> body.substring(0, truncateLength) + "... (truncated)"
                    else -> body
                }
                "Failed to parse lineage definition from SILO, it was: '$bodyToLog'"
            }
            throw RuntimeException("Failed to parse lineage definition from SILO: ${e.message}", e)
        }
    }

    private fun <ResponseBodyType> send(
        uri: URI,
        bodyHandler: HttpResponse.BodyHandler<ResponseBodyType>,
        tryToReadRhyDbErrorFromBody: (ResponseBodyType) -> RhyDbErrorResponse,
        buildRequest: (HttpRequest.Builder) -> Unit,
    ): HttpResponse<ResponseBodyType> {
        val request = HttpRequest.newBuilder(uri)
            .apply(buildRequest)
            .apply {
                if (RequestContextHolder.getRequestAttributes() != null && requestIdContext.requestId != null) {
                    header(REQUEST_ID, requestIdContext.requestId)
                }
            }
            .build()

        val startedAtMillis = System.currentTimeMillis()

        val response = try {
            try {
                httpClient.send(request, bodyHandler)
            } catch (ioException: IOException) {
                // When sending requests to SILO behind an NGINX, NGINX will send GOAWAY
                // after 1000 requests through the same connection. The HTTPClient will
                // retry GET requests (idempotent) but not POST requests. Our POST requests
                // are idempotent as well, so we can do a retry.
                if (ioException.message?.contains("GOAWAY") == true) {
                    httpClient.send(request, bodyHandler) // retry
                } else {
                    throw ioException
                }
            }
        } catch (exception: Exception) {
            throw when (exception) {
                is HttpTimeoutException -> RhyDbTimeoutException(rhyDbTimeoutMessage(uri, startedAtMillis, exception))

                is ConnectException,
                is UnknownHostException,
                is NoRouteToHostException,
                is PortUnreachableException,
                -> RhyDbNotReachableException(rhyDbNotReachableMessage(uri, exception))

                // we don't know what went wrong here, so don't claim that we couldn't connect
                else -> RuntimeException(rhyDbErrorMessage(uri, exception), exception)
            }
        }

        if (!uri.toString().endsWith("info")) {
            log.info { "Response from SILO: ${response.statusCode()}" }
        }

        if (response.statusCode() != 200) {
            val rhyDbErrorResponse = tryToReadRhyDbErrorFromBody(response.body())

            if (response.statusCode() == 503) {
                val message = rhyDbErrorResponse.message
                throw RhyDbUnavailableException(
                    "SILO is currently unavailable: $message",
                    response.headers().firstValue("retry-after").orElse(null),
                )
            }

            throw RhyDbException(
                response.statusCode(),
                rhyDbErrorResponse.error,
                "Error from SILO: " + rhyDbErrorResponse.message,
            )
        }

        return response
    }

    private fun rhyDbNotReachableMessage(
        uri: URI,
        exception: Exception,
    ) = "Could not connect to silo at $uri: ${exception::class} ${exception.message}"

    private fun rhyDbErrorMessage(
        uri: URI,
        exception: Exception,
    ) = "Error talking to silo at $uri: ${exception::class} ${exception.message}"

    private fun rhyDbTimeoutMessage(
        uri: URI,
        startedAtMillis: Long,
        exception: HttpTimeoutException,
    ): String {
        val elapsedMillis = System.currentTimeMillis() - startedAtMillis
        return when (exception) {
            is HttpConnectTimeoutException -> "Timed out connecting to silo at $uri after ${elapsedMillis}ms"
            else -> "Timed out waiting for a response from silo at $uri after ${elapsedMillis}ms"
        }
    }

    private fun tryToReadRhyDbErrorFromString(responseBody: String) =
        try {
            objectMapper.readValue<RhyDbErrorResponse>(responseBody)
        } catch (e: Exception) {
            log.error { "Failed to deserialize error response from SILO: $e" }

            throw RhyDbException(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Unexpected error from SILO: $responseBody",
            )
        }

    private fun getDataVersion(response: HttpResponse<*>): String =
        response.headers().firstValue("data-version").orElse("")
}

/**
 * Indicates that SILO returned an error response and forwards the status code, error title and message.
 */
class RhyDbException(
    val statusCode: Int,
    val title: String,
    override val message: String,
) : Exception(message)

/**
 * Indicates that SILO is reachable but claims that it's currently unavailable (HTTP 503).
 */
class RhyDbUnavailableException(
    override val message: String,
    val retryAfter: String?,
) : Exception(message)

/**
 * Indicates that SILO did not answer within the timeout that LAPIS set for the request.
 * SILO may well be reachable and healthy - the request simply outlived its budget.
 */
class RhyDbTimeoutException(
    override val message: String,
) : Exception(message)

/**
 * Indicates that SILO is not reachable at all (e.g. connection refused).
 */
class RhyDbNotReachableException(
    override val message: String,
) : Exception(message)

data class WithDataVersion<ResponseType>(
    val dataVersion: String,
    val queryResult: ResponseType,
) {
    fun <NewResponseType> map(transform: (ResponseType) -> NewResponseType): WithDataVersion<NewResponseType> =
        WithDataVersion(dataVersion, transform(queryResult))
}

data class RhyDbErrorResponse(
    val error: String,
    val message: String,
)

data class RhyDbInfo(
    val version: String,
)

typealias LineageDefinition = Map<String, LineageNode>

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class LineageNode(
    val parents: List<String>?,
    val aliases: List<String>?,
)
