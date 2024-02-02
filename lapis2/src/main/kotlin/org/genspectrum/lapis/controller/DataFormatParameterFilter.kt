package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.genspectrum.lapis.util.CachedBodyHttpServletRequest
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.Collections
import java.util.Enumeration

private val log = KotlinLogging.logger {}

const val HEADERS_ACCEPT_HEADER_PARAMETER = "headers"

const val TEXT_CSV_HEADER = "text/csv"
const val TEXT_CSV_WITHOUT_HEADERS_HEADER = "text/csv;$HEADERS_ACCEPT_HEADER_PARAMETER=false"
const val TEXT_TSV_HEADER = "text/tab-separated-values"

const val DATA_FORMAT_FILTER_ORDER = 0

object DataFormat {
    const val JSON = "JSON"
    const val CSV = "CSV"
    const val CSV_WITHOUT_HEADERS = "CSV-WITHOUT-HEADERS"
    const val TSV = "TSV"
}

@Component
@Order(DATA_FORMAT_FILTER_ORDER)
class DataFormatParameterFilter(val objectMapper: ObjectMapper) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val reReadableRequest = CachedBodyHttpServletRequest(request, objectMapper)

        filterChain.doFilter(AcceptHeaderModifyingRequestWrapper(reReadableRequest), response)
    }
}

class AcceptHeaderModifyingRequestWrapper(
    private val reReadableRequest: CachedBodyHttpServletRequest,
) : HttpServletRequestWrapper(reReadableRequest) {
    override fun getHeader(name: String): String? {
        if (name.equals("Accept", ignoreCase = true)) {
            when (val overwrittenValue = findAcceptHeaderOverwriteValue()) {
                null -> {}
                else -> return overwriteWith(overwrittenValue)
            }
        }

        return super.getHeader(name)
    }

    override fun getHeaders(name: String): Enumeration<String>? {
        if (name.equals("Accept", ignoreCase = true)) {
            when (val overwrittenValue = findAcceptHeaderOverwriteValue()) {
                null -> {}
                else -> return Collections.enumeration(listOf(overwriteWith(overwrittenValue)))
            }
        }

        return super.getHeaders(name)
    }

    override fun getHeaderNames(): Enumeration<String> =
        when (findAcceptHeaderOverwriteValue()) {
            null -> super.getHeaderNames()
            else -> Collections.enumeration(super.getHeaderNames().toList() + "Accept")
        }

    private fun findAcceptHeaderOverwriteValue() =
        when (reReadableRequest.getStringField(FORMAT_PROPERTY)?.uppercase()) {
            DataFormat.CSV -> TEXT_CSV_HEADER
            DataFormat.CSV_WITHOUT_HEADERS -> TEXT_CSV_WITHOUT_HEADERS_HEADER
            DataFormat.TSV -> TEXT_TSV_HEADER
            DataFormat.JSON -> MediaType.APPLICATION_JSON_VALUE
            else -> null
        }

    private fun overwriteWith(value: String): String {
        log.debug { "Overwriting Accept header to $value due to format property" }
        return value
    }
}
