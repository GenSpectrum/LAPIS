package org.genspectrum.lapis.silo

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

@Component
class SiloClient(@Value("\${silo.url}") private val siloUrl: String) {
    private val objectMapper = jacksonObjectMapper()

    fun <ResponseType> sendQuery(query: SiloQuery<ResponseType>): ResponseType {
        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder(URI("$siloUrl/query"))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(query)))
            .build()

        val response =
            try {
                client.send(request, BodyHandlers.ofString())
            } catch (exception: Exception) {
                throw SiloException(exception.message, exception)
            }

        if (response.statusCode() != 200) {
            throw SiloException(response.body(), null)
        }

        try {
            return objectMapper.readValue(response.body(), query.action.typeReference).queryResult
        } catch (exception: Exception) {
            throw SiloException(exception.message, exception)
        }
    }
}

class SiloException(message: String?, cause: Throwable?) : Exception(message, cause)

data class SiloQueryResponse<ResponseType>(
    val queryResult: ResponseType,
    val actionTime: Int,
    val filterTime: Int,
    val parseTime: Int,
)
