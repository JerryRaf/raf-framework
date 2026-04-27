package com.raf.framework.redis.redisson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

/**
 * redis-starter 内部轻量 JSON 工具，避免强依赖 web-starter 的 JsonService。
 * 由 RedissonConfig 创建，复用 Spring 容器中的 ObjectMapper。
 */
public class RedisJsonHelper {

    private final ObjectMapper objectMapper;

    public RedisJsonHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialization error", e);
        }
    }

    public <T> T parse(String json, TypeReference<T> typeRef) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new RuntimeException("JSON deserialization error", e);
        }
    }
}
