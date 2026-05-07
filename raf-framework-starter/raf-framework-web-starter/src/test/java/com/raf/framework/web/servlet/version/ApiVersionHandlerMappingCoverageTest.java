package com.raf.framework.web.servlet.version;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage tests for ApiVersionHandlerMapping.getMappingForMethod
 * and the private createRequestMappingInfo / resolvePaths methods.
 *
 * @author Jerry
 * @since 2026-05-07
 */
class ApiVersionHandlerMappingCoverageTest {

    private ApiVersionHandlerMapping mapping;

    @BeforeEach
    void setUp() throws Exception {
        mapping = new ApiVersionHandlerMapping();
        // Initialize the mapping with a minimal web application context
        StaticWebApplicationContext context = new StaticWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.refresh();
        mapping.setApplicationContext(context);
        mapping.afterPropertiesSet();
    }

    // ─── getMappingForMethod: method with @RequestMapping, no @ApiVersion ─────

    @Test
    void getMappingForMethod_noApiVersion_returnsStandardMapping() throws Exception {
        Method method = PlainController.class.getMethod("list");

        RequestMappingInfo info = mapping.getMappingForMethod(method, PlainController.class);

        assertNotNull(info);
    }

    // ─── getMappingForMethod: method with @ApiVersion on method ──────────────

    @Test
    void getMappingForMethod_apiVersionOnMethod_replacesVersionPlaceholder() throws Exception {
        Method method = VersionedMethodController.class.getMethod("getItem");

        RequestMappingInfo info = mapping.getMappingForMethod(method, VersionedMethodController.class);

        assertNotNull(info);
        // The path should contain "v2" (from @ApiVersion(2))
        String patterns = info.toString();
        assertTrue(patterns.contains("v2"), "Expected path to contain 'v2' but was: " + patterns);
    }

    // ─── getMappingForMethod: @ApiVersion on class ────────────────────────────

    @Test
    void getMappingForMethod_apiVersionOnClass_replacesVersionPlaceholder() throws Exception {
        Method method = VersionedClassController.class.getMethod("list");

        RequestMappingInfo info = mapping.getMappingForMethod(method, VersionedClassController.class);

        assertNotNull(info);
        String patterns = info.toString();
        assertTrue(patterns.contains("v3"), "Expected path to contain 'v3' but was: " + patterns);
    }

    // ─── getMappingForMethod: method without @RequestMapping returns null ─────

    @Test
    void getMappingForMethod_noRequestMapping_returnsNull() throws Exception {
        Method method = PlainController.class.getMethod("notMapped");

        RequestMappingInfo info = mapping.getMappingForMethod(method, PlainController.class);

        assertNull(info);
    }

    // ─── Helper controllers ───────────────────────────────────────────────────

    @RequestMapping("/items")
    static class PlainController {
        @GetMapping
        public void list() {}

        public void notMapped() {}
    }

    @RequestMapping("/{version}/orders")
    static class VersionedMethodController {
        @GetMapping("/{id}")
        @ApiVersion(2)
        public void getItem() {}
    }

    @RequestMapping("/{version}/products")
    @ApiVersion(3)
    static class VersionedClassController {
        @GetMapping
        public void list() {}
    }
}
