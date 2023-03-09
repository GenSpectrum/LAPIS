package org.genspectrum.lapis.controller

import org.genspectrum.lapis.response.AggregatedResponse
import org.genspectrum.lapis.silo.SiloAction
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.SiloQuery
import org.genspectrum.lapis.silo.StringEquals
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class LapisController(val siloClient: SiloClient) {
    @GetMapping("/aggregated")
    fun aggregated(): AggregatedResponse {
        return siloClient.sendQuery(
            SiloQuery(SiloAction.aggregated(), StringEquals("theColumn", "theValue"))
        )
    }
}
