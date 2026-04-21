package com.example.mangacatalog.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ApiCacheManager {

    private static final Logger LOG = LoggerFactory.getLogger(ApiCacheManager.class);

    private final Map<ApiCacheKey, Object> cache = new ConcurrentHashMap<>();

    public Object get(final ApiCacheKey key) {
        Object result = cache.get(key);
        if (result != null) {
            LOG.info("Cache HIT");
        } else {
            LOG.info("Cache MISS");
        }
        return result;
    }

    public void put(final ApiCacheKey key, final Object value) {
        LOG.info("Cache PUT");
        cache.put(key, value);
    }

    public void invalidate() {
        LOG.info("Cache INVALIDATED - clearing all {} entries", cache.size());
        cache.clear();
    }
}