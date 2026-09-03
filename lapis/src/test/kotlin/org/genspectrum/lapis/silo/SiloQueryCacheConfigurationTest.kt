package org.genspectrum.lapis.silo

import com.github.benmanes.caffeine.cache.Cache
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import java.util.concurrent.TimeUnit

@SpringBootTest
class SiloQueryCacheConfigurationTest(
    @param:Autowired private val cacheManager: CacheManager,
) {
    @Test
    fun `siloQueryCache entries expire after access`() {
        @Suppress("UNCHECKED_CAST")
        val nativeCache = cacheManager.getCache(SILO_QUERY_CACHE_NAME)!!.nativeCache as Cache<Any, Any>

        assertThat(
            nativeCache.policy().expireAfterAccess().get().getExpiresAfter(TimeUnit.HOURS),
            equalTo(30L),
        )
    }
}
