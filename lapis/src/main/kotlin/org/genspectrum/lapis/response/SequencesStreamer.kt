package org.genspectrum.lapis.response

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.controller.LapisHeaders.LAPIS_DATA_VERSION
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_X_FASTA
import org.genspectrum.lapis.controller.middleware.SequencesDataFormat
import org.genspectrum.lapis.log
import org.genspectrum.lapis.model.SequencesResponse
import org.genspectrum.lapis.silo.DataVersion
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.charset.Charset

@Component
class SequencesStreamer(
    private val dataVersion: DataVersion,
    private val objectMapper: ObjectMapper,
) {
    fun stream(
        sequencesResponse: SequencesResponse,
        response: HttpServletResponse,
        sequencesDataFormat: SequencesDataFormat,
    ) {
        response.setHeader(LAPIS_DATA_VERSION, dataVersion.dataVersion)

        when (sequencesDataFormat) {
            SequencesDataFormat.FASTA -> streamFasta(response, sequencesResponse)
            SequencesDataFormat.JSON -> streamJson(response, sequencesResponse)
            SequencesDataFormat.NDJSON -> streamNdjson(response, sequencesResponse)
        }
    }

    private fun streamFasta(
        response: HttpServletResponse,
        sequencesResponse: SequencesResponse,
    ) {
        if (response.contentType == null) {
            response.contentType = MediaType(TEXT_X_FASTA, Charset.defaultCharset()).toString()
        }

        val dataTypeName = "sequence FASTA"

        response.outputStream.writer().use { outputStream ->
            sequencesResponse.sequenceData.use { inputStream ->
                inputStream.forEach {
                    log.info("Processing next sequence entry")
                    for (sequenceName in sequencesResponse.requestedSequenceNames) {
                        val sequence = it[sequenceName]
                        log.info("Streaming sequence '$sequenceName' with length ${sequence?.asText()?.length ?: 0}")
                        if (sequence == null || sequence == NullNode.instance) {
                            continue
                        }

                        val fastaHeader = sequencesResponse.fastaHeaderTemplate.fillTemplate(
                            values = it,
                            sequenceName = sequenceName,
                        )
                        try {
                            outputStream.appendLine(">$fastaHeader\n${sequence.asText()}")
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
    }

    private fun streamJson(
        response: HttpServletResponse,
        sequencesResponse: SequencesResponse,
    ) {
        if (response.contentType == null) {
            response.contentType = MediaType(MediaType.APPLICATION_JSON, Charset.defaultCharset()).toString()
        }

        val dataTypeName = "sequence JSON"

        sequencesResponse.sequenceData.use { inputStream ->
            var isFirstEntry = true
            response.outputStream.writer().use { outputStream ->
                try {
                    outputStream.append('[')
                    inputStream.forEach {
                        if (isFirstEntry) {
                            isFirstEntry = false
                        } else {
                            outputStream.append(',')
                        }
                        outputStream.append(objectMapper.writeValueAsString(it))
                    }
                    outputStream.append(']')
                } catch (e: IOException) {
                    log.info { "Client likely disconnected while streaming $dataTypeName" }
                } catch (e: Exception) {
                    log.error(e) { "Error streaming sequence $dataTypeName" }
                    throw e
                }
            }
        }
    }

    private fun streamNdjson(
        response: HttpServletResponse,
        sequencesResponse: SequencesResponse,
    ) {
        if (response.contentType == null) {
            response.contentType = MediaType(MediaType.APPLICATION_NDJSON, Charset.defaultCharset()).toString()
        }
        val dataTypeName = "sequence NDJSON"

        response.outputStream.writer().use { outputStream ->
            sequencesResponse.sequenceData.use { inputStream ->
                try {
                    inputStream.forEach { outputStream.appendLine(objectMapper.writeValueAsString(it)) }
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
