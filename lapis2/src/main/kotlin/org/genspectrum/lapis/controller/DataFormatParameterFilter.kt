package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.genspectrum.lapis.util.CachedBodyHttpServletRequest
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.Collections
import java.util.Enumeration

private val log = KotlinLogging.logger {}

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
                    log.debug { "Overwriting Accept header to text/csv due to format property" }
                    return "text/csv"
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
                    log.debug { "Overwriting Accept header to text/csv due to format property" }
                    return Collections.enumeration(listOf("text/csv"))
                }

                else -> {}
            }
        }

        return super.getHeaders(name)
    }
}
