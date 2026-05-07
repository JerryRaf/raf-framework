package com.raf.framework.core.jackson;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Jackson 配置属性
 * <p>
 * 支持国际化时区配置，默认使用 UTC 时区（国际标准）
 * 业务系统可通过配置覆盖为 Asia/Shanghai 等地区时区
 * <p>
 * 配置示例：
 * <pre>
 * raf:
 *   jackson:
 *     # 时区配置（推荐使用 ZoneId 标准名称）
 *     timezone: Asia/Shanghai  # 或 UTC、GMT、America/New_York 等
 *
 *     # 日期时间格式配置
 *     date-format: yyyy-MM-dd HH:mm:ss
 *
 *     # 序列化配置
 *     write-dates-as-timestamps: false
 *     serialization-inclusion: ALWAYS  # ALWAYS / NON_NULL / NON_EMPTY / NON_DEFAULT
 *
 *     # 反序列化配置
 *     fail-on-unknown-properties: false
 * </pre>
 *
 * @author Jerry
 * @date 2026/01/21
 */
@Data
@ConfigurationProperties(prefix = "raf.jackson")
public class JacksonProperties {

    /**
     * 时区配置
     * <p>
     * - 默认值：UTC（国际标准时区，推荐用于国际化系统）
     * - 国内系统可覆盖为：Asia/Shanghai
     * - 支持所有 ZoneId 标准名称：https://docs.oracle.com/javase/8/docs/api/java/time/ZoneId.html
     * <p>
     * 常用时区：
     * - UTC / GMT：协调世界时
     * - Asia/Shanghai：中国标准时间（UTC+8）
     * - America/New_York：美国东部时间（UTC-5/UTC-4）
     * - Europe/London：英国时间（UTC+0/UTC+1）
     */
    private String timezone = "Asia/Shanghai";

    /**
     * 日期时间序列化格式
     * <p>
     * - 默认值：yyyy-MM-dd HH:mm:ss（人类可读格式）
     * - 可选：ISO-8601（ISO 标准格式，带时区信息）
     */
    private String dateFormat = "yyyy-MM-dd HH:mm:ss";

    /**
     * 是否将日期序列化为时间戳（毫秒数）
     * <p>
     * - false：使用字符串格式（推荐，人类可读）
     * - true：使用时间戳格式（JSON 数字）
     */
    private Boolean writeDatesAsTimestamps = false;

    /**
     * JSON 字段包含策略
     * <p>
     * - ALWAYS：总是包含所有字段（默认）
     * - NON_NULL：不包含 null 字段
     * - NON_EMPTY：不包含 null 或空字段
     * - NON_DEFAULT：不包含默认值字段
     */
    private String serializationInclusion = "ALWAYS";

    /**
     * 反序列化时是否忽略未知属性
     * <p>
     * - true：忽略未知字段（推荐，兼容性更好）
     * - false：遇到未知字段抛出异常
     */
    private Boolean failOnUnknownProperties = false;

    /**
     * 序列化时是否忽略空 Bean
     * <p>
     * - true：空序列化为 {}（默认）
     * - false：空序列化为 null
     */
    private Boolean failOnEmptyBeans = false;
}
