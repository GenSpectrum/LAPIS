package org.genspectrum.lapis.response

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.controller.LapisHeaders.LAPIS_DATA_VERSION
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_CSV_VALUE
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_TSV_VALUE
import org.genspectrum.lapis.controller.middleware.HEADERS_ACCEPT_HEADER_PARAMETER
import org.genspectrum.lapis.logging.RequestContext
import org.genspectrum.lapis.request.CommonSequenceFilters
import org.genspectrum.lapis.response.Delimiter.COMMA
import org.genspectrum.lapis.response.Delimiter.TAB
import org.genspectrum.lapis.silo.DataVersion
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.nio.charset.Charset
import java.util.stream.Stream

sealed interface ResponseFormat {
    data object Json : ResponseFormat

    data class Csv(
        val delimiter: Delimiter,
        val acceptHeader: List<MediaType>,
    ) : ResponseFormat
}

@Component
class LapisResponseStreamer(
    private val dataVersion: DataVersion,
    private val objectMapper: ObjectMapper,
    private val lapisInfoFactory: LapisInfoFactory,
    private val requestContext: RequestContext,
    private val csvWriter: CsvWriter,
) {
    fun <Request : CommonSequenceFilters> streamData(
        request: Request,
        getData: (request: Request) -> RecordCollection<*>,
        response: HttpServletResponse,
        responseFormat: ResponseFormat,
    ) {
        requestContext.filter = request
        val data = getData(request)

        response.setHeader(LAPIS_DATA_VERSION, dataVersion.dataVersion)

        when (responseFormat) {
            is ResponseFormat.Json -> {
                val isDownload = response.getHeader(HttpHeaders.CONTENT_DISPOSITION)?.startsWith("attachment") ?: false

                if (isDownload) {
                    streamPlainJson(response, data.records)
                    return
                }

                streamLapisResponseJson(response, data.records)
            }

            is ResponseFormat.Csv -> streamCsv(response, data, responseFormat)
        }
    }

    private fun streamPlainJson(
        response: HttpServletResponse,
        sequenceData: Stream<*>,
    ) {
        if (response.contentType == null) {
            response.contentType = MediaType(MediaType.APPLICATION_JSON).toString()
        }

        val jsonFactory = objectMapper.factory

        response.outputStream.writer().use { stream ->
            jsonFactory.createGenerator(stream).use { generator ->
                generator.writeStartArray()
                sequenceData.forEach {
                    generator.writeObject(it)
                }
                generator.writeEndArray()
            }
        }
    }

    private fun streamLapisResponseJson(
        response: HttpServletResponse,
        sequenceData: Stream<*>,
    ) {
        if (response.contentType == null) {
            response.contentType = MediaType(MediaType.APPLICATION_JSON).toString()
        }

        val jsonFactory = objectMapper.factory

        response.outputStream.writer().use { stream ->
            jsonFactory.createGenerator(stream).use { generator ->
                generator.writeStartObject()

                generator.writeArrayFieldStart("data")
                sequenceData.forEach {
                    generator.writeObject(it)
                }
                generator.writeEndArray()

                generator.writePOJOField("info", lapisInfoFactory.create())

                generator.writeEndObject()
            }
        }
    }

    private fun streamCsv(
        response: HttpServletResponse,
        data: RecordCollection<*>,
        responseFormat: ResponseFormat.Csv,
    ) {
        val targetMediaType = MediaType.valueOf(
            when (responseFormat.delimiter) {
                COMMA -> TEXT_CSV_VALUE
                TAB -> TEXT_TSV_VALUE
            },
        )
        val headersParameter = getHeadersParameter(targetMediaType, responseFormat.acceptHeader)
        val includeHeaders = headersParameter != "false"

        val contentType = when {
            !includeHeaders -> MediaType.TEXT_PLAIN
            else -> MediaType(targetMediaType, Charset.defaultCharset())
        }

        response.setHeader(LAPIS_DATA_VERSION, dataVersion.dataVersion)
        if (response.contentType == null) {
            response.contentType = contentType.toString()
        }

        response.outputStream.use {
            csvWriter.write(
                appendable = it.writer(),
                includeHeaders = includeHeaders,
                data = data,
                delimiter = responseFormat.delimiter,
            )
        }
    }

    private fun getHeadersParameter(
        targetMediaType: MediaType,
        acceptHeader: List<MediaType>,
    ): String? =
        acceptHeader.find { it.includes(targetMediaType) }
            ?.parameters
            ?.get(HEADERS_ACCEPT_HEADER_PARAMETER)
}
