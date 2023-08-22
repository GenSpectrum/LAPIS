package org.genspectrum.lapis.controller

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.genspectrum.lapis.config.LapisApplicationProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

private val log = KotlinLogging.logger {}

/**
 * This is necessary in case of using a base url. The Swagger UI will use a link with the base url.
 * The proxy will however strip it away, so that it will call a route without that base url.
 */
@Controller
@ConditionalOnProperty("lapis.base-url")
class ApiDocsForwardController(private val applicationProperties: LapisApplicationProperties) {
    @RequestMapping(
        "\${lapis.api-docs-postfix}",
        "\${lapis.api-docs-postfix}.yaml",
        "\${lapis.api-docs-postfix}/**",
    )
    fun forward(request: HttpServletRequest): String {
        val requestURI = request.requestURI
        val forwardUrl = applicationProperties.baseUrl + requestURI

        log.debug { "Forwarding from $requestURI to $forwardUrl" }

        return "forward:$forwardUrl"
    }
}
