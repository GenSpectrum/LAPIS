package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.util.CachedBodyHttpServletRequest
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.ACCEPT_ENCODING
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

const val DOWNLOAD_AS_FILE_FILTER_ORDER = DATA_FORMAT_FILTER_ORDER + 1

@Component
@Order(DOWNLOAD_AS_FILE_FILTER_ORDER)
class DownloadAsFileFilter(private val objectMapper: ObjectMapper) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val reReadableRequest = CachedBodyHttpServletRequest(request, objectMapper)

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

        val compressionEnding = when (Compression.fromHeaders(request.getHeaders(ACCEPT_ENCODING))) {
            Compression.GZIP -> ".gzip"
            Compression.ZSTD -> ".zstd"
            null -> ""
        }

        val fileEnding = when (request.getHeader(ACCEPT)) {
            TEXT_CSV_HEADER -> "csv"
            TEXT_TSV_HEADER -> "tsv"
            else -> when (matchingRoute?.servesFasta) {
                true -> "fasta"
                else -> "json"
            }
        }
        return "$dataName.$fileEnding$compressionEnding"
    }
}
