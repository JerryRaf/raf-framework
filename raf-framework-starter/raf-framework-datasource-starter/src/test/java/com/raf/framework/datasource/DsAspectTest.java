package com.raf.framework.datasource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DsAspect Test
 *
 * @author Jerry
 * @since 2026-04-20
 */
@SpringBootTest(classes = DsAspectTest.TestConfig.class)
class DsAspectTest {

    @Autowired
    private TestService testService;

    @AfterEach
    void tearDown() {
        DataSourceContextHolder.clearDB();
    }

    @Test
    void testAspectSwitchesToAnnotatedDatasource() {
        testService.slaveMethod();
        assertEquals("slave", testService.getCapturedKey());
    }

    @Test
    void testAspectClearsContextAfterMethod() {
        testService.slaveMethod();
        assertNull(DataSourceContextHolder.getDB());
    }

    @Test
    void testAspectUsesDefaultDatasource() {
        testService.defaultMethod();
        assertEquals(DataSourceContextHolder.DEFAULT_DS, testService.getCapturedKey());
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestConfig {

        @Bean
        public DsAspect dsAspect() {
            return new DsAspect();
        }

        @Bean
        public TestService testService() {
            return new TestService();
        }
    }

    static class TestService {
        private String capturedKey;

        public String getCapturedKey() {
            return capturedKey;
        }

        @DsSelector("slave")
        public void slaveMethod() {
            capturedKey = DataSourceContextHolder.getDB();
        }

        @DsSelector
        public void defaultMethod() {
            capturedKey = DataSourceContextHolder.getDB();
        }
    }
}
