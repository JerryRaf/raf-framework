package com.raf.framework.mybatisplus.extension.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 慢 SQL 监控配置属性。
 *
 * <p>示例配置：
 * <pre>
 * raf:
 *   mybatis:
 *     slow-sql:
 *       enabled: true
 *       threshold-ms: 1000
 *       log-full-sql: false
 *       log-stack-trace: false
 * </pre>
 *
 * @author Jerry
 */
@Data
@ConfigurationProperties(prefix = "raf.mybatis.slow-sql")
public class SlowSqlProperties {

    /**
     * 是否启用慢 SQL 监控，默认 false。
     */
    private boolean enabled = false;

    /**
     * 慢 SQL 阈值（毫秒），默认 1000ms。
     * <p>执行时间超过此阈值的 SQL 会打印 WARN 日志。
     */
    private long thresholdMs = 1000;

    /**
     * 是否打印完整 SQL（含参数值），默认 false。
     * <p>生产环境建议保持 false，避免敏感数据泄露到日志。
     */
    private boolean logFullSql = false;

    /**
     * 是否打印调用栈，默认 false。
     * <p>开启后可快速定位慢 SQL 来源代码，但会增加日志量。
     */
    private boolean logStackTrace = false;
}
