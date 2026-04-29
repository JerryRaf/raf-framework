package com.raf.framework.core.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for DateExtUtil.
 *
 * @author Jerry
 * @since 2026-04-29
 */
class DateExtUtilTest {

    @Test
    void gmtFormat_convertsValidGmtString() {
        // "Mon, 01 Jan 2024 10:30:00 GMT" -> "2024-01-01 10:30:00"
        String result = DateExtUtil.gmtFormat("Mon, 01 Jan 2024 10:30:00 GMT");
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertTrue(result.startsWith("2024-01-01"));
    }

    @Test
    void gmtFormat_returnsEmptyStringForInvalidInput() {
        String result = DateExtUtil.gmtFormat("not-a-date");
        Assertions.assertEquals("", result);
    }

    @Test
    void gmtFormat_returnsEmptyStringForNull() {
        String result = DateExtUtil.gmtFormat(null);
        Assertions.assertEquals("", result);
    }

    @Test
    void gmtFormat_returnsEmptyStringForEmptyString() {
        String result = DateExtUtil.gmtFormat("");
        Assertions.assertEquals("", result);
    }
}
