package org.genspectrum.lapis.health

import org.genspectrum.lapis.silo.CachedSiloClient
import org.genspectrum.lapis.silo.SiloNotReachableException
import org.genspectrum.lapis.silo.SiloUnavailableException
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class SiloHealthIndicator(
    private val cachedSiloClient: CachedSiloClient,
) : HealthIndicator {
    override fun health(): Health =
        try {
            val info = cachedSiloClient.callInfo()
            Health
                .up()
                .withDetail("dataVersion", info.dataVersion)
                .withDetail("siloVersion", info.siloVersion ?: "unknown")
                .build()
        } catch (e: SiloNotReachableException) {
            Health
                .down()
                .withDetail("error", "SILO not reachable")
                .withDetail("message", e.message)
                .withException(e)
                .build()
        } catch (e: SiloUnavailableException) {
            Health
                .down()
                .withDetail("error", "SILO unavailable (HTTP 503)")
                .withDetail("message", e.message)
                .withDetail("retryAfter", e.retryAfter)
                .withException(e)
                .build()
        } catch (e: Exception) {
            Health
                .down()
                .withDetail("error", "Unexpected error checking SILO")
                .withDetail("message", e.message)
                .withException(e)
                .build()
        }
}
