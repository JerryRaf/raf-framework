package com.raf.framework.core.jackson;

import com.raf.framework.core.util.DesensitizeUtil;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.TimeZone;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson 自动配置
 * <p>
 * 功能特性：
 * 1. 支持 Java 8 时间类型（LocalDateTime、LocalDate、LocalTime）
 * 2. 支持国际化时区配置（默认 UTC，可配置为 Asia/Shanghai 等）
 * 3. 支持灵活的日期格式配置
 * 4. 支持多种序列化包含策略（ALWAYS/NON_NULL/NON_EMPTY）
 * 5. Long 类型转 String（解决 JavaScript 精度丢失问题）
 * 6. 安全深度限制（防止 DoS 攻击）
 * <p>
 * 配置示例：
 * <pre>
 * raf:
 *   jackson:
 *     timezone: Asia/Shanghai
 *     date-format: yyyy-MM-dd HH:mm:ss
 * </pre>
 *
 * @author Jerry
 * @date 2019/01/01
 */
@Slf4j
@Configuration
@AutoConfiguration(after = JacksonAutoConfiguration.class)
@EnableConfigurationProperties(JacksonProperties.class)
public class JacksonConfig {

    /**
     * 安全限制：JSON 最大嵌套深度（防止 DoS 攻击）
     */
    private static final int MAX_NESTING_DEPTH = 10;

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper(JacksonProperties properties) {
        log.info("Initializing ObjectMapper with properties: timezone={}, dateFormat={}",
                properties.getTimezone(), properties.getDateFormat());

        ObjectMapper objectMapper = new ObjectMapper();

        // 1. 序列化包含策略
        JsonInclude.Include inclusion = serDeserializationStrategy(properties.getSerializationInclusion());
        objectMapper.setSerializationInclusion(inclusion);

        // 2. 反序列化配置
        if (!properties.getFailOnUnknownProperties()) {
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        } else {
            objectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }

        // 3. 序列化配置
        if (!properties.getFailOnEmptyBeans()) {
            objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        } else {
            objectMapper.enable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        }

        if (!properties.getWriteDatesAsTimestamps()) {
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        } else {
            objectMapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }

        // 4. 时区配置（国际化支持）
        TimeZone timeZone = parseTimeZone(properties.getTimezone());
        objectMapper.setTimeZone(timeZone);

        // 5. 日期格式配置
        SimpleDateFormat dateFormat = new SimpleDateFormat(properties.getDateFormat());
        dateFormat.setTimeZone(timeZone);
        objectMapper.setDateFormat(dateFormat);

        // 6. Java 8 时间模块（支持 LocalDateTime、LocalDate、LocalTime）
        JavaTimeModule javaTimeModule = createJavaTimeModule(properties.getDateFormat(), timeZone);
        objectMapper.registerModule(javaTimeModule);

        // 7. 安全深度限制（Jackson 2.15+）
        configureStreamReadConstraints(objectMapper);

        log.info("ObjectMapper initialized successfully: timezone={}, inclusion={}",
                timeZone.getID(), inclusion);

        return objectMapper;
    }

    /**
     * 脱敏专用 ObjectMapper
     */
    @Bean
    @ConditionalOnMissingBean(name = "desensitizeObjectMapper")
    public ObjectMapper desensitizeObjectMapper(ObjectMapper objectMapper) {
        log.info("Initializing desensitizeObjectMapper based on primary ObjectMapper");

        ObjectMapper desensitizeMapper = objectMapper.copy();
        DesensitizeUtil.setCustomObjectMapper(desensitizeMapper);

        return desensitizeMapper;
    }

    /**
     * JSON 服务 Bean
     */
    @Bean
    @ConditionalOnMissingBean(JsonService.class)
    public JsonService jsonService(ObjectMapper objectMapper) {
        return new JsonService(objectMapper);
    }

    /**
     * 自定义 Jackson 构建器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // 处理 Long 类型 -> String（解决 JavaScript 精度丢失）
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);

            // 处理 BigInteger -> String（同样是为了精度）
            builder.serializerByType(BigInteger.class, ToStringSerializer.instance);
        };
    }

    // ==================== 私有辅助方法 ====================

    private TimeZone parseTimeZone(String timezone) {
        try {
            TimeZone tz = TimeZone.getTimeZone(timezone);
            log.info("Parsed timezone: {} -> {}", timezone, tz.getID());
            return tz;
        } catch (Exception e) {
            log.warn("Invalid timezone format: {}, fallback to UTC", timezone, e);
            return TimeZone.getTimeZone("UTC");
        }
    }

    private JsonInclude.Include serDeserializationStrategy(String strategy) {
        try {
            return JsonInclude.Include.valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid serialization-inclusion: {}, fallback to ALWAYS", strategy);
            return JsonInclude.Include.ALWAYS;
        }
    }

    private JavaTimeModule createJavaTimeModule(String dateFormatPattern, TimeZone timeZone) {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormatPattern);

        LocalDateTimeDeserializer localDateTimeDeserializer = new LocalDateTimeDeserializer(formatter) {
            @Override
            public LocalDateTime deserialize(JsonParser parser, DeserializationContext ctxt) throws java.io.IOException {
                try {
                    return super.deserialize(parser, ctxt);
                } catch (DateTimeParseException e) {
                    String text = parser.getText();
                    try {
                        return LocalDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME);
                    } catch (DateTimeParseException ex) {
                        log.warn("Failed to parse LocalDateTime: {}", text, e);
                        throw ex;
                    }
                }
            }
        };
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, localDateTimeDeserializer);

        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(formatter));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(formatter));

        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(formatter));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(formatter));

        return javaTimeModule;
    }

    private void configureStreamReadConstraints(ObjectMapper objectMapper) {
        try {
            objectMapper.getFactory()
                    .setStreamReadConstraints(StreamReadConstraints.builder()
                            .maxNestingDepth(MAX_NESTING_DEPTH)
                            .build());
        } catch (NoSuchMethodError e) {
            log.warn("Jackson version too old to support StreamReadConstraints, skipping security configuration");
        }
    }
}
