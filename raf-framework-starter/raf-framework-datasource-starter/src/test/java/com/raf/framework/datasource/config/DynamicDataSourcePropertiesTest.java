package com.raf.framework.datasource.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DynamicDataSourceProperties Test
 *
 * @author Jerry
 * @since 2026-04-20
 */
class DynamicDataSourcePropertiesTest {

    @Test
    void testDefaultValues() {
        DynamicDataSourceProperties properties = new DynamicDataSourceProperties();
        assertFalse(properties.isEnabled());
        assertEquals("master", properties.getPrimary());
        assertTrue(properties.getDatasources().isEmpty());
    }

    @Test
    void testSetEnabled() {
        DynamicDataSourceProperties properties = new DynamicDataSourceProperties();
        properties.setEnabled(true);
        assertTrue(properties.isEnabled());
    }

    @Test
    void testSetPrimary() {
        DynamicDataSourceProperties properties = new DynamicDataSourceProperties();
        properties.setPrimary("cms-master");
        assertEquals("cms-master", properties.getPrimary());
    }

    @Test
    void testSetDatasources() {
        DynamicDataSourceProperties properties = new DynamicDataSourceProperties();
        properties.setDatasources(Arrays.asList("cms-master", "cms-slave"));
        assertEquals(2, properties.getDatasources().size());
        assertTrue(properties.getDatasources().contains("cms-master"));
        assertTrue(properties.getDatasources().contains("cms-slave"));
    }
}
