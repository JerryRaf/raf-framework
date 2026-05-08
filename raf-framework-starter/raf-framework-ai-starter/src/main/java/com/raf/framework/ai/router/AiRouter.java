package com.raf.framework.ai.router;

import com.raf.framework.ai.AiProperties;
import com.raf.framework.ai.AiRequest;
import lombok.RequiredArgsConstructor;

/**
 * AI 路由决策器。
 * 优先级：request.provider > routes[scene] > defaultProvider
 */
@RequiredArgsConstructor
public class AiRouter {

    private final AiProperties properties;

    /**
     * 根据请求解析出目标 Provider 名称。
     */
    public String resolve(AiRequest request) {
        // 1. 代码层直接指定
        if (request.getProvider() != null && !request.getProvider().isBlank()) {
            return request.getProvider();
        }
        // 2. 场景路由
        if (request.getScene() != null && !request.getScene().isBlank()) {
            String routed = properties.getRoutes().get(request.getScene());
            if (routed != null && !routed.isBlank()) {
                return routed;
            }
        }
        // 3. 默认 Provider
        return properties.getDefaultProvider();
    }
}
