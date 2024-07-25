package org.genspectrum.lapis.controller.middleware

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_CSV
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_TSV
import org.genspectrum.lapis.controller.SampleRoute
import org.genspectrum.lapis.request.DOWNLOAD_AS_FILE_PROPERTY
import org.genspectrum.lapis.request.DOWNLOAD_FILE_BASENAME_PROPERTY
import org.genspectrum.lapis.util.CachedBodyHttpServletRequest
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(DOWNLOAD_AS_FILE_FILTER_ORDER)
class DownloadAsFileFilter(
    private val objectMapper: ObjectMapper,
    private val requestCompression: RequestCompression,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val reReadableRequest = CachedBodyHttpServletRequest.from(request, objectMapper)

        val downloadAsFile = reReadableRequest.getBooleanField(DOWNLOAD_AS_FILE_PROPERTY) ?: false
        if (downloadAsFile) {
            val filename = getFilename(reReadableRequest)
            response.setHeader(CONTENT_DISPOSITION, "attachment; filename=$filename")
        }
        filterChain.doFilter(reReadableRequest, response)
    }

    private fun getFilename(request: CachedBodyHttpServletRequest): String {
        val matchingRoute =
            SampleRoute.entries.find { request.getProxyAwarePath().startsWith("/sample${it.pathSegment}") }
        val dataName = request.getStringField(DOWNLOAD_FILE_BASENAME_PROPERTY)
            ?: matchingRoute?.pathSegment?.trim('/')
            ?: "data"

        val compressionEnding = when (val compressionSource = requestCompression.compressionSource) {
            is CompressionSource.RequestProperty -> compressionSource.compression.fileEnding
            else -> ""
        }

        val acceptHeader = request.getHeader(ACCEPT)?.let { MediaType.parseMediaType(it) }
        val fileEnding = if (matchingRoute?.servesFasta == true) {
            ".fasta"
        } else if (acceptHeader?.equalsTypeAndSubtype(TEXT_TSV) == true) {
            ".tsv"
        } else if (acceptHeader?.equalsTypeAndSubtype(TEXT_CSV) == true) {
            ".csv"
        } else {
            ".json"
        }

        return "$dataName$fileEnding$compressionEnding"
    }
}
