package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.genspectrum.lapis.util.CachedBodyHttpServletRequest
import org.genspectrum.lapis.util.HeaderModifyingRequestWrapper
import org.genspectrum.lapis.util.ResponseWithContentType
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.InvalidMediaTypeException
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
            when (isCsvOrTsvWithoutHeaders(requestWithModifiedAcceptHeader)) {
                true -> ResponseWithContentType(response, MediaType.TEXT_PLAIN_VALUE)
                false -> response
            },
        )
    }

    private fun findAcceptHeaderOverwriteValue(reReadableRequest: CachedBodyHttpServletRequest) =
        when (reReadableRequest.getStringField(FORMAT_PROPERTY)?.uppercase()) {
            DataFormat.CSV -> LapisMediaType.TEXT_CSV
            DataFormat.CSV_WITHOUT_HEADERS -> LapisMediaType.TEXT_CSV_WITHOUT_HEADERS
            DataFormat.TSV -> LapisMediaType.TEXT_TSV
            DataFormat.JSON -> MediaType.APPLICATION_JSON_VALUE
            else -> null
        }

    private fun isCsvOrTsvWithoutHeaders(requestWithModifiedAcceptHeader: HeaderModifyingRequestWrapper): Boolean {
        val acceptHeader = requestWithModifiedAcceptHeader.getHeader(ACCEPT) ?: return false

        val acceptMediaType = try {
            MediaType.parseMediaType(acceptHeader)
        } catch (e: InvalidMediaTypeException) {
            log.info { "failed to parse accept header: " + e.message }
            return false
        }

        if (acceptMediaType.parameters[HEADERS_ACCEPT_HEADER_PARAMETER] == "false") {
            log.info { "Setting response content type to plain text due to 'headers=false'" }
            return true
        }

        return false
    }
}
