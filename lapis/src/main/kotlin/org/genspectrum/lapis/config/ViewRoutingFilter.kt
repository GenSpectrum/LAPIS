package org.genspectrum.lapis.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.controller.middleware.DATA_OPENNESS_AUTHORIZATION_FILTER_ORDER
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(DATA_OPENNESS_AUTHORIZATION_FILTER_ORDER - 1)
class ViewRoutingFilter(
    private val viewRegistry: ViewRegistry,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val segments = request.requestURI.removePrefix(request.contextPath).split('/').filter { it.isNotBlank() }
        val view = segments.firstOrNull()?.let(viewRegistry::get)

        if (view != null) {
            request.setAttribute(ACTIVE_VIEW_REQUEST_ATTRIBUTE, view)
        } else if (isViewRouteWithoutKnownView(segments, request.requestURI.endsWith('/'))) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun isViewRouteWithoutKnownView(
        segments: List<String>,
        hasTrailingSlash: Boolean,
    ): Boolean =
        segments.firstOrNull() in BLOCKED_UNPREFIXED_ROUTES ||
            (hasTrailingSlash && segments.size == 1 && segments.first() !in NON_VIEW_ROUTE_PREFIXES) ||
            (segments.firstOrNull() == "swagger-ui" && segments.getOrNull(1) == "index.html") ||
            (segments.size > 1 && segments[1] in PREFIXED_VIEW_ROUTES)

    companion object {
        private val BLOCKED_UNPREFIXED_ROUTES =
            setOf("sample", "component", "query", "llms.txt", "api-docs", "api-docs.yaml")
        private val PREFIXED_VIEW_ROUTES = setOf("sample", "component", "query", "llms.txt", "api-docs", "swagger-ui")
        private val NON_VIEW_ROUTE_PREFIXES = setOf("actuator", "error", "swagger-ui")
    }
}
