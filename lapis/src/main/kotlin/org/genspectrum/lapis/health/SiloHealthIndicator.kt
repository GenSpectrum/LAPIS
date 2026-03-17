package org.genspectrum.lapis.health

import org.genspectrum.lapis.silo.CachedSiloClient
import org.genspectrum.lapis.silo.SiloNotReachableException
import org.genspectrum.lapis.silo.SiloUnavailableException
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.actuate.health.Status
import org.springframework.stereotype.Component

@Component
class SiloHealthIndicator(
    private val cachedSiloClient: CachedSiloClient,
) : HealthIndicator {
    override fun health(): Health =
        Health
            .up() // LAPIS should always be "up", independent of SILO.
            .let {
                try {
                    val info = cachedSiloClient.callInfo()
                    it
                        .withDetail("siloStatus", Status.UP.code)
                        .withDetail("dataVersion", info.dataVersion)
                        .withDetail("siloVersion", info.siloVersion ?: "unknown")
                } catch (_: SiloNotReachableException) {
                    it
                        .withDetail("siloStatus", Status.DOWN.code)
                        .withDetail("error", "SILO not reachable")
                } catch (e: SiloUnavailableException) {
                    it
                        .withDetail("siloStatus", Status.DOWN.code)
                        .withDetail("error", "SILO unavailable (HTTP 503)")
                        .withDetail("retryAfter", e.retryAfter)
                } catch (_: Exception) {
                    it
                        .withDetail("siloStatus", Status.DOWN.code)
                        .withDetail("error", "Unexpected error checking SILO")
                }
            }
            .build()
}
