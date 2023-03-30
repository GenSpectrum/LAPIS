package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.Explode
import io.swagger.v3.oas.annotations.enums.ParameterStyle
import io.swagger.v3.oas.annotations.media.Schema
import org.genspectrum.lapis.model.AggregatedModel
import org.genspectrum.lapis.response.AggregatedResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

const val REQUEST_SCHEMA = "SequenceFilters"

@RestController
class LapisController(val aggregatedModel: AggregatedModel) {
    @GetMapping("/aggregated")
    fun aggregated(
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA"),
            explode = Explode.TRUE,
            style = ParameterStyle.FORM,
        )
        @RequestParam
        sequenceFilters: Map<String, String>,
    ): AggregatedResponse {
        return aggregatedModel.handleRequest(sequenceFilters)
    }

    @PostMapping("/aggregated")
    fun postAggregated(
        @Parameter(schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA"))
        @RequestBody
        sequenceFilters: Map<String, String>,
    ): AggregatedResponse {
        return aggregatedModel.handleRequest(sequenceFilters)
    }
}
