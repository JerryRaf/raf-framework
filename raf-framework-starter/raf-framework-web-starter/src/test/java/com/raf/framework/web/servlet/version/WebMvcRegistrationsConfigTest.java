package com.raf.framework.web.servlet.version;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for WebMvcRegistrationsConfig.
 *
 * @author Jerry
 * @since 2026-04-29
 */
class WebMvcRegistrationsConfigTest {

    @Test
    void getRequestMappingHandlerMapping_returnsApiVersionHandlerMapping() {
        WebMvcRegistrationsConfig config = new WebMvcRegistrationsConfig();
        var mapping = config.getRequestMappingHandlerMapping();

        Assertions.assertNotNull(mapping);
        Assertions.assertInstanceOf(ApiVersionHandlerMapping.class, mapping);
    }

    @Test
    void getRequestMappingHandlerMapping_hasOrderZero() {
        WebMvcRegistrationsConfig config = new WebMvcRegistrationsConfig();
        var mapping = config.getRequestMappingHandlerMapping();

        Assertions.assertEquals(0, mapping.getOrder());
    }
}
