package com.raf.framework.core.jackson;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * JSON 处理服务
 */
@Slf4j
public class JsonService {

    private final ObjectMapper objectMapper;

    public JsonService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /* ======================== 序列化 ======================== */

    public String toJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new RuntimeException("JSON序列化异常", e);
        }
    }

    /* ======================== 反序列化 ======================== */

    public <T> T parse(String jsonStr, Class<T> clazz) {
        if (StringUtils.isBlank(jsonStr)) {
            return null;
        }
        try {
            return objectMapper.readValue(jsonStr, clazz);
        } catch (IOException e) {
            throw new RuntimeException("JSON解析异常 (Class)", e);
        }
    }

    public <T> T parse(String jsonStr, TypeReference<T> typeRef) {
        if (StringUtils.isBlank(jsonStr)) {
            return null;
        }
        try {
            return objectMapper.readValue(jsonStr, typeRef);
        } catch (IOException e) {
            throw new RuntimeException("JSON解析异常 (TypeRef)", e);
        }
    }

    public <T> List<T> toList(String jsonStr, Class<T> elementType) {
        if (StringUtils.isBlank(jsonStr)) {
            return Collections.emptyList();
        }
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructParametricType(List.class, elementType);
            return objectMapper.readValue(jsonStr, javaType);
        } catch (IOException e) {
            throw new RuntimeException("JSON转List异常", e);
        }
    }

    public Map<String, Object> toMap(String jsonStr) {
        if (StringUtils.isBlank(jsonStr)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("JSON转Map异常", e);
        }
    }

    public JsonNode toJsonDesensitizeNode(String jsonStr) {
        if (StringUtils.isBlank(jsonStr)) {
            return null;
        }
        try {
            return objectMapper.readTree(jsonStr);
        } catch (IOException e) {
            throw new RuntimeException("JSON转Node异常", e);
        }
    }
}
