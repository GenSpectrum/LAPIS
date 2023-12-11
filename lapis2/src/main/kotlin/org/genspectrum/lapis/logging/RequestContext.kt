package org.genspectrum.lapis.logging

import com.fasterxml.jackson.core.JsonProcessingException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.genspectrum.lapis.request.CommonSequenceFilters
import org.genspectrum.lapis.util.TimeFactory
import org.slf4j.Logger
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope
import org.springframework.web.filter.OncePerRequestFilter

@Component
@RequestScope
class RequestContext {
    var unixTimestamp: Long = 0
    var responseTimeInMilliSeconds: Long = 0
    var endpoint: String? = null
    var filter: CommonSequenceFilters? = null
    var responseCode: Int? = null
}

private val log = KotlinLogging.logger {}

@Component
class RequestContextLoggerFilterAdapter(private val requestContextLogger: RequestContextLogger) :
    OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        requestContextLogger.handleAndLogRequest(request, response, filterChain)
    }
}

class RequestContextLogger(
    private val requestContext: RequestContext,
    private val objectMapper: StatisticsLogObjectMapper,
    private val statisticsLogger: Logger,
    private val timeFactory: TimeFactory,
) {
    fun handleAndLogRequest(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val before: Long = timeFactory.now()
        requestContext.unixTimestamp = before
        requestContext.endpoint = request.requestURI

        try {
            filterChain.doFilter(request, response)
        } finally {
            requestContext.responseCode = response.status
            requestContext.responseTimeInMilliSeconds = timeFactory.now() - before
            try {
                statisticsLogger.info(objectMapper.writeValueAsString(requestContext))
            } catch (e: JsonProcessingException) {
                log.error(e) { "Could not log statistics message: " + e.message }
            }
        }
    }
}
