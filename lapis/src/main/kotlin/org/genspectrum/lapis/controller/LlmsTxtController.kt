package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Hidden
import org.genspectrum.lapis.response.LlmsTxtGenerator
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for serving llms.txt file following the llms.txt specification (https://llmstxt.org/).
 * This provides LLM agents with information about the LAPIS instance, available endpoints,
 * metadata fields, and query examples.
 */
@Hidden
@RestController
class LlmsTxtController(
    private val llmsTxtGenerator: LlmsTxtGenerator,
) {
    @GetMapping("/llms.txt", produces = ["text/plain; charset=UTF-8"])
    fun getLlmsTxt(): String = llmsTxtGenerator.generate()
}
