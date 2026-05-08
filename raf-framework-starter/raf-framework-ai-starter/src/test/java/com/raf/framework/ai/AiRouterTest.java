package com.raf.framework.ai;

import com.raf.framework.ai.router.AiRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiRouterTest {

    private AiRouter router;

    @BeforeEach
    void setUp() {
        AiProperties props = new AiProperties();
        props.setDefaultProvider("deepseek");
        props.setRoutes(Map.of("code", "claude", "summary", "openai"));
        router = new AiRouter(props);
    }

    @Test
    void provider_field_takes_highest_priority() {
        AiRequest req = AiRequest.builder().provider("openai").prompt("hi").build();
        assertThat(router.resolve(req)).isEqualTo("openai");
    }

    @Test
    void scene_routes_when_provider_is_blank() {
        AiRequest req = AiRequest.builder().scene("code").prompt("hi").build();
        assertThat(router.resolve(req)).isEqualTo("claude");
    }

    @Test
    void falls_back_to_default_when_scene_not_configured() {
        AiRequest req = AiRequest.builder().scene("unknown").prompt("hi").build();
        assertThat(router.resolve(req)).isEqualTo("deepseek");
    }

    @Test
    void falls_back_to_default_when_no_provider_and_no_scene() {
        AiRequest req = AiRequest.builder().prompt("hi").build();
        assertThat(router.resolve(req)).isEqualTo("deepseek");
    }
}
