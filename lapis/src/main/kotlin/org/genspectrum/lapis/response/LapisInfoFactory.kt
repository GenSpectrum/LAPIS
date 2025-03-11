package org.genspectrum.lapis.response

import jakarta.servlet.http.HttpServletRequest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.LapisVersion
import org.genspectrum.lapis.config.SiloVersion
import org.genspectrum.lapis.logging.RequestIdContext
import org.genspectrum.lapis.silo.DataVersion
import org.springframework.stereotype.Component
import java.net.URI

@Component
class LapisInfoFactory(
    private val dataVersion: DataVersion,
    private val requestIdContext: RequestIdContext,
    private val databaseConfig: DatabaseConfig,
    private val lapisVersion: LapisVersion,
    private val request: HttpServletRequest,
    private val siloVersion: SiloVersion,
) {
    fun create() =
        LapisInfo(
            dataVersion = dataVersion.dataVersion,
            requestId = requestIdContext.requestId,
            requestInfo = getRequestInfo(),
            lapisVersion = lapisVersion.version,
            siloVersion = siloVersion.version,
        )

    fun getRequestInfo() =
        "${databaseConfig.schema.instanceName} on ${URI(
            request.requestURL.toString(),
        ).host} at ${now()}"

    private fun now(): String = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
}
