package com.raf.framework.datasource;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DsSelector Annotation Test
 *
 * @author Jerry
 * @since 2026-04-20
 */
class DsSelectorTest {

    @Test
    void testAnnotationExists() {
        assertNotNull(DsSelector.class);
    }

    @Test
    void testAnnotationDefaultValue() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("defaultMethod");
        DsSelector annotation = method.getAnnotation(DsSelector.class);
        assertEquals(DataSourceContextHolder.DEFAULT_DS, annotation.value());
    }

    @Test
    void testAnnotationCustomValue() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("slaveMethod");
        DsSelector annotation = method.getAnnotation(DsSelector.class);
        assertEquals("slave", annotation.value());
    }

    static class TestService {
        @DsSelector
        public void defaultMethod() {}

        @DsSelector("slave")
        public void slaveMethod() {}
    }
}
