package com.raf.framework.core.jackson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Tests for JsonService.
 *
 * @author Jerry
 * @since 2026-04-29
 */
class JsonServiceTest {

    private JsonService jsonService;

    @BeforeEach
    void setUp() {
        jsonService = new JsonService(new ObjectMapper());
    }

    // ─── getObjectMapper ─────────────────────────────────────────────────────

    @Test
    void getObjectMapper_returnsNonNull() {
        Assertions.assertNotNull(jsonService.getObjectMapper());
    }

    // ─── toJson ──────────────────────────────────────────────────────────────

    @Test
    void toJson_returnsNullForNull() {
        Assertions.assertNull(jsonService.toJson(null));
    }

    @Test
    void toJson_serializesSimpleObject() {
        Map<String, Object> map = Map.of("key", "value");
        String json = jsonService.toJson(map);
        Assertions.assertNotNull(json);
        Assertions.assertTrue(json.contains("key"));
        Assertions.assertTrue(json.contains("value"));
    }

    // ─── parse(String, Class) ────────────────────────────────────────────────

    @Test
    void parse_returnsNullForBlankString() {
        Assertions.assertNull(jsonService.parse("", String.class));
        Assertions.assertNull(jsonService.parse("   ", String.class));
        Assertions.assertNull(jsonService.parse(null, String.class));
    }

    @Test
    void parse_deserializesJsonToClass() {
        String json = "{\"name\":\"Jerry\",\"age\":30}";
        Map result = jsonService.parse(json, Map.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("Jerry", result.get("name"));
    }

    @Test
    void parse_throwsRuntimeExceptionForInvalidJson() {
        Assertions.assertThrows(RuntimeException.class,
                () -> jsonService.parse("not-json", Map.class));
    }

    // ─── parse(String, TypeReference) ───────────────────────────────────────

    @Test
    void parse_withTypeRef_returnsNullForBlank() {
        Assertions.assertNull(jsonService.parse("", new TypeReference<Map<String, Object>>() {}));
    }

    @Test
    void parse_withTypeRef_deserializesCorrectly() {
        String json = "{\"id\":1}";
        Map<String, Object> result = jsonService.parse(json, new TypeReference<Map<String, Object>>() {});
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.get("id"));
    }

    // ─── toList ──────────────────────────────────────────────────────────────

    @Test
    void toList_returnsEmptyListForBlank() {
        List<String> result = jsonService.toList("", String.class);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void toList_deserializesJsonArray() {
        String json = "[\"a\",\"b\",\"c\"]";
        List<String> result = jsonService.toList(json, String.class);
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("a", result.get(0));
    }

    // ─── toMap ───────────────────────────────────────────────────────────────

    @Test
    void toMap_returnsEmptyMapForBlank() {
        Map<String, Object> result = jsonService.toMap("");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void toMap_deserializesJsonObject() {
        String json = "{\"foo\":\"bar\"}";
        Map<String, Object> result = jsonService.toMap(json);
        Assertions.assertEquals("bar", result.get("foo"));
    }

    // ─── toJsonDesensitizeNode ───────────────────────────────────────────────

    @Test
    void toJsonDesensitizeNode_returnsNullForBlank() {
        Assertions.assertNull(jsonService.toJsonDesensitizeNode(""));
        Assertions.assertNull(jsonService.toJsonDesensitizeNode(null));
    }

    @Test
    void toJsonDesensitizeNode_returnsJsonNode() {
        String json = "{\"x\":42}";
        JsonNode node = jsonService.toJsonDesensitizeNode(json);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(42, node.get("x").asInt());
    }
}
