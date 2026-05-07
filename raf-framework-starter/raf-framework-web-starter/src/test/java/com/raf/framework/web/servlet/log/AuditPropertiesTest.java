package com.raf.framework.web.servlet.log;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for AuditProperties and its nested types.
 *
 * @author Jerry
 * @since 2026-04-29
 */
class AuditPropertiesTest {

    @Test
    void defaultLogLevel_isRspHeaders() {
        AuditProperties props = new AuditProperties();
        Assertions.assertEquals(AuditProperties.LogLevel.RSP_HEADERS, props.getLog().level);
    }

    @Test
    void logLevel_off_hasLevel0() {
        Assertions.assertEquals(0, AuditProperties.LogLevel.OFF.getLevel());
    }

    @Test
    void logLevel_basic_hasLevel1() {
        Assertions.assertEquals(1, AuditProperties.LogLevel.BASIC.getLevel());
    }

    @Test
    void logLevel_reqHeaders_hasLevel2() {
        Assertions.assertEquals(2, AuditProperties.LogLevel.REQ_HEADERS.getLevel());
    }

    @Test
    void logLevel_reqBody_hasLevel3() {
        Assertions.assertEquals(3, AuditProperties.LogLevel.REQ_BODY.getLevel());
    }

    @Test
    void logLevel_rspHeaders_hasLevel4() {
        Assertions.assertEquals(4, AuditProperties.LogLevel.RSP_HEADERS.getLevel());
    }

    @Test
    void logLevel_rspBody_hasLevel5() {
        Assertions.assertEquals(5, AuditProperties.LogLevel.RSP_BODY.getLevel());
    }

    @Test
    void logLevel_ordering_isCorrect() {
        Assertions.assertTrue(AuditProperties.LogLevel.OFF.getLevel()
                < AuditProperties.LogLevel.BASIC.getLevel());
        Assertions.assertTrue(AuditProperties.LogLevel.BASIC.getLevel()
                < AuditProperties.LogLevel.REQ_HEADERS.getLevel());
        Assertions.assertTrue(AuditProperties.LogLevel.REQ_HEADERS.getLevel()
                < AuditProperties.LogLevel.REQ_BODY.getLevel());
        Assertions.assertTrue(AuditProperties.LogLevel.REQ_BODY.getLevel()
                < AuditProperties.LogLevel.RSP_HEADERS.getLevel());
        Assertions.assertTrue(AuditProperties.LogLevel.RSP_HEADERS.getLevel()
                < AuditProperties.LogLevel.RSP_BODY.getLevel());
    }

    @Test
    void setLogLevel_updatesLevel() {
        AuditProperties props = new AuditProperties();
        props.getLog().level = AuditProperties.LogLevel.OFF;
        Assertions.assertEquals(AuditProperties.LogLevel.OFF, props.getLog().level);
    }
}
