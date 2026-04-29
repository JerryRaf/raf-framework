package com.raf.framework.core.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.raf.framework.core.util.DesensitizeUtil;
import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Tests for DesensitizeBeanSerializerModifier and DesensitizeJsonSerializer.
 * Exercises the Jackson serialization pipeline with sensitive POJO fields.
 *
 * @author Jerry
 * @since 2026-04-29
 */
class DesensitizeBeanSerializerModifierTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();

        DesensitizeBeanSerializerModifier modifier = new DesensitizeBeanSerializerModifier();
        mapper.setSerializerFactory(
                mapper.getSerializerFactory().withSerializerModifier(modifier)
        );

        DesensitizeMapFilter mapFilter = new DesensitizeMapFilter();
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter("desensitizeMapFilter", mapFilter);
        mapper.addMixIn(Map.class, DesensitizeMapFilter.class);
        mapper.setFilterProvider(filterProvider);
    }

    // ─── POJO with sensitive fields ──────────────────────────────────────────

    @Data
    static class UserDto {
        private String userId;
        private String mobile;
        private String password;
        private String email;
        private String name;
        private String idcard;
        private String bankcard;
        private String token;
    }

    @Test
    void serialize_masksMobileField() throws Exception {
        UserDto user = new UserDto();
        user.setUserId("u001");
        user.setMobile("13812348888");

        String json = mapper.writeValueAsString(user);
        Map<?, ?> result = mapper.readValue(json, Map.class);

        Assertions.assertEquals("u001", result.get("userId"));
        Assertions.assertNotEquals("13812348888", result.get("mobile"));
        Assertions.assertEquals("138****8888", result.get("mobile"));
    }

    @Test
    void serialize_masksPasswordWithFullMask() throws Exception {
        UserDto user = new UserDto();
        user.setPassword("mySecret123");

        String json = mapper.writeValueAsString(user);
        Map<?, ?> result = mapper.readValue(json, Map.class);

        Assertions.assertEquals("***", result.get("password"));
    }

    @Test
    void serialize_masksEmailField() throws Exception {
        UserDto user = new UserDto();
        user.setEmail("jerry@example.com");

        String json = mapper.writeValueAsString(user);
        Map<?, ?> result = mapper.readValue(json, Map.class);

        String maskedEmail = (String) result.get("email");
        Assertions.assertNotNull(maskedEmail);
        Assertions.assertNotEquals("jerry@example.com", maskedEmail);
        Assertions.assertTrue(maskedEmail.contains("@"));
    }

    @Test
    void serialize_masksNameField() throws Exception {
        UserDto user = new UserDto();
        user.setName("张三丰");

        String json = mapper.writeValueAsString(user);
        Map<?, ?> result = mapper.readValue(json, Map.class);

        Assertions.assertNotEquals("张三丰", result.get("name"));
    }

    @Test
    void serialize_masksIdcardField() throws Exception {
        UserDto user = new UserDto();
        user.setIdcard("123456789012345678");

        String json = mapper.writeValueAsString(user);
        Map<?, ?> result = mapper.readValue(json, Map.class);

        Assertions.assertNotEquals("123456789012345678", result.get("idcard"));
    }

    @Test
    void serialize_masksBankcardField() throws Exception {
        UserDto user = new UserDto();
        user.setBankcard("6222021234567890");

        String json = mapper.writeValueAsString(user);
        Map<?, ?> result = mapper.readValue(json, Map.class);

        Assertions.assertNotEquals("6222021234567890", result.get("bankcard"));
    }

    @Test
    void serialize_masksTokenField() throws Exception {
        UserDto user = new UserDto();
        user.setToken("abcdefghijklmnop");

        String json = mapper.writeValueAsString(user);
        Map<?, ?> result = mapper.readValue(json, Map.class);

        Assertions.assertNotEquals("abcdefghijklmnop", result.get("token"));
    }

    @Test
    void serialize_doesNotMaskNonSensitiveField() throws Exception {
        UserDto user = new UserDto();
        user.setUserId("user-123");

        String json = mapper.writeValueAsString(user);
        Map<?, ?> result = mapper.readValue(json, Map.class);

        Assertions.assertEquals("user-123", result.get("userId"));
    }

    @Test
    void serialize_handlesNullSensitiveField() throws Exception {
        UserDto user = new UserDto();
        user.setMobile(null);

        // Should not throw
        String json = mapper.writeValueAsString(user);
        Assertions.assertNotNull(json);
    }

    // ─── POJO with numeric sensitive field ───────────────────────────────────

    @Data
    static class OrderDto {
        private Long orderId;
        private Long mobile;  // numeric mobile (Long to hold 11-digit phone)
        private String userId;
    }

    @Test
    void serialize_masksNumericMobileField() throws Exception {
        OrderDto order = new OrderDto();
        order.setOrderId(1001L);
        order.setMobile(13812348888L);
        order.setUserId("u001");

        String json = mapper.writeValueAsString(order);
        Map<?, ?> result = mapper.readValue(json, Map.class);

        // mobile should be masked (not the original number)
        Assertions.assertNotEquals(13812348888L, result.get("mobile"));
        Assertions.assertEquals("u001", result.get("userId"));
    }

    // ─── Custom strategy registration ────────────────────────────────────────

    @Data
    static class SecretDto {
        private String secretCode;
        private String publicField;
    }

    @Test
    void serialize_usesCustomStrategy() throws Exception {
        DesensitizeUtil.addCustomStrategy("secretCode", DesensitizeUtil.DesensitizeStrategy.FULL);

        SecretDto dto = new SecretDto();
        dto.setSecretCode("ABC123");
        dto.setPublicField("visible");

        String json = mapper.writeValueAsString(dto);
        Map<?, ?> result = mapper.readValue(json, Map.class);

        Assertions.assertEquals("***", result.get("secretCode"));
        Assertions.assertEquals("visible", result.get("publicField"));

        DesensitizeUtil.resetToDefault();
    }

    // ─── Non-sensitive fields with various types (covers DesensitizeJsonSerializer branches) ──

    @Data
    static class MixedTypeDto {
        private String label;        // non-sensitive String
        private Integer count;       // non-sensitive Integer
        private Long bigCount;       // non-sensitive Long
        private Double ratio;        // non-sensitive Double
        private Float score;         // non-sensitive Float
        private Boolean active;      // non-sensitive Boolean
    }

    @Test
    void serialize_preservesNonSensitiveNumericAndBooleanFields() throws Exception {
        MixedTypeDto dto = new MixedTypeDto();
        dto.setLabel("test");
        dto.setCount(42);
        dto.setBigCount(1000000L);
        dto.setRatio(0.75);
        dto.setScore(9.5f);
        dto.setActive(true);

        String json = mapper.writeValueAsString(dto);
        Map<?, ?> result = mapper.readValue(json, Map.class);

        Assertions.assertEquals("test", result.get("label"));
        Assertions.assertEquals(42, result.get("count"));
        Assertions.assertEquals(1000000, ((Number) result.get("bigCount")).longValue());
        Assertions.assertEquals(true, result.get("active"));
    }
}
