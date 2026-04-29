package com.raf.framework.core.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tests for DesensitizeUtil and DesensitizeStrategy.
 *
 * @author Jerry
 * @since 2026-04-29
 */
class DesensitizeUtilTest {

    @AfterEach
    void tearDown() {
        DesensitizeUtil.resetToDefault();
    }

    // ─── isSensitiveField ───────────────────────────────────────────────────

    @Test
    void isSensitiveField_returnsTrueForKnownFields() {
        Assertions.assertTrue(DesensitizeUtil.isSensitiveField("password"));
        Assertions.assertTrue(DesensitizeUtil.isSensitiveField("mobile"));
        Assertions.assertTrue(DesensitizeUtil.isSensitiveField("email"));
        Assertions.assertTrue(DesensitizeUtil.isSensitiveField("idcard"));
        Assertions.assertTrue(DesensitizeUtil.isSensitiveField("bankcard"));
        Assertions.assertTrue(DesensitizeUtil.isSensitiveField("token"));
    }

    @Test
    void isSensitiveField_isCaseInsensitive() {
        Assertions.assertTrue(DesensitizeUtil.isSensitiveField("PASSWORD"));
        Assertions.assertTrue(DesensitizeUtil.isSensitiveField("Mobile"));
        Assertions.assertTrue(DesensitizeUtil.isSensitiveField("EMAIL"));
    }

    @Test
    void isSensitiveField_returnsFalseForUnknownField() {
        Assertions.assertFalse(DesensitizeUtil.isSensitiveField("userId"));
        Assertions.assertFalse(DesensitizeUtil.isSensitiveField("orderId"));
    }

    @Test
    void isSensitiveField_returnsFalseForNullOrEmpty() {
        Assertions.assertFalse(DesensitizeUtil.isSensitiveField(null));
        Assertions.assertFalse(DesensitizeUtil.isSensitiveField(""));
        Assertions.assertFalse(DesensitizeUtil.isSensitiveField("   "));
    }

    // ─── getDesensitizeStrategy ─────────────────────────────────────────────

    @Test
    void getDesensitizeStrategy_returnsCorrectStrategyForKnownFields() {
        Assertions.assertEquals(DesensitizeUtil.DesensitizeStrategy.FULL,
                DesensitizeUtil.getDesensitizeStrategy("password"));
        Assertions.assertEquals(DesensitizeUtil.DesensitizeStrategy.MOBILE,
                DesensitizeUtil.getDesensitizeStrategy("mobile"));
        Assertions.assertEquals(DesensitizeUtil.DesensitizeStrategy.EMAIL,
                DesensitizeUtil.getDesensitizeStrategy("email"));
        Assertions.assertEquals(DesensitizeUtil.DesensitizeStrategy.ID_CARD,
                DesensitizeUtil.getDesensitizeStrategy("idcard"));
        Assertions.assertEquals(DesensitizeUtil.DesensitizeStrategy.BANK_CARD,
                DesensitizeUtil.getDesensitizeStrategy("bankcard"));
        Assertions.assertEquals(DesensitizeUtil.DesensitizeStrategy.PARTIAL,
                DesensitizeUtil.getDesensitizeStrategy("token"));
    }

    @Test
    void getDesensitizeStrategy_returnsPartialForUnknownField() {
        Assertions.assertEquals(DesensitizeUtil.DesensitizeStrategy.PARTIAL,
                DesensitizeUtil.getDesensitizeStrategy("unknownField"));
    }

    @Test
    void getDesensitizeStrategy_returnsPartialForNull() {
        Assertions.assertEquals(DesensitizeUtil.DesensitizeStrategy.PARTIAL,
                DesensitizeUtil.getDesensitizeStrategy(null));
    }

    // ─── DesensitizeStrategy.PASSWORD / FULL ────────────────────────────────

    @Test
    void strategy_password_alwaysReturnsMask() {
        Assertions.assertEquals("***", DesensitizeUtil.DesensitizeStrategy.PASSWORD.apply("secret123"));
        Assertions.assertEquals("***", DesensitizeUtil.DesensitizeStrategy.PASSWORD.apply(null));
        Assertions.assertEquals("***", DesensitizeUtil.DesensitizeStrategy.FULL.apply("anything"));
    }

    // ─── DesensitizeStrategy.MOBILE ─────────────────────────────────────────

    @Test
    void strategy_mobile_masksMiddleFourDigits() {
        Assertions.assertEquals("138****8888", DesensitizeUtil.DesensitizeStrategy.MOBILE.apply("13812348888"));
    }

    @Test
    void strategy_mobile_returnsPlaceholderForNonElevenDigits() {
        Assertions.assertEquals("***", DesensitizeUtil.DesensitizeStrategy.MOBILE.apply("1234567"));
        Assertions.assertEquals("***", DesensitizeUtil.DesensitizeStrategy.MOBILE.apply("123456789012"));
    }

