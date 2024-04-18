package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.genspectrum.lapis.util.CachedBodyHttpServletRequest
import org.genspectrum.lapis.util.HeaderModifyingRequestWrapper
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

const val HEADERS_ACCEPT_HEADER_PARAMETER = "headers"

object DataFormat {
    const val JSON = "JSON"
    const val CSV = "CSV"
    const val CSV_WITHOUT_HEADERS = "CSV-WITHOUT-HEADERS"
    const val TSV = "TSV"
}

private val log = KotlinLogging.logger {}

@Component
@Order(DATA_FORMAT_FILTER_ORDER)
class DataFormatParameterFilter(val objectMapper: ObjectMapper) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val reReadableRequest = CachedBodyHttpServletRequest(request, objectMapper)

        val requestWithModifiedAcceptHeader = HeaderModifyingRequestWrapper(
            reReadableRequest,
            ACCEPT,
            ::findAcceptHeaderOverwriteValue,
        )

        filterChain.doFilter(
            requestWithModifiedAcceptHeader,
            response,
        )
    }

    private fun findAcceptHeaderOverwriteValue(reReadableRequest: CachedBodyHttpServletRequest) =
        when (reReadableRequest.getStringField(FORMAT_PROPERTY)?.uppercase()) {
            DataFormat.CSV -> LapisMediaType.TEXT_CSV_VALUE
            DataFormat.CSV_WITHOUT_HEADERS -> LapisMediaType.TEXT_CSV_WITHOUT_HEADERS_VALUE
            DataFormat.TSV -> LapisMediaType.TEXT_TSV_VALUE
            DataFormat.JSON -> MediaType.APPLICATION_JSON_VALUE
            else -> null
        }
}
