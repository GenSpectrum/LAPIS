package org.genspectrum.lapis.config

import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.webmvc.autoconfigure.WebMvcRegistrations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.web.servlet.mvc.condition.RequestCondition
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SegmentedViewController(
    val type: SegmentationType,
)

enum class SegmentationType {
    SINGLE,
    MULTI,
}

private data class ViewSegmentationRequestCondition(
    val type: SegmentationType,
    val viewRegistry: ViewRegistry,
) : RequestCondition<ViewSegmentationRequestCondition> {
    override fun combine(other: ViewSegmentationRequestCondition) = other

    override fun getMatchingCondition(request: HttpServletRequest): ViewSegmentationRequestCondition? {
        val view = request.getAttribute(ACTIVE_VIEW_REQUEST_ATTRIBUTE) as? ViewConfig ?: viewRegistry.first()
        val matches = when (type) {
            SegmentationType.SINGLE -> view.referenceGenomeSchema.isSingleSegmented()
            SegmentationType.MULTI -> !view.referenceGenomeSchema.isSingleSegmented()
        }
        return if (matches) this else null
    }

    override fun compareTo(
        other: ViewSegmentationRequestCondition,
        request: HttpServletRequest,
    ) = 0
}

@Configuration
class ViewSegmentationWebMvcConfig {
    @Bean
    fun viewAwareWebMvcRegistrations(viewRegistry: ViewRegistry) =
        object : WebMvcRegistrations {
            override fun getRequestMappingHandlerMapping() =
                object : RequestMappingHandlerMapping() {
                    override fun getCustomTypeCondition(handlerType: Class<*>): RequestCondition<*>? =
                        AnnotatedElementUtils.findMergedAnnotation(handlerType, SegmentedViewController::class.java)
                            ?.let { ViewSegmentationRequestCondition(it.type, viewRegistry) }
                }
        }
}
