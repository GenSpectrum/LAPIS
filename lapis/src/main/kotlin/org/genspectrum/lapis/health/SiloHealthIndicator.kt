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
        Health
            .up() // LAPIS should always be "up", independent of SILO.
            .let {
                try {
                    val info = cachedSiloClient.callInfo()
                    it
                        .withDetail("dataVersion", info.dataVersion)
                        .withDetail("siloVersion", info.siloVersion ?: "unknown")
                } catch (e: SiloNotReachableException) {
                    it
                        .withDetail("siloStatus", "DOWN")
                        .withDetail("error", "SILO not reachable")
                        .withDetail("message", e.message)
                } catch (e: SiloUnavailableException) {
                    it
                        .withDetail("siloStatus", "DOWN")
                        .withDetail("error", "SILO unavailable (HTTP 503)")
                        .withDetail("message", e.message)
                        .withDetail("retryAfter", e.retryAfter)
                } catch (e: Exception) {
                    it
                        .withDetail("siloStatus", "DOWN")
                        .withDetail("error", "Unexpected error checking SILO")
                        .withDetail("message", e.message)
                }
            }
            .build()
}
