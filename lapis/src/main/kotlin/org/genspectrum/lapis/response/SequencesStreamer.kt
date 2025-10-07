package org.genspectrum.lapis.response

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.controller.LapisHeaders.LAPIS_DATA_VERSION
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_X_FASTA
import org.genspectrum.lapis.controller.middleware.SequencesDataFormat
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

        // Prefer the servlet-provided writer (respects characterEncoding)
        val writer = response.writer
        var wroteAnything = false
        var count = 0

        fun isClientAbort(t: Throwable?): Boolean {
            var c = t
            while (c != null) {
                val name = c.javaClass.name
                val msg = c.message?.lowercase()
                if (name.endsWith("ClientAbortException")) return true
                if (msg?.contains("broken pipe") == true) return true
                if (msg?.contains("connection reset by peer") == true) return true
                c = c.cause
            }
            return false
        }

        try {
            for (row in sequencesResponse.sequenceData) {
                for (sequenceName in sequencesResponse.requestedSequenceNames) {
                    val node = row[sequenceName]
                    if (node == null || node == NullNode.instance) continue

                    val fastaHeader = sequencesResponse.fastaHeaderTemplate.fillTemplate(
                        values = row,
                        sequenceName = sequenceName,
                    )

                    writer.append('>')
                    writer.append(fastaHeader)
                    writer.append('\n')
                    writer.append(node.asText())
                    writer.append('\n')

                    wroteAnything = true
                    if (++count % 500 == 0) writer.flush() // periodic flush to push chunks & detect aborts
                }
            }
            writer.flush() // final flush may detect an abort too
        } catch (ioe: IOException) {
            // Client went away during write/flush
            if (isClientAbort(ioe)) {
                // Log at DEBUG/INFO; treat as normal cancellation
                // log.debug("Client aborted FASTA stream", ioe)
            } else {
                // log.warn("I/O error while streaming FASTA", ioe)
                // Nothing else we can send; headers are probably committed.
            }
        } finally {
            try {
                // Close quietly; after an abort close() can also throw
                writer.close()
            } catch (_: IOException) { /* swallow */ }
            // If sequencesResponse holds any resources (cursor/stream), close them here.
            // e.g. sequencesResponse.close()
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
