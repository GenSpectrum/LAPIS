package org.genspectrum.lapis.response

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.controller.LapisHeaders.LAPIS_DATA_VERSION
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_NEWICK
import org.genspectrum.lapis.silo.DataVersion
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.nio.charset.Charset
import java.util.stream.Stream

@Component
class TreeStreamer(
    private val dataVersion: DataVersion,
    private val objectMapper: ObjectMapper,
) {
    fun stream(
        treeResponse: Stream<PhyloSubtreeData>,
        response: HttpServletResponse,
    ) {
        response.setHeader(LAPIS_DATA_VERSION, dataVersion.dataVersion)
        if (response.contentType == null) {
            response.contentType = MediaType(TEXT_NEWICK, Charset.defaultCharset()).toString()
        }

        response.outputStream.writer().use { stream ->
            treeResponse.forEach {
                stream.appendLine(it.subtreeNewick)
            }
        }
    }
}
