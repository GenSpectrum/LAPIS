package org.genspectrum.lapis.controller.middleware

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_CSV
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_TSV
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_X_FASTA
import org.genspectrum.lapis.controller.SampleRoute
import org.genspectrum.lapis.controller.ServeType
import org.genspectrum.lapis.request.DOWNLOAD_AS_FILE_PROPERTY
import org.genspectrum.lapis.request.DOWNLOAD_FILE_BASENAME_PROPERTY
import org.genspectrum.lapis.util.CachedBodyHttpServletRequest
import org.springframework.core.annotation.Order
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import java.nio.charset.StandardCharsets
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

            // Use Spring to generate RFC 5987 encoded filename*
            val springDisposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build()
                .toString()

            // Extract filename* part and combine with plain filename
            val filenameStar = springDisposition
                .substringAfter("filename*=")
                .substringBefore(";")
                .ifEmpty { springDisposition.substringAfter("filename*=") }

            response.setHeader(CONTENT_DISPOSITION, "attachment; filename=$filename; filename*=$filenameStar")
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

        val fileEnding = findFileEnding(request, matchingRoute)

        return "$dataName$fileEnding$compressionEnding"
    }

    private fun findFileEnding(
        request: CachedBodyHttpServletRequest,
        matchingRoute: SampleRoute?,
    ): String {
        val acceptHeaders = request.getHeader(ACCEPT)
            ?.let { MediaType.parseMediaTypes(it) }
            ?.sortedByDescending { it.qualityValue }
            ?: emptyList()

        if (matchingRoute?.serveType == ServeType.SEQUENCES) {
            for (acceptHeader in acceptHeaders) {
                if (acceptHeader.equalsTypeAndSubtype(TEXT_X_FASTA)) {
                    return ".fasta"
                } else if (acceptHeader.equalsTypeAndSubtype(MediaType.APPLICATION_NDJSON)) {
                    return ".ndjson"
                } else if (acceptHeader.equalsTypeAndSubtype(MediaType.APPLICATION_JSON)) {
                    return ".json"
                }
            }
            return ".fasta"
        }
        if (matchingRoute?.serveType == ServeType.NEWICK) {
            return ".nwk"
        }
        for (acceptHeader in acceptHeaders) {
            if (acceptHeader.equalsTypeAndSubtype(TEXT_TSV)) {
                return ".tsv"
            } else if (acceptHeader.equalsTypeAndSubtype(TEXT_CSV)) {
                return ".csv"
            } else if (acceptHeader.equalsTypeAndSubtype(MediaType.APPLICATION_JSON)) {
                return ".json"
            }
        }
        return ".json"
    }
}
