package org.genspectrum.lapis.silo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import org.genspectrum.lapis.request.DataVersion
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
) {
    fun <ResponseType> sendQuery(query: SiloQuery<ResponseType>): ResponseType {
        val queryJson = objectMapper.writeValueAsString(query)

        log.info { "Calling SILO: $queryJson" }

        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder(URI("$siloUrl/query"))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(queryJson))
            .build()

        val response = try {
            client.send(request, BodyHandlers.ofString())
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
            val siloErrorResponse = try {
                objectMapper.readValue<SiloErrorResponse>(response.body())
            } catch (e: Exception) {
                log.error { "Failed to deserialize error response from SILO: $e" }
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

        addDataVersion(response)

        try {
            return objectMapper.readValue(response.body(), query.action.typeReference).queryResult
        } catch (exception: Exception) {
            val message = "Could not parse response from silo: " + exception::class.toString() + " " + exception.message
            throw RuntimeException(message, exception)
        }
    }

    private fun addDataVersion(response: HttpResponse<String>) {
        dataVersion.dataVersion = response.headers().firstValue("data-version").orElse("")
    }
}

class SiloException(val statusCode: Int, val title: String, override val message: String) : Exception(message)

data class SiloQueryResponse<ResponseType>(
    val queryResult: ResponseType,
)

data class SiloErrorResponse(val error: String, val message: String)
