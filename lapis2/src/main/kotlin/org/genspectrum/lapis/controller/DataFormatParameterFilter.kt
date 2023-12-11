package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.genspectrum.lapis.util.CachedBodyHttpServletRequest
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.Collections
import java.util.Enumeration

private val log = KotlinLogging.logger {}

const val TEXT_CSV_HEADER = "text/csv"
const val TEXT_TSV_HEADER = "text/tab-separated-values"

@Component
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
            when (reReadableRequest.getRequestFields()[FORMAT_PROPERTY]?.textValue()?.uppercase()) {
                "CSV" -> {
                    log.debug { "Overwriting Accept header to $TEXT_CSV_HEADER due to format property" }
                    return TEXT_CSV_HEADER
                }

                "TSV" -> {
                    log.debug { "Overwriting Accept header to $TEXT_TSV_HEADER due to format property" }
                    return TEXT_TSV_HEADER
                }

                "JSON" -> {
                    log.debug {
                        "Overwriting Accept header to ${MediaType.APPLICATION_JSON_VALUE} due to format property"
                    }
                    return MediaType.APPLICATION_JSON_VALUE
                }

                else -> {}
            }
        }

        return super.getHeader(name)
    }

    override fun getHeaders(name: String): Enumeration<String>? {
        if (name.equals("Accept", ignoreCase = true)) {
            when (reReadableRequest.getRequestFields()[FORMAT_PROPERTY]?.textValue()?.uppercase()) {
                "CSV" -> {
                    log.debug { "Overwriting Accept header to $TEXT_CSV_HEADER due to format property" }
                    return Collections.enumeration(listOf(TEXT_CSV_HEADER))
                }

                "TSV" -> {
                    log.debug { "Overwriting Accept header to $TEXT_TSV_HEADER due to format property" }
                    return Collections.enumeration(listOf(TEXT_TSV_HEADER))
                }

                "JSON" -> {
                    log.debug {
                        "Overwriting Accept header to ${MediaType.APPLICATION_JSON_VALUE} due to format property"
                    }
                    return Collections.enumeration(listOf(MediaType.APPLICATION_JSON_VALUE))
                }

                else -> {}
            }
        }

        return super.getHeaders(name)
    }
}
