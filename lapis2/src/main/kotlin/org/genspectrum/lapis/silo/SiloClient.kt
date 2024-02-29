package org.genspectrum.lapis.silo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import org.genspectrum.lapis.controller.LapisHeaders.REQUEST_ID
import org.genspectrum.lapis.logging.RequestIdContext
import org.genspectrum.lapis.response.InfoData
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers

private val log = KotlinLogging.logger {}

const val SILO_RESPONSE_MAX_LOG_LENGTH = 10_000

@Component
class SiloClient(
    @Value("\${silo.url}") private val siloUrl: String,
    private val objectMapper: ObjectMapper,
    private val dataVersion: DataVersion,
    private val requestIdContext: RequestIdContext,
) {
    private val httpClient = HttpClient.newHttpClient()

    fun <ResponseType> sendQuery(query: SiloQuery<ResponseType>): ResponseType {
        val queryJson = objectMapper.writeValueAsString(query)

        log.info { "Calling SILO: $queryJson" }

        val response = send(URI("$siloUrl/query")) {
            it.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(queryJson))
        }

        try {
            return objectMapper.readValue(response.body(), query.action.typeReference).queryResult
        } catch (exception: Exception) {
            val message = "Could not parse response from silo: " + exception::class.toString() + " " + exception.message
            throw RuntimeException(message, exception)
        }
    }

    fun callInfo(): InfoData {
        log.info { "Calling SILO info" }

        val response = send(URI("$siloUrl/info")) { it.GET() }

        return InfoData(getDataVersion(response))
    }

    private fun send(
        uri: URI,
        buildRequest: (HttpRequest.Builder) -> Unit,
    ): HttpResponse<String> {
        val request = HttpRequest.newBuilder(uri)
            .apply(buildRequest)
            .apply {
                if (requestIdContext.requestId != null) {
                    header(REQUEST_ID, requestIdContext.requestId)
                }
            }
            .build()

        val response = try {
            httpClient.send(request, BodyHandlers.ofString())
        } catch (exception: Exception) {
            val message = "Could not connect to silo: " + exception::class.toString() + " " + exception.message
            throw RuntimeException(message, exception)
        }

        log.info { "Response from SILO: ${response.statusCode()}" }
        log.debug {
            val body = response.body()
            val truncationPostfix = when {
                body.length > SILO_RESPONSE_MAX_LOG_LENGTH -> "(...truncated)"
                else -> ""
            }
            "Data from SILO: ${body.take(SILO_RESPONSE_MAX_LOG_LENGTH)}$truncationPostfix"
        }

        if (response.statusCode() != 200) {
            val siloErrorResponse = tryToReadSiloError(response)

            if (response.statusCode() == 503) {
                val message = siloErrorResponse?.message ?: "Unknown reason."
                throw SiloUnavailableException(
                    "SILO is currently unavailable: $message",
                    response.headers().firstValue("retry-after").orElse(null),
                )
            }

            if (siloErrorResponse == null) {
                throw SiloException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal Server Error",
                    "Unexpected error from SILO: ${response.body()}",
                )
            }

            throw SiloException(
                response.statusCode(),
                siloErrorResponse.error,
                "Error from SILO: " + siloErrorResponse.message,
            )
        }

        addDataVersionToRequestScope(response)
        return response
    }

    private fun tryToReadSiloError(response: HttpResponse<String>) =
        try {
            objectMapper.readValue<SiloErrorResponse>(response.body())
        } catch (e: Exception) {
            log.error { "Failed to deserialize error response from SILO: $e" }
            null
        }

    private fun addDataVersionToRequestScope(response: HttpResponse<String>) {
        dataVersion.dataVersion = getDataVersion(response)
    }

    private fun getDataVersion(response: HttpResponse<String>): String {
        return response.headers().firstValue("data-version").orElse("")
    }
}

class SiloException(val statusCode: Int, val title: String, override val message: String) : Exception(message)

class SiloUnavailableException(override val message: String, val retryAfter: String?) : Exception(message)

data class SiloQueryResponse<ResponseType>(
    val queryResult: ResponseType,
)

data class SiloErrorResponse(val error: String, val message: String)
