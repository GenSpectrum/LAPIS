package org.genspectrum.lapis.response

import jakarta.servlet.http.HttpServletRequest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.LapisVersion
import org.genspectrum.lapis.config.RhyDbVersion
import org.genspectrum.lapis.logging.RequestIdContext
import org.genspectrum.lapis.rhydb.DataVersion
import org.springframework.stereotype.Component
import java.net.URI
import kotlin.time.Clock

@Component
class LapisInfoFactory(
    private val dataVersion: DataVersion,
    private val requestIdContext: RequestIdContext,
    private val databaseConfig: DatabaseConfig,
    private val lapisVersion: LapisVersion,
    private val request: HttpServletRequest,
    private val rhydbVersion: RhyDbVersion,
) {
    fun create() =
        LapisInfo(
            dataVersion = dataVersion.dataVersion,
            requestId = requestIdContext.requestId,
            requestInfo = getRequestInfo(),
            lapisVersion = lapisVersion.version,
            rhydbVersion = rhydbVersion.version,
        )

    fun getRequestInfo() =
        "${databaseConfig.schema.instanceName} on ${URI(
            request.requestURL.toString(),
        ).host} at ${now()}"

    private fun now(): String = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
}
