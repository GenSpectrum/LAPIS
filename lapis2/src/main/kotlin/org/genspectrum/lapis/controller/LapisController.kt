package org.genspectrum.lapis.controller

import org.genspectrum.lapis.model.AggregatedModel
import org.genspectrum.lapis.response.AggregatedResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class LapisController(val aggregatedModel: AggregatedModel) {
    @GetMapping("/aggregated")
    fun aggregated(
        @RequestParam filterParameter: Map<String, String>,
    ): AggregatedResponse {
        return aggregatedModel.handleRequest(filterParameter)
    }
}
