package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.LapisVersion
import org.genspectrum.lapis.config.ReferenceGenome
import org.genspectrum.lapis.controller.LapisMediaType.APPLICATION_YAML_VALUE
import org.genspectrum.lapis.controller.middleware.getRequestInfo
import org.genspectrum.lapis.logging.RequestIdContext
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.response.LapisInfo
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

const val INFO_ROUTE = "/info"
const val DATABASE_CONFIG_ROUTE = "/databaseConfig"
const val REFERENCE_GENOME_ROUTE = "/referenceGenome"

@RestController
@RequestMapping("/sample")
class InfoController(
    private val siloQueryModel: SiloQueryModel,
    private val databaseConfig: DatabaseConfig,
    private val referenceGenome: ReferenceGenome,
    private val lapisVersion: LapisVersion,
    private val requestIdContext: RequestIdContext,
) {
    @GetMapping(INFO_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = INFO_ENDPOINT_DESCRIPTION)
    fun getInfo(request: HttpServletRequest): LapisInfo {
        val siloInfo = siloQueryModel.getInfo()
        return LapisInfo(
            dataVersion = siloInfo.dataVersion,
            lapisVersion = lapisVersion.version,
            requestId = requestIdContext.requestId,
            requestInfo = getRequestInfo(databaseConfig, URI(request.requestURI)),
        )
    }

    @GetMapping(DATABASE_CONFIG_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML_VALUE])
    @Operation(description = DATABASE_CONFIG_ENDPOINT_DESCRIPTION)
    fun getDatabaseConfigAsJson(): DatabaseConfig = databaseConfig

    @GetMapping(REFERENCE_GENOME_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = REFERENCE_GENOME_ENDPOINT_DESCRIPTION)
    fun getReferenceGenome(): ReferenceGenome = referenceGenome
}
