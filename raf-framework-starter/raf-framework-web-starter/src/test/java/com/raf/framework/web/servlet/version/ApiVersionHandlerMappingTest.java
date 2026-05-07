package com.raf.framework.web.servlet.version;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

import java.lang.reflect.Method;

/**
 * Tests for ApiVersionHandlerMapping.
 *
 * @author Jerry
 * @since 2026-04-29
 */
class ApiVersionHandlerMappingTest {

    private final ApiVersionHandlerMapping mapping = new ApiVersionHandlerMapping();

    // ─── getCustomTypeCondition ───────────────────────────────────────────────

    @Test
    void getCustomTypeCondition_withApiVersionAnnotation_returnsCondition() throws Exception {
        @ApiVersion(2)
        class VersionedController {}

        RequestCondition<?> condition = mapping.getCustomTypeCondition(VersionedController.class);

        Assertions.assertNotNull(condition);
        Assertions.assertInstanceOf(ApiVersionCondition.class, condition);
        Assertions.assertEquals(2, ((ApiVersionCondition) condition).getApiVersion());
    }

    @Test
    void getCustomTypeCondition_withoutApiVersionAnnotation_returnsNull() {
        class PlainController {}

        RequestCondition<?> condition = mapping.getCustomTypeCondition(PlainController.class);

        Assertions.assertNull(condition);
    }

    // ─── getCustomMethodCondition ─────────────────────────────────────────────

    @Test
    void getCustomMethodCondition_withApiVersionAnnotation_returnsCondition() throws Exception {
        class Controller {
            @ApiVersion(3)
            public void handle() {}
        }

        Method method = Controller.class.getMethod("handle");
        RequestCondition<?> condition = mapping.getCustomMethodCondition(method);

        Assertions.assertNotNull(condition);
        Assertions.assertInstanceOf(ApiVersionCondition.class, condition);
        Assertions.assertEquals(3, ((ApiVersionCondition) condition).getApiVersion());
    }

    @Test
    void getCustomMethodCondition_withoutApiVersionAnnotation_returnsNull() throws Exception {
        class Controller {
            public void handle() {}
        }

        Method method = Controller.class.getMethod("handle");
        RequestCondition<?> condition = mapping.getCustomMethodCondition(method);

        Assertions.assertNull(condition);
    }

    // ─── ApiVersionCondition.combine ─────────────────────────────────────────

    @Test
    void apiVersionCondition_combine_usesMethodLevelVersion() {
        ApiVersionCondition typeLevel = new ApiVersionCondition(1);
        ApiVersionCondition methodLevel = new ApiVersionCondition(3);

        ApiVersionCondition combined = typeLevel.combine(methodLevel);

        Assertions.assertEquals(3, combined.getApiVersion());
    }

    // ─── ApiVersionCondition.getMatchingCondition ─────────────────────────────

    @Test
    void apiVersionCondition_getMatchingCondition_exactMatch() {
        ApiVersionCondition condition = new ApiVersionCondition(2);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v2/orders");

        ApiVersionCondition matched = condition.getMatchingCondition(request);

        Assertions.assertNotNull(matched);
    }

    @Test
    void apiVersionCondition_getMatchingCondition_noVersionInUri_returnsNull() {
        ApiVersionCondition condition = new ApiVersionCondition(1);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/orders");

        ApiVersionCondition matched = condition.getMatchingCondition(request);

        Assertions.assertNull(matched);
    }
}
