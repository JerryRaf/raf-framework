package com.raf.framework.core.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for RegUtil.
 *
 * @author Jerry
 * @since 2026-04-29
 */
class RegUtilTest {

    @Test
    void isStrongPwd_returnsTrueForValidPassword() {
        // 字母 + 数字
        Assertions.assertTrue(RegUtil.isStrongPwd("abc123"));
        // 字母 + 符号
        Assertions.assertTrue(RegUtil.isStrongPwd("abc!@#"));
        // 数字 + 符号
        Assertions.assertTrue(RegUtil.isStrongPwd("123!@#"));
        // 三种混合
        Assertions.assertTrue(RegUtil.isStrongPwd("Abc123!"));
    }

    @Test
    void isStrongPwd_returnsFalseForAllDigits() {
        Assertions.assertFalse(RegUtil.isStrongPwd("123456"));
    }

    @Test
    void isStrongPwd_returnsFalseForAllLetters() {
        Assertions.assertFalse(RegUtil.isStrongPwd("abcdef"));
    }

    @Test
    void isStrongPwd_returnsFalseForAllSymbols() {
        Assertions.assertFalse(RegUtil.isStrongPwd("!@#$%^"));
    }

    @Test
    void isStrongPwd_returnsFalseForTooShort() {
        Assertions.assertFalse(RegUtil.isStrongPwd("Ab1"));
    }

    @Test
    void isStrongPwd_returnsFalseForTooLong() {
        Assertions.assertFalse(RegUtil.isStrongPwd("Abc123456789012345678"));
    }

    @Test
    void isStrongPwd_returnsFalseForContainingChineseChars() {
        Assertions.assertFalse(RegUtil.isStrongPwd("abc123中文"));
    }

    @Test
    void isStrongPwd_returnsFalseForContainingSpace() {
        Assertions.assertFalse(RegUtil.isStrongPwd("abc 123"));
    }
}
