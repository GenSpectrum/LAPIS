package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Operation
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.LapisInfo
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

const val INFO_ROUTE = "/info"

@RestController
@RequestMapping("/sample")
class InfoController(private val siloQueryModel: SiloQueryModel) {
    @GetMapping(INFO_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = INFO_ENDPOINT_DESCRIPTION)
    fun getInfo(): LapisInfo {
        val siloInfo = siloQueryModel.getInfo()
        return LapisInfo(siloInfo.dataVersion)
    }
}
