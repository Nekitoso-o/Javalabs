package com.example.mangacatalog.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiCacheManagerTest {

    private ApiCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager = new ApiCacheManager();
    }

    @Test
    void get_whenMiss_returnsNull() {
        ApiCacheKey key = new ApiCacheKey("missing");
        Object result = cacheManager.get(key);
        assertThat(result).isNull();
    }

    @Test
    void get_whenHit_returnsValue() {
        ApiCacheKey key = new ApiCacheKey("key1");
        String value = "testValue";
        cacheManager.put(key, value);

        Object result = cacheManager.get(key);
        assertThat(result).isEqualTo(value);
    }

    @Test
    void put_storesValue() {
        ApiCacheKey key = new ApiCacheKey("key2");
        cacheManager.put(key, 42);
        assertThat(cacheManager.get(key)).isEqualTo(42);
    }

    @Test
    void invalidate_clearsCache() {
        ApiCacheKey key = new ApiCacheKey("key3");
        cacheManager.put(key, "value");
        cacheManager.invalidate();
        assertThat(cacheManager.get(key)).isNull();
    }

    @Test
    void invalidate_whenEmpty_doesNotThrow() {
        // Should not throw
        cacheManager.invalidate();
        assertThat(cacheManager.get(new ApiCacheKey("any"))).isNull();
    }
}