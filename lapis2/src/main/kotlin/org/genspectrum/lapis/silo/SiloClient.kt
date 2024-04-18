package org.genspectrum.lapis.silo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import org.genspectrum.lapis.controller.LapisHeaders.REQUEST_ID
import org.genspectrum.lapis.logging.RequestContext
import org.genspectrum.lapis.logging.RequestIdContext
import org.genspectrum.lapis.response.InfoData
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers

private val log = KotlinLogging.logger {}

@Component
class SiloClient(
    private val cachedSiloClient: CachedSiloClient,
    private val dataVersion: DataVersion,
    private val requestContext: RequestContext,
) {
    fun <ResponseType> sendQuery(query: SiloQuery<ResponseType>): List<ResponseType> {
        val result = cachedSiloClient.sendQuery(query)
        dataVersion.dataVersion = result.dataVersion

        if (RequestContextHolder.getRequestAttributes() != null && requestContext.cached == null) {
            requestContext.cached = true
        }

        return result.queryResult
    }

    fun callInfo(): InfoData {
        log.info { "Calling SILO info" }

        val info = cachedSiloClient.callInfo()
        dataVersion.dataVersion = info.dataVersion
        return info
    }
}

const val SILO_QUERY_CACHE_NAME = "siloQueryCache"

@Component
class CachedSiloClient(
    @Value("\${silo.url}") private val siloUrl: String,
    private val objectMapper: ObjectMapper,
    private val requestIdContext: RequestIdContext,
    private val requestContext: RequestContext,
) {
    private val httpClient = HttpClient.newHttpClient()

    @Cacheable(SILO_QUERY_CACHE_NAME, condition = "#query.action.cacheable && !#query.action.randomize")
    fun <ResponseType> sendQuery(query: SiloQuery<ResponseType>): WithDataVersion<ResponseType> {
        if (RequestContextHolder.getRequestAttributes() != null) {
            requestContext.cached = false
        }

        val queryJson = objectMapper.writeValueAsString(query)

        log.info { "Calling SILO: $queryJson" }

        val response = send(
            uri = URI("$siloUrl/query"),
            bodyHandler = BodyHandlers.ofLines(),
            tryToReadSiloErrorFromBody = { tryToReadSiloErrorFromString(it.findFirst().orElse("")) },
        ) {
            it.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(queryJson))
        }

        return WithDataVersion(
            queryResult = response.body()
                .filter { it.isNotBlank() }
                .map {
                    try {
                        objectMapper.readValue(it, query.action.typeReference)
                    } catch (exception: Exception) {
                        val message = "Could not parse response from silo: " +
                            exception::class.toString() + " " + exception.message
                        throw RuntimeException(message, exception)
                    }
                }
                .toList(),
            dataVersion = getDataVersion(response),
        )
    }

    fun callInfo(): InfoData {
        val response = send(URI("$siloUrl/info"), BodyHandlers.ofString(), ::tryToReadSiloErrorFromString) { it.GET() }

        return InfoData(getDataVersion(response))
    }

    private fun <ResponseBodyType> send(
        uri: URI,
        bodyHandler: HttpResponse.BodyHandler<ResponseBodyType>,
        tryToReadSiloErrorFromBody: (ResponseBodyType) -> SiloErrorResponse,
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

        val response = try {
            httpClient.send(request, bodyHandler)
        } catch (exception: Exception) {
            val message = "Could not connect to silo: " + exception::class.toString() + " " + exception.message
            throw RuntimeException(message, exception)
        }

        if (!uri.toString().endsWith("info")) {
            log.info { "Response from SILO: ${response.statusCode()}" }
        }

        if (response.statusCode() != 200) {
            val siloErrorResponse = tryToReadSiloErrorFromBody(response.body())

            if (response.statusCode() == 503) {
                val message = siloErrorResponse.message
                throw SiloUnavailableException(
                    "SILO is currently unavailable: $message",
                    response.headers().firstValue("retry-after").orElse(null),
                )
            }

            throw SiloException(
                response.statusCode(),
                siloErrorResponse.error,
                "Error from SILO: " + siloErrorResponse.message,
            )
        }

        return response
    }

    private fun tryToReadSiloErrorFromString(responseBody: String) =
        try {
            objectMapper.readValue<SiloErrorResponse>(responseBody)
        } catch (e: Exception) {
            log.error { "Failed to deserialize error response from SILO: $e" }

            throw SiloException(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Unexpected error from SILO: $responseBody",
            )
        }

    private fun getDataVersion(response: HttpResponse<*>): String {
        return response.headers().firstValue("data-version").orElse("")
    }
}

class SiloException(val statusCode: Int, val title: String, override val message: String) : Exception(message)

class SiloUnavailableException(override val message: String, val retryAfter: String?) : Exception(message)

data class WithDataVersion<ResponseType>(
    val dataVersion: String,
    val queryResult: List<ResponseType>,
)

data class SiloErrorResponse(val error: String, val message: String)