    @Test
    void strategy_mobile_returnsPlaceholderForNull() {
        Assertions.assertEquals("***", DesensitizeUtil.DesensitizeStrategy.MOBILE.apply(null));
    }

    // ─── DesensitizeStrategy.ID_CARD ────────────────────────────────────────

    @Test
    void strategy_idCard_masks15DigitId() {
        // 15位：前6位 + **** + 后3位（substring(12)）
        String result = DesensitizeUtil.DesensitizeStrategy.ID_CARD.apply("123456789012345");
        Assertions.assertEquals("123456****345", result);
    }

    @Test
    void strategy_idCard_masks18DigitId() {
        String result = DesensitizeUtil.DesensitizeStrategy.ID_CARD.apply("123456789012345678");
        Assertions.assertEquals("123456****5678", result);
    }

    @Test
    void strategy_idCard_returnsPlaceholderForShortId() {
        Assertions.assertEquals("***", DesensitizeUtil.DesensitizeStrategy.ID_CARD.apply("1234567"));
    }

    @Test
    void strategy_idCard_masksOtherLengthId() {
        // length=10, >= 8: first 6 + **** + last 4
        String result = DesensitizeUtil.DesensitizeStrategy.ID_CARD.apply("1234567890");
        Assertions.assertEquals("123456****7890", result);
    }

    @Test
    void strategy_idCard_returnsPlaceholderForNull() {
        Assertions.assertEquals("***", DesensitizeUtil.DesensitizeStrategy.ID_CARD.apply(null));
    }

    // ─── DesensitizeStrategy.BANK_CARD ──────────────────────────────────────

    @Test
    void strategy_bankCard_masksMiddleDigits() {
        String result = DesensitizeUtil.DesensitizeStrategy.BANK_CARD.apply("6222021234567890");
        Assertions.assertEquals("622202****7890", result);
    }

    @Test
    void strategy_bankCard_returnsPlaceholderForShortCard() {
        Assertions.assertEquals("***", DesensitizeUtil.DesensitizeStrategy.BANK_CARD.apply("1234567"));
    }

    @Test
    void strategy_bankCard_returnsPlaceholderForNull() {
        Assertions.assertEquals("***", DesensitizeUtil.DesensitizeStrategy.BANK_CARD.apply(null));
    }

    // ─── DesensitizeStrategy.TOKEN ──────────────────────────────────────────

    @Test
    void strategy_token_masksMiddlePart() {
        String result = DesensitizeUtil.DesensitizeStrategy.TOKEN.apply("abcdefghijklmnop");
        Assertions.assertEquals("abcd****mnop", result);
    }

    @Test
    void strategy_token_returnsPlaceholderForShortToken() {
        Assertions.assertEquals("***", DesensitizeUtil.DesensitizeStrategy.TOKEN.apply("short"));
        Assertions.assertEquals("***", DesensitizeUtil.DesensitizeStrategy.TOKEN.apply("12345678"));
    }

    @Test
    void strategy_token_returnsPlaceholderForNull() {
        Assertions.assertEquals("***", DesensitizeUtil.DesensitizeStrategy.TOKEN.apply(null));
    }

    // ─── DesensitizeStrategy.EMAIL ──────────────────────────────────────────

    @Test
    void strategy_email_masksLocalPart() {
        Assertions.assertEquals("je***@example.com",
                DesensitizeUtil.DesensitizeStrategy.EMAIL.apply("jerry@example.com"));
    }

    @Test
    void strategy_email_returnsPlaceholderWhenNoAtSign() {
        Assertions.assertEquals("***", DesensitizeUtil.DesensitizeStrategy.EMAIL.apply("notanemail"));
    }

    @Test
    void strategy_email_handlesShortLocalPart() {
        // atIndex <= 2: "a@b.com" -> "***@b.com"
        Assertions.assertEquals("***@b.com", DesensitizeUtil.DesensitizeStrategy.EMAIL.apply("a@b.com"));
    }

    @Test
    void strategy_email_returnsPlaceholderForNull() {
        Assertions.assertEquals("***", DesensitizeUtil.DesensitizeStrategy.EMAIL.apply(null));
    }

    // ─── DesensitizeStrategy.NAME ───────────────────────────────────────────

    @Test
    void strategy_name_masksSingleChar() {
        Assertions.assertEquals("*", DesensitizeUtil.DesensitizeStrategy.NAME.apply("张"));
    }

    @Test
    void strategy_name_masksTwoChars() {
        Assertions.assertEquals("张*", DesensitizeUtil.DesensitizeStrategy.NAME.apply("张三"));
    }

    @Test
    void strategy_name_masksThreeOrMoreChars() {
        Assertions.assertEquals("张**三", DesensitizeUtil.DesensitizeStrategy.NAME.apply("张小三"));
    }

    @Test
    void strategy_name_returnsEmptyForNullOrEmpty() {
        Assertions.assertEquals("", DesensitizeUtil.DesensitizeStrategy.NAME.apply(null));
        Assertions.assertEquals("", DesensitizeUtil.DesensitizeStrategy.NAME.apply(""));
    }

