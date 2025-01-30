package org.genspectrum.lapis.scheduler

import mu.KotlinLogging
import org.genspectrum.lapis.config.SiloVersion
import org.genspectrum.lapis.response.InfoData
import org.genspectrum.lapis.silo.CachedSiloClient
import org.genspectrum.lapis.silo.SILO_QUERY_CACHE_NAME
import org.genspectrum.lapis.silo.SiloUnavailableException
import org.springframework.cache.annotation.CacheEvict
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@Component
class DataVersionCacheInvalidator(
    private val cachedSiloClient: CachedSiloClient,
    private val cacheClearer: CacheClearer,
    private val siloVersion: SiloVersion,
) {
    private var currentlyCachedDataVersion = "uninitialized"

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
    @Synchronized
    fun invalidateSiloCache() {
        log.debug { "checking for data version change" }

        val info = try {
            cachedSiloClient.callInfo()
        } catch (e: SiloUnavailableException) {
            log.debug { "Caught ${SiloUnavailableException::class.java} $e" }
            InfoData(
                dataVersion = "currently unavailable",
                siloVersion = null,
            )
        } catch (e: Exception) {
            log.debug { "Failed to call info: $e" }
            return
        }
        if (info.dataVersion != currentlyCachedDataVersion) {
            log.info {
                "Invalidating cache, old data version: $currentlyCachedDataVersion, " +
                    "new data version: ${info.dataVersion}"
            }
            cacheClearer.clearCache()
            currentlyCachedDataVersion = info.dataVersion
            siloVersion.version = info.siloVersion
        }
    }
}

@Component
class CacheClearer {
    @CacheEvict(SILO_QUERY_CACHE_NAME, allEntries = true)
    fun clearCache() {
        log.info { "Clearing cache $SILO_QUERY_CACHE_NAME" }
    }
}
