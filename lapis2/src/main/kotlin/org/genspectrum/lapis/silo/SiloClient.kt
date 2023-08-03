package org.genspectrum.lapis.silo

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.genspectrum.lapis.request.DataVersion
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers

private val log = KotlinLogging.logger {}

@Component
class SiloClient(
    @Value("\${silo.url}") private val siloUrl: String,
    private val objectMapper: ObjectMapper,
    private val dataVersion: DataVersion,
) {
    fun <ResponseType> sendQuery(
        query: SiloQuery<ResponseType>,
    ): ResponseType {
        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder(URI("$siloUrl/query"))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(query)))
            .build()

        log.info { "Calling SILO: $query" }

        val response =
            try {
                client.send(request, BodyHandlers.ofString())
            } catch (exception: Exception) {
                val message = "Could not connect to silo: " + exception::class.toString() + " " + exception.message
                throw SiloException(message, exception)
            }

        log.info { "Response from SILO: ${response.statusCode()} - ${response.body()}" }

        if (response.statusCode() != 200) {
            throw SiloException(response.body(), null)
        }

        addDataVersion(response)

        try {
            return objectMapper.readValue(response.body(), query.action.typeReference).queryResult
        } catch (exception: Exception) {
            val message = "Could not parse response from silo: " + exception::class.toString() + " " + exception.message
            throw SiloException(message, exception)
        }
    }

    private fun addDataVersion(response: HttpResponse<String>) {
        dataVersion.dataVersion = response.headers().firstValue("data-version").orElse("")
    }
}

class SiloException(message: String?, cause: Throwable?) : Exception(message, cause)

data class SiloQueryResponse<ResponseType>(
    val queryResult: ResponseType,
)
