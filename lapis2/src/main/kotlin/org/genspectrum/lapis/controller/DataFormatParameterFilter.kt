package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.util.CachedBodyHttpServletRequest
import org.genspectrum.lapis.util.HeaderModifyingRequestWrapper
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

const val HEADERS_ACCEPT_HEADER_PARAMETER = "headers"

const val TEXT_CSV_HEADER = "text/csv"
const val TEXT_CSV_WITHOUT_HEADERS_HEADER = "text/csv;$HEADERS_ACCEPT_HEADER_PARAMETER=false"
const val TEXT_TSV_HEADER = "text/tab-separated-values"

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

        filterChain.doFilter(
            HeaderModifyingRequestWrapper(
                reReadableRequest,
                ACCEPT,
                ::findAcceptHeaderOverwriteValue,
            ),
            response,
        )
    }

    private fun findAcceptHeaderOverwriteValue(reReadableRequest: CachedBodyHttpServletRequest) =
        when (reReadableRequest.getStringField(FORMAT_PROPERTY)?.uppercase()) {
            DataFormat.CSV -> TEXT_CSV_HEADER
            DataFormat.CSV_WITHOUT_HEADERS -> TEXT_CSV_WITHOUT_HEADERS_HEADER
            DataFormat.TSV -> TEXT_TSV_HEADER
            DataFormat.JSON -> MediaType.APPLICATION_JSON_VALUE
            else -> null
        }
}
