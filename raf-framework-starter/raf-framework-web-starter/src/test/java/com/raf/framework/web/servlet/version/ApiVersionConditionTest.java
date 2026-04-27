package com.raf.framework.web.servlet.version;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Tests for ApiVersionCondition.
 *
 * @author Jerry
 * @since 2026-04-27
 */
class ApiVersionConditionTest {

    @Test
    void shouldMatchWhenRequestVersionIsGreaterThanOrEqualToHandlerVersion() {
        ApiVersionCondition condition = new ApiVersionCondition(1);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v2/orders/list");

        ApiVersionCondition matched = condition.getMatchingCondition(request);

        Assertions.assertNotNull(matched);
        Assertions.assertEquals(1, matched.getApiVersion());
    }

    @Test
    void shouldNotMatchWhenRequestVersionIsLowerThanHandlerVersion() {
        ApiVersionCondition condition = new ApiVersionCondition(3);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v2/orders/list");

        ApiVersionCondition matched = condition.getMatchingCondition(request);

        Assertions.assertNull(matched);
    }

    @Test
    void shouldPreferHigherVersionInCompare() {
        ApiVersionCondition lower = new ApiVersionCondition(1);
        ApiVersionCondition higher = new ApiVersionCondition(2);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v2/orders/list");

        int result = higher.compareTo(lower, request);

        Assertions.assertTrue(result < 0);
    }
}
