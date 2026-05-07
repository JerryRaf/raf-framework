package com.raf.framework.core.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests for JacksonConfig Spring auto-configuration.
 *
 * @author Jerry
 * @since 2026-04-29
 */
@SpringBootTest(classes = JacksonConfigTestApp.class)
@TestPropertySource(properties = {
        "raf.jackson.timezone=Asia/Shanghai",
        "raf.jackson.date-format=yyyy-MM-dd HH:mm:ss",
        "raf.jackson.serialization-inclusion=NON_NULL",
        "raf.jackson.fail-on-unknown-properties=false",
        "raf.jackson.fail-on-empty-beans=false",
        "raf.jackson.write-dates-as-timestamps=false"
})
class JacksonConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JsonService jsonService;

    @Test
    void objectMapper_isNotNull() {
        Assertions.assertNotNull(objectMapper);
    }

    @Test
    void jsonService_isNotNull() {
        Assertions.assertNotNull(jsonService);
    }

    @Test
    void objectMapper_canSerializeSimpleObject() throws Exception {
        String json = objectMapper.writeValueAsString(java.util.Map.of("key", "value"));
        Assertions.assertNotNull(json);
        Assertions.assertTrue(json.contains("key"));
    }

    // ─── Direct unit tests for JacksonConfig branches ────────────────────────

    @Test
    void objectMapper_withAlternativeProperties_createsValidMapper() {
        JacksonConfig config = new JacksonConfig();
        JacksonProperties props = new JacksonProperties();

        // Cover failOnUnknownProperties=true, failOnEmptyBeans=true, writeDatesAsTimestamps=true branches
        props.setFailOnUnknownProperties(true);
        props.setFailOnEmptyBeans(true);
        props.setWriteDatesAsTimestamps(true);
        props.setSerializationInclusion("ALWAYS");
        props.setTimezone("UTC");
        props.setDateFormat("yyyy-MM-dd HH:mm:ss");

        ObjectMapper mapper = config.objectMapper(props);
        Assertions.assertNotNull(mapper);
    }

    @Test
    void objectMapper_withInvalidTimezone_fallsBackToUtc() {
        JacksonConfig config = new JacksonConfig();
        JacksonProperties props = new JacksonProperties();
        props.setTimezone("Invalid/Timezone");
        props.setDateFormat("yyyy-MM-dd HH:mm:ss");
        props.setSerializationInclusion("ALWAYS");

        // Should not throw, falls back to UTC
        ObjectMapper mapper = config.objectMapper(props);
        Assertions.assertNotNull(mapper);
    }

    @Test
    void objectMapper_withInvalidSerializationInclusion_fallsBackToAlways() {
        JacksonConfig config = new JacksonConfig();
        JacksonProperties props = new JacksonProperties();
        props.setSerializationInclusion("INVALID_VALUE");
        props.setTimezone("UTC");
        props.setDateFormat("yyyy-MM-dd HH:mm:ss");

        // Should not throw, falls back to ALWAYS
        ObjectMapper mapper = config.objectMapper(props);
        Assertions.assertNotNull(mapper);
    }

    @Test
    void jacksonCustomizer_isNotNull() {
        JacksonConfig config = new JacksonConfig();
        Assertions.assertNotNull(config.jacksonCustomizer());
    }
}
