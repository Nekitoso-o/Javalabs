package com.example.mangacatalog.cache;

import java.util.Arrays;
import java.util.Objects;

public class ApiCacheKey {
    private final String operation;
    private final Object[] params;

    public ApiCacheKey(String operation, Object... params) {
        this.operation = operation;
        this.params = params;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ApiCacheKey that = (ApiCacheKey) o;
        return Objects.equals(operation, that.operation) && Arrays.equals(params, that.params);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(operation);
        result = 31 * result + Arrays.hashCode(params);
        return result;
    }

    @Override
    public String toString() {
        return "ApiCacheKey{" +
            "operation='" + operation + '\'' +
            ", params=" + Arrays.toString(params) +
            '}';
    }
}