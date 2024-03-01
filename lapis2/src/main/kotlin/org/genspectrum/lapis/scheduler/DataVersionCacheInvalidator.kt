package org.genspectrum.lapis.scheduler

import mu.KotlinLogging
import org.genspectrum.lapis.silo.CachedSiloClient
import org.genspectrum.lapis.silo.SILO_QUERY_CACHE_NAME
import org.springframework.cache.annotation.CacheEvict
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@Component
class DataVersionCacheInvalidator(
    private val cachedSiloClient: CachedSiloClient,
    private val cacheClearer: CacheClearer,
) {
    private var currentlyCachedDataVersion = "uninitialized"

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
    @Synchronized
    fun invalidateSiloCache() {
        log.debug { "checking for data version change" }

        val info = cachedSiloClient.callInfo()
        if (info.dataVersion != currentlyCachedDataVersion) {
            log.info {
                "Invalidating cache, old data version: $currentlyCachedDataVersion, " +
                    "new data version: ${info.dataVersion}"
            }
            cacheClearer.clearCache()
            currentlyCachedDataVersion = info.dataVersion
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
