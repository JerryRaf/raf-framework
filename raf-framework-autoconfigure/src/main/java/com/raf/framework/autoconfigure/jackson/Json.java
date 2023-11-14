package com.raf.framework.autoconfigure.jackson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public class Json {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 将对象转换为json字符串
     */
    public <T> String objToString(T t) {
        try {
            return OBJECT_MAPPER.writeValueAsString(t);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @param t
     * @param serializationView
     * @param <T>
     * @return
     */
    public <T> String objToString(T t, Class<?> serializationView) {
        try {
            return OBJECT_MAPPER.writerWithView(serializationView).writeValueAsString(t);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * json字符串转对象
     * @param jsonStr
     * @param typeReference
     * @param <T>
     * @return
     */
    public <T> T strToObj(String jsonStr, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(jsonStr, typeReference);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * json字符串转单个对象
     */
    public <T> T strToObj(String jsonStr, Class<T> cls) {
        try {
            return OBJECT_MAPPER.readValue(jsonStr, cls);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 将字符串转list对象
     */
    public <T> List<T> strToList(String jsonStr, Class<T> cls) {
        try {
            JavaType t = OBJECT_MAPPER.getTypeFactory().constructParametricType(
                    List.class, cls);
            return OBJECT_MAPPER.readValue(jsonStr, t);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 将字符串转为json节点
     */
    public JsonNode strToNode(String jsonStr) {
        try {
            return OBJECT_MAPPER.readTree(jsonStr);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public <T> T readAs(byte[] bytes, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(bytes, typeReference);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
