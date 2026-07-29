package org.genspectrum.lapis.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.controller.middleware.DATA_OPENNESS_AUTHORIZATION_FILTER_ORDER
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(DATA_OPENNESS_AUTHORIZATION_FILTER_ORDER - 1)
class ViewRoutingFilter(
    private val viewRegistry: ViewRegistry,
    @Value("\${lapis.legacyRoutesEnabled:false}") private val legacyRoutesEnabled: Boolean,
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
            val capability = requiredViewCapability(segments.drop(1))
            if (capability != null && !view.supports(capability)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND)
                return
            }
        } else if (legacyRoutesEnabled && segments.firstOrNull() in LEGACY_VIEW_ROUTES) {
            request.setAttribute(ACTIVE_VIEW_REQUEST_ATTRIBUTE, viewRegistry.first())
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
        private val LEGACY_VIEW_ROUTES = setOf("sample", "component", "query", "llms.txt", "api-docs", "swagger-ui")
        private val BLOCKED_UNPREFIXED_ROUTES = setOf("sample", "component", "query", "llms.txt", "api-docs")
        private val PREFIXED_VIEW_ROUTES = setOf("sample", "component", "query", "llms.txt", "api-docs", "swagger-ui")
        private val NON_VIEW_ROUTE_PREFIXES = setOf("actuator", "error", "swagger-ui")
    }
}

private val VIEW_CAPABILITIES_BY_SAMPLE_ROUTE = mapOf(
    "aggregated" to ViewCapability.METADATA,
    "details" to ViewCapability.METADATA,
    "nucleotideMutations" to ViewCapability.MUTATIONS,
    "aminoAcidMutations" to ViewCapability.MUTATIONS,
    "nucleotideInsertions" to ViewCapability.INSERTIONS,
    "aminoAcidInsertions" to ViewCapability.INSERTIONS,
    "alignedNucleotideSequences" to ViewCapability.SEQUENCES,
    "unalignedNucleotideSequences" to ViewCapability.SEQUENCES,
    "alignedAminoAcidSequences" to ViewCapability.SEQUENCES,
    "mostRecentCommonAncestor" to ViewCapability.PHYLO_TREE,
    "phyloSubtree" to ViewCapability.PHYLO_TREE,
)

internal fun requiredViewCapability(viewRelativeSegments: List<String>): ViewCapability? =
    when (viewRelativeSegments.firstOrNull()) {
        "component" -> ViewCapability.COMPONENTS
        "sample" -> VIEW_CAPABILITIES_BY_SAMPLE_ROUTE[viewRelativeSegments.getOrNull(1)]
        else -> null
    }
