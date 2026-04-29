package com.example.mangacatalog.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiCacheKeyTest {

    @Test
    void testEqualsAndHashCode_sameOperationAndParams() {
        ApiCacheKey key1 = new ApiCacheKey("op", 1L, "str");
        ApiCacheKey key2 = new ApiCacheKey("op", 1L, "str");

        assertThat(key1).isEqualTo(key2);
        assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
    }

    @Test
    void testEquals_sameObject() {
        ApiCacheKey key = new ApiCacheKey("op", 1L);
        assertThat(key).isEqualTo(key);
    }

    @Test
    void testEquals_null() {
        ApiCacheKey key = new ApiCacheKey("op", 1L);
        assertThat(key).isNotEqualTo(null);
    }

    @Test
    void testEquals_differentClass() {
        ApiCacheKey key = new ApiCacheKey("op", 1L);
        assertThat(key).isNotEqualTo("string");
    }

    @Test
    void testEquals_differentOperation() {
        ApiCacheKey key1 = new ApiCacheKey("op1", 1L);
        ApiCacheKey key2 = new ApiCacheKey("op2", 1L);
        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void testEquals_differentParams() {
        ApiCacheKey key1 = new ApiCacheKey("op", 1L);
        ApiCacheKey key2 = new ApiCacheKey("op", 2L);
        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void testEquals_nullParams() {
        ApiCacheKey key1 = new ApiCacheKey("op", (Object[]) null);
        ApiCacheKey key2 = new ApiCacheKey("op");
        assertThat(key1).isEqualTo(key2);
    }

    @Test
    void testToString() {
        ApiCacheKey key = new ApiCacheKey("getAllComics");
        assertThat(key.toString()).contains("getAllComics");
    }

    @Test
    void testHashCode_differentOperations() {
        ApiCacheKey key1 = new ApiCacheKey("op1");
        ApiCacheKey key2 = new ApiCacheKey("op2");
        assertThat(key1.hashCode()).isNotEqualTo(key2.hashCode());
    }
}