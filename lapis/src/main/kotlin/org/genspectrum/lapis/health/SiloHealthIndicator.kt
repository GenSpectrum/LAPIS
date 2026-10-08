package org.genspectrum.lapis.health

import org.genspectrum.lapis.rhydb.CachedRhyDbClient
import org.genspectrum.lapis.rhydb.RhyDbNotReachableException
import org.genspectrum.lapis.rhydb.RhyDbTimeoutException
import org.genspectrum.lapis.rhydb.RhyDbUnavailableException
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.boot.health.contributor.Status
import org.springframework.stereotype.Component
import java.time.Duration

private val HEALTH_CHECK_TIMEOUT = Duration.ofMillis(100)

@Component
class SiloHealthIndicator(
    private val cachedRhyDbClient: CachedRhyDbClient,
) : HealthIndicator {
    override fun health(): Health =
        Health
            .up() // LAPIS should always be "up", independent of SILO.
            .let {
                try {
                    val info = cachedRhyDbClient.callInfo(HEALTH_CHECK_TIMEOUT)
                    it
                        .withDetail("siloStatus", Status.UP)
                        .withDetail("dataVersion", info.dataVersion)
                        .withDetail("siloVersion", info.siloVersion ?: "unknown")
                } catch (_: RhyDbNotReachableException) {
                    it
                        .withDetail("siloStatus", Status.DOWN)
                        .withDetail("error", "SILO not reachable")
                } catch (_: RhyDbTimeoutException) {
                    // not DOWN: exceeding a 100ms budget says nothing about whether SILO is alive
                    it
                        .withDetail("siloStatus", Status.UNKNOWN)
                        .withDetail("error", "SILO did not answer within the health check timeout")
                } catch (e: RhyDbUnavailableException) {
                    it
                        .withDetail("siloStatus", Status.DOWN)
                        .withDetail("error", "SILO unavailable (HTTP 503)")
                        .withDetail("retryAfter", e.retryAfter ?: "unknown")
                } catch (_: Exception) {
                    it
                        .withDetail("siloStatus", Status.DOWN)
                        .withDetail("error", "Unexpected error checking SILO")
                }
            }
            .build()
}
