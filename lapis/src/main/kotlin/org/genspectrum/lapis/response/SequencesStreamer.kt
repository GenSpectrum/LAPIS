package org.genspectrum.lapis.response

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.controller.LapisHeaders.LAPIS_DATA_VERSION
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_X_FASTA
import org.genspectrum.lapis.controller.middleware.SequencesDataFormat
import org.genspectrum.lapis.model.SequencesResponse
import org.genspectrum.lapis.silo.DataVersion
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
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

        response.outputStream.writer().use { stream ->
            sequencesResponse.sequenceData.forEach {
                for (sequenceName in sequencesResponse.requestedSequenceNames) {
                    val sequence = it[sequenceName]
                    if (sequence == null) {
                        continue
                    }

                    val fastaHeader = sequencesResponse.fastaHeaderTemplate.fillTemplate(
                        values = it,
                        sequenceName = sequenceName,
                    )
                    stream.appendLine(">$fastaHeader\n${sequence.asText()}")
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

        var isFirstEntry = true
        response.outputStream.writer().use { stream ->
            stream.append('[')
            sequencesResponse.sequenceData
                .forEach {
                    if (isFirstEntry) {
                        isFirstEntry = false
                    } else {
                        stream.append(',')
                    }
                    stream.append(objectMapper.writeValueAsString(it))
                }
            stream.append(']')
        }
    }

    private fun streamNdjson(
        response: HttpServletResponse,
        sequencesResponse: SequencesResponse,
    ) {
        if (response.contentType == null) {
            response.contentType = MediaType(MediaType.APPLICATION_NDJSON, Charset.defaultCharset()).toString()
        }

        response.outputStream.writer().use { stream ->
            sequencesResponse.sequenceData
                .forEach { stream.appendLine(objectMapper.writeValueAsString(it)) }
        }
    }
}
