package com.raf.framework.ai.provider;

import com.raf.framework.core.common.exception.BusinessException;
import com.raf.framework.core.common.result.RafResponseEnum;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Provider 工厂，按名称获取对应的 AiProvider 实例。
 */
@RequiredArgsConstructor
public class ProviderFactory {

    /** key=provider name, value=AiProvider 实例 */
    private final Map<String, AiProvider> providers;

    /**
     * 按名称获取 Provider。
     *
     * @param name provider 名称（openai / claude / deepseek）
     * @return AiProvider 实例
     * @throws BusinessException 如果 provider 不存在或未启用
     */
    public AiProvider get(String name) {
        AiProvider provider = providers.get(name);
        if (provider == null) {
            throw new BusinessException(RafResponseEnum.PARAM_ERROR,
                "AI provider not found or not enabled: " + name);
        }
        return provider;
    }
}