    // ─── DesensitizeStrategy.PARTIAL ────────────────────────────────────────

    @Test
    void strategy_partial_masksLongValue() {
        // length > 6: "***" + last 4
        Assertions.assertEquals("***5678", DesensitizeUtil.DesensitizeStrategy.PARTIAL.apply("12345678"));
    }

    @Test
    void strategy_partial_masksMiddleValue() {
        // length > 4 && <= 6: "***" + last 1
        Assertions.assertEquals("***5", DesensitizeUtil.DesensitizeStrategy.PARTIAL.apply("12345"));
    }

    @Test
    void strategy_partial_masksShortValue() {
        Assertions.assertEquals("***", DesensitizeUtil.DesensitizeStrategy.PARTIAL.apply("123"));
    }

    @Test
    void strategy_partial_returnsNullForNull() {
        Assertions.assertNull(DesensitizeUtil.DesensitizeStrategy.PARTIAL.apply(null));
    }

    // ─── addCustomStrategy / addSensitiveField ───────────────────────────────

    @Test
    void addCustomStrategy_registersNewFieldAndStrategy() {
        DesensitizeUtil.addCustomStrategy("customField", DesensitizeUtil.DesensitizeStrategy.FULL);

        Assertions.assertTrue(DesensitizeUtil.isSensitiveField("customField"));
        Assertions.assertEquals(DesensitizeUtil.DesensitizeStrategy.FULL,
                DesensitizeUtil.getDesensitizeStrategy("customField"));
    }

    @Test
    void addCustomStrategy_ignoresNullInputs() {
        // should not throw
        DesensitizeUtil.addCustomStrategy(null, DesensitizeUtil.DesensitizeStrategy.FULL);
        DesensitizeUtil.addCustomStrategy("field", null);
    }

    @Test
    void addSensitiveField_registersField() {
        DesensitizeUtil.addSensitiveField("mySecretField");
        Assertions.assertTrue(DesensitizeUtil.isSensitiveField("mySecretField"));
    }

    @Test
    void addSensitiveField_ignoresNullOrEmpty() {
        // should not throw
        DesensitizeUtil.addSensitiveField(null);
        DesensitizeUtil.addSensitiveField("");
        DesensitizeUtil.addSensitiveField("   ");
    }

    // ─── getSensitiveFields / getFieldStrategyMapping ───────────────────────

    @Test
    void getSensitiveFields_returnsDefensiveCopy() {
        Set<String> fields = DesensitizeUtil.getSensitiveFields();
        Assertions.assertNotNull(fields);
        Assertions.assertTrue(fields.contains("password"));
        // modifying returned set should not affect internal state
        fields.clear();
        Assertions.assertTrue(DesensitizeUtil.isSensitiveField("password"));
    }

    @Test
    void getFieldStrategyMapping_returnsDefensiveCopy() {
        Map<String, DesensitizeUtil.DesensitizeStrategy> mapping = DesensitizeUtil.getFieldStrategyMapping();
        Assertions.assertNotNull(mapping);
        Assertions.assertTrue(mapping.containsKey("password"));
    }

    // ─── toJSONString ────────────────────────────────────────────────────────

    @Test
    void toJSONString_returnsNullForNull() {
        Assertions.assertNull(DesensitizeUtil.toJSONString(null));
    }

    @Test
    void toJSONString_serializesSimpleObject() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", "123");
        String json = DesensitizeUtil.toJSONString(map);
        Assertions.assertNotNull(json);
        Assertions.assertTrue(json.contains("userId"));
    }

    // ─── desensitizeMap ──────────────────────────────────────────────────────

    @Test
    void desensitizeMap_returnsNullForNull() {
        Assertions.assertNull(DesensitizeUtil.desensitizeMap(null));
    }

    @Test
    void desensitizeMap_masksSensitiveKeys() {
        Map<String, Object> map = new HashMap<>();
        map.put("mobile", "13812348888");
        map.put("userId", "user001");

        Map<String, Object> result = DesensitizeUtil.desensitizeMap(map);

        Assertions.assertNotNull(result);
        // userId should remain unchanged
        Assertions.assertEquals("user001", result.get("userId"));
        // mobile should be masked
        String maskedMobile = (String) result.get("mobile");
        Assertions.assertNotNull(maskedMobile);
        Assertions.assertNotEquals("13812348888", maskedMobile);
    }

    // ─── resetToDefault ──────────────────────────────────────────────────────

    @Test
    void resetToDefault_restoresDefaultMappings() {
        DesensitizeUtil.addCustomStrategy("tempField", DesensitizeUtil.DesensitizeStrategy.FULL);
        Assertions.assertTrue(DesensitizeUtil.isSensitiveField("tempField"));

        DesensitizeUtil.resetToDefault();

        Assertions.assertFalse(DesensitizeUtil.isSensitiveField("tempField"));
        Assertions.assertTrue(DesensitizeUtil.isSensitiveField("password"));
    }
}
