package com.raf.framework.autoconfigure.servlet.log;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Data
@ConfigurationProperties(prefix = "raf")
public class AuditProperties {
    private Log log = new Log();

    @Data
    public static class Log {
        public LogLevel level = LogLevel.RSP_HEADERS;
    }

    public enum LogLevel {
        /**
         * 关闭日志
         */
        OFF(0),
        /**
         * 基本
         */
        BASIC(1),
        /**
         * 请求头
         */
        REQ_HEADERS(2),
        /**
         * 请求体
         */
        REQ_BODY(3),
        /**
         * 返回头
         */
        RSP_HEADERS(4),
        /**
         * 返回体
         */
        RSP_BODY(5);
        private int level;

        LogLevel(int level) {
            this.level = level;
        }

        public int getLevel() {
            return this.level;
        }
    }
}
