package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Operation
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.ReferenceGenome
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.LapisInfo
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

const val INFO_ROUTE = "/info"
const val DATABASE_CONFIG_ROUTE = "/databaseConfig"
const val REFERENCE_GENOME_ROUTE = "/referenceGenome"

@RestController
@RequestMapping("/sample")
class InfoController(
    private val siloQueryModel: SiloQueryModel,
    private val databaseConfig: DatabaseConfig,
    private val referenceGenome: ReferenceGenome,
) {
    @GetMapping(INFO_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = INFO_ENDPOINT_DESCRIPTION)
    fun getInfo(): LapisInfo {
        val siloInfo = siloQueryModel.getInfo()
        return LapisInfo(siloInfo.dataVersion)
    }

    @GetMapping(DATABASE_CONFIG_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = DATABASE_CONFIG_ENDPOINT_DESCRIPTION)
    fun getDatabaseConfig(): DatabaseConfig = databaseConfig

    @GetMapping(REFERENCE_GENOME_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = REFERENCE_GENOME_ENDPOINT_DESCRIPTION)
    fun getReferenceGenome(): ReferenceGenome = referenceGenome
}
