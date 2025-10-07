package org.genspectrum.lapis.response

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.controller.LapisHeaders.LAPIS_DATA_VERSION
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_CSV_VALUE
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_TSV_VALUE
import org.genspectrum.lapis.controller.middleware.ESCAPED_ACCEPT_HEADER_PARAMETER
import org.genspectrum.lapis.controller.middleware.HEADERS_ACCEPT_HEADER_PARAMETER
import org.genspectrum.lapis.log
import org.genspectrum.lapis.logging.RequestContext
import org.genspectrum.lapis.request.CommonSequenceFilters
import org.genspectrum.lapis.response.Delimiter.COMMA
import org.genspectrum.lapis.response.Delimiter.TAB
import org.genspectrum.lapis.silo.DataVersion
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.io.IOException
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
    private val ianaTsvWriter: IanaTsvWriter,
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
        val dataTypeName = "plain JSON data"

        val jsonFactory = objectMapper.factory
        sequenceData.use { inputStream ->
            response.outputStream.writer().use { outputStream ->
                jsonFactory.createGenerator(outputStream).use { generator ->
                    try {
                        generator.writeStartArray()
                        inputStream.forEach {
                            generator.writeObject(it)
                        }
                        generator.writeEndArray()
                    } catch (e: IOException) {
                        log.info { "Client likely disconnected while streaming $dataTypeName" }
                    } catch (e: Exception) {
                        log.error(e) { "Error streaming $dataTypeName" }
                        throw e
                    }
                }
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
        val dataTypeName = "Lapis JSON data"

        val jsonFactory = objectMapper.factory

        sequenceData.use { inputStream ->
            response.outputStream.writer().use { outputStream ->
                jsonFactory.createGenerator(outputStream).use { generator ->
                    try {
                        generator.writeStartObject()

                        generator.writeArrayFieldStart("data")
                        inputStream.forEach {
                            generator.writeObject(it)
                        }
                        generator.writeEndArray()

                        generator.writePOJOField("info", lapisInfoFactory.create())

                        generator.writeEndObject()
                    } catch (e: IOException) {
                        log.info { "Client likely disconnected while streaming $dataTypeName" }
                    } catch (e: Exception) {
                        log.error(e) { "Error streaming $dataTypeName" }
                        throw e
                    }
                }
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
        val headersParameter = getMediaTypeParameter(
            targetMediaType,
            HEADERS_ACCEPT_HEADER_PARAMETER,
            responseFormat.acceptHeader,
        )
        val includeHeaders = headersParameter != "false"

        val escapeParameter = getMediaTypeParameter(
            targetMediaType,
            ESCAPED_ACCEPT_HEADER_PARAMETER,
            responseFormat.acceptHeader,
        )
        val escapeNewlines = escapeParameter == "true"

        val contentType = when {
            !includeHeaders -> MediaType.TEXT_PLAIN
            else -> MediaType(targetMediaType, Charset.defaultCharset())
        }

        response.setHeader(LAPIS_DATA_VERSION, dataVersion.dataVersion)
        if (response.contentType == null) {
            response.contentType = contentType.toString()
        }

        if (escapeNewlines) {
            response.outputStream.use {
                ianaTsvWriter.write(
                    appendable = it.writer(),
                    includeHeaders = includeHeaders,
                    data = data,
                    delimiter = responseFormat.delimiter,
                )
            }
        } else {
            response.outputStream.use {
                csvWriter.write(
                    appendable = it.writer(),
                    includeHeaders = includeHeaders,
                    data = data,
                    delimiter = responseFormat.delimiter,
                )
            }
        }
    }

    private fun getMediaTypeParameter(
        targetMediaType: MediaType,
        parameter: String,
        acceptHeader: List<MediaType>,
    ): String? =
        acceptHeader.find { it.includes(targetMediaType) }
            ?.parameters
            ?.get(parameter)
}
