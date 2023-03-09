package org.genspectrum.lapis.controller

import org.genspectrum.lapis.response.AggregatedResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class LapisController {
    @GetMapping("/aggregated")
    fun aggregated(): AggregatedResponse {
        return AggregatedResponse(0u)
    }
}
