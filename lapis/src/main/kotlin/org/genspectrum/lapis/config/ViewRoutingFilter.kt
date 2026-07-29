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
        val segments = request.requestURI.substringBefore('?').split('/').filter { it.isNotBlank() }
        val view = segments.firstOrNull()?.let(viewRegistry::get)

        if (view != null) {
            request.setAttribute(ACTIVE_VIEW_REQUEST_ATTRIBUTE, view)
            val capability = requiredCapability(segments.drop(1))
            if (capability != null && !view.supports(capability)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND)
                return
            }
        } else if (legacyRoutesEnabled && segments.firstOrNull() in LEGACY_VIEW_ROUTES) {
            request.setAttribute(ACTIVE_VIEW_REQUEST_ATTRIBUTE, viewRegistry.first())
        } else if (!legacyRoutesEnabled && isViewRouteWithoutKnownView(segments)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun isViewRouteWithoutKnownView(segments: List<String>): Boolean =
        segments.firstOrNull() in BLOCKED_UNPREFIXED_ROUTES ||
            (segments.firstOrNull() == "swagger-ui" && segments.getOrNull(1) == "index.html") ||
            (segments.size > 1 && segments[1] in PREFIXED_VIEW_ROUTES)

    private fun requiredCapability(viewRelativeSegments: List<String>): ViewCapability? {
        if (viewRelativeSegments.firstOrNull() == "component") {
            return ViewCapability.COMPONENTS
        }
        if (viewRelativeSegments.firstOrNull() != "sample") {
            return null
        }

        return when {
            viewRelativeSegments.any { it in METADATA_ROUTES } -> ViewCapability.METADATA
            viewRelativeSegments.any { it in MUTATION_ROUTES } -> ViewCapability.MUTATIONS
            viewRelativeSegments.any { it in INSERTION_ROUTES } -> ViewCapability.INSERTIONS
            viewRelativeSegments.any { it in SEQUENCE_ROUTES } -> ViewCapability.SEQUENCES
            viewRelativeSegments.any { it in PHYLO_TREE_ROUTES } -> ViewCapability.PHYLO_TREE
            else -> null
        }
    }

    companion object {
        private val LEGACY_VIEW_ROUTES = setOf("sample", "component", "query", "llms.txt", "api-docs", "swagger-ui")
        private val BLOCKED_UNPREFIXED_ROUTES = setOf("sample", "component", "query", "llms.txt", "api-docs")
        private val PREFIXED_VIEW_ROUTES = setOf("sample", "component", "query", "llms.txt", "api-docs", "swagger-ui")
        private val METADATA_ROUTES = setOf("aggregated", "details")
        private val MUTATION_ROUTES = setOf("nucleotideMutations", "aminoAcidMutations")
        private val INSERTION_ROUTES = setOf("nucleotideInsertions", "aminoAcidInsertions")
        private val SEQUENCE_ROUTES = setOf(
            "alignedNucleotideSequences",
            "unalignedNucleotideSequences",
            "alignedAminoAcidSequences",
        )
        private val PHYLO_TREE_ROUTES = setOf("mostRecentCommonAncestor", "phyloSubtree")
    }
}
