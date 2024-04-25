package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_CSV_VALUE
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_TSV_VALUE
import org.genspectrum.lapis.util.CachedBodyHttpServletRequest
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
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
        val dataName = matchingRoute?.pathSegment?.trim('/') ?: "data"

        val compressionEnding = when (val compressionSource = requestCompression.compressionSource) {
            is CompressionSource.RequestProperty -> compressionSource.compression.fileEnding
            else -> ""
        }

        val fileEnding = when (request.getHeader(ACCEPT)) {
            TEXT_CSV_VALUE -> "csv"
            TEXT_TSV_VALUE -> "tsv"
            else -> when (matchingRoute?.servesFasta) {
                true -> "fasta"
                else -> "json"
            }
        }
        return "$dataName.$fileEnding$compressionEnding"
    }
}
