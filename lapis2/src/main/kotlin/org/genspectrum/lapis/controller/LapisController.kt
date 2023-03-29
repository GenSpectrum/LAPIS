package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import org.genspectrum.lapis.model.AggregatedModel
import org.genspectrum.lapis.response.AggregatedResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

const val REQUEST_SCHEMA = "SequenceFilters"

@RestController
class LapisController(val aggregatedModel: AggregatedModel) {
    @GetMapping("/aggregated")
    fun aggregated(
        @Parameter(schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA"))
        @RequestParam
        filterParameter: Map<String, String>,
    ): AggregatedResponse {
        return aggregatedModel.handleRequest(filterParameter)
    }
}
