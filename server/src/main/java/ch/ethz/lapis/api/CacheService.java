package ch.ethz.lapis.api;

import ch.ethz.lapis.api.entity.ApiCacheKey;
import ch.ethz.lapis.util.DeflateSeqCompressor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.nio.charset.StandardCharsets;


@Service
public class CacheService {

    private final JedisPool pool = new JedisPool("127.0.0.1", 6789);
    private final ObjectMapper objectMapper;
    private final DeflateSeqCompressor compressor;


    public CacheService() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        compressor = new DeflateSeqCompressor(DeflateSeqCompressor.DICT.NONE);
    }


    public String getCompressedString(ApiCacheKey cacheKey) {
        try {
            String keyString = objectMapper.writeValueAsString(cacheKey);
            try (Jedis jedis = pool.getResource()) {
                byte[] compressed = jedis.get(keyString.getBytes(StandardCharsets.UTF_8));
                if (compressed == null) {
                    return null;
                }
                return compressor.decompress(compressed);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    public void setCompressedString(ApiCacheKey cacheKey, String value) {
        try {
            String keyString = objectMapper.writeValueAsString(cacheKey);
            byte[] compressed = compressor.compress(value);
            try (Jedis jedis = pool.getResource()) {
                jedis.set(keyString.getBytes(StandardCharsets.UTF_8), compressed);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
