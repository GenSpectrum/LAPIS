package ch.ethz.lapis.api;

import ch.ethz.lapis.LapisMain;
import ch.ethz.lapis.api.controller.v1.SampleController;
import ch.ethz.lapis.api.entity.ApiCacheKey;
import ch.ethz.lapis.api.entity.req.GeneralConfig;
import ch.ethz.lapis.api.entity.req.MutationRequest;
import ch.ethz.lapis.api.entity.req.SampleAggregatedRequest;
import ch.ethz.lapis.util.SeqCompressor;
import ch.ethz.lapis.util.ZstdSeqCompressor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.args.FlushMode;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;


@Service
@Conditional(IsCacheEnabledCondition.class)
@Slf4j
public class CacheService {

    private static final Map<String, Class<?>> endpointToClass = new HashMap<>() {{
        put(SupportedEndpoints.SAMPLE_AGGREGATED, SampleAggregatedRequest.class);
        put(SupportedEndpoints.SAMPLE_AA_MUTATIONS, MutationRequest.class);
        put(SupportedEndpoints.SAMPLE_NUC_MUTATIONS, MutationRequest.class);
    }};
    private static final Map<String, BiConsumer<SampleController, Object>> endpointToPreComputation = new HashMap<>() {{
        put(SupportedEndpoints.SAMPLE_AGGREGATED, (sampleController, obj) -> {
            SampleAggregatedRequest request = (SampleAggregatedRequest) obj;
            sampleController.getAggregated(request, new GeneralConfig(), "");
        });
        put(SupportedEndpoints.SAMPLE_AA_MUTATIONS, (sampleController, obj) -> {
            MutationRequest request = (MutationRequest) obj;
            sampleController.getAAMutations(request, new GeneralConfig(), "");
        });
        put(SupportedEndpoints.SAMPLE_NUC_MUTATIONS, (sampleController, obj) -> {
            MutationRequest request = (MutationRequest) obj;
            sampleController.getNucMutations(request, new GeneralConfig(), "");
        });
    }};
    private final JedisPool pool = new JedisPool(LapisMain.globalConfig.getRedisHost(),
        LapisMain.globalConfig.getRedisPort());
    private final ObjectMapper objectMapper;
    private final SeqCompressor compressor;
    private final SampleController sampleController;
    public CacheService(@Lazy SampleController sampleController) {
        this.sampleController = sampleController;
        objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        compressor = new ZstdSeqCompressor(ZstdSeqCompressor.DICT.NONE);
    }

    public String getCompressedString(ApiCacheKey cacheKey) {
        String keyString = apiCacheKeyToString(cacheKey);
        try (Jedis jedis = pool.getResource()) {
            byte[] compressed = jedis.get(keyString.getBytes(StandardCharsets.UTF_8));
            if (compressed == null) {
                return null;
            }
            return compressor.decompress(compressed);
        }
    }

    public void setCompressedString(ApiCacheKey cacheKey, String value) {
        String keyString = apiCacheKeyToString(cacheKey);
        byte[] compressed = compressor.compress(value);
        try (Jedis jedis = pool.getResource()) {
            jedis.set(keyString.getBytes(StandardCharsets.UTF_8), compressed);
        }
    }

    public Long getLong(String cacheKey) {
        try (Jedis jedis = pool.getResource()) {
            String s = jedis.get(cacheKey);
            if (s == null) {
                return null;
            }
            return Long.parseLong(s);
        }
    }

    public void setLong(String cacheKey, long value) {
        try (Jedis jedis = pool.getResource()) {
            jedis.set(cacheKey, String.valueOf(value));
        }
    }

    /**
     * Clear the cache and recalculate values for all keys if the cache is out-dated.
     */
    public void updateCacheIfOutdated(long newDataVersion) {
        Long currentDataVersion = getCacheDataVersion();
        if (currentDataVersion != null && currentDataVersion == newDataVersion) {
            return;
        }
        try (Jedis jedis = pool.getResource()) {
            jedis.flushAll(FlushMode.SYNC);
        }
        setCacheDataVersion(newDataVersion);
    }

    private String apiCacheKeyToString(ApiCacheKey cacheKey) {
        try {
            return "api###" + cacheKey.getEndpointName() + "###"
                + objectMapper.writeValueAsString(cacheKey.getRequestObject());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Long getCacheDataVersion() {
        return getLong("general###data_version");
    }

    private void setCacheDataVersion(long dataVersion) {
        setLong("general###data_version", dataVersion);
    }

    public static final class SupportedEndpoints {
        public static final String SAMPLE_AGGREGATED = "/v0/sample/aggregated";
        public static final String SAMPLE_AA_MUTATIONS = "/v0/sample/aa-mutations";
        public static final String SAMPLE_NUC_MUTATIONS = "/v0/sample/nuc-mutations";
        public static final String SAMPLE_NUC_INSERTIONS = "/v0/sample/nuc-insertions";
        public static final String SAMPLE_AA_INSERTIONS = "/v0/sample/aa-insertions";
        public static final String SAMPLE_SQL_FOR_CHAT = "/v0/sample/sqlForChat";
    }

}
