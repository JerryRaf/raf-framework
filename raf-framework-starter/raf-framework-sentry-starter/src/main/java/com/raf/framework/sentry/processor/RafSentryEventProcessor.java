package com.raf.framework.sentry.processor;

import com.raf.framework.sentry.RafSentryProperties;
import com.raf.framework.sentry.trace.TraceIdProvider;
import io.sentry.EventProcessor;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.protocol.Contexts;
import io.sentry.protocol.SentryException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Sentry 事件处理器
 * 负责：
 * 1. 注入 traceId 到事件上下文
 * 2. 过滤不需要上报的异常
 * 3. 增强事件上下文信息
 *
 * @author Jerry
 * @date 2026/04/20
 */
@Slf4j
public class RafSentryEventProcessor implements EventProcessor {

    private final TraceIdProvider traceIdProvider;
    private final RafSentryProperties properties;

    public RafSentryEventProcessor(TraceIdProvider traceIdProvider, RafSentryProperties properties) {
        this.traceIdProvider = traceIdProvider;
        this.properties = properties;
    }

    @Override
    public SentryEvent process(SentryEvent event, Hint hint) {
        // 1. 注入 traceId
        injectTraceId(event);

        // 2. 异常过滤
        if (shouldFilterException(event)) {
            log.debug("Sentry event filtered: {}", getExceptionClassName(event));
            return null; // 返回 null 表示丢弃该事件
        }

        // 3. 增强上下文
        enhanceContext(event);

        return event;
    }

    /**
     * 注入 traceId 到事件上下文
     */
    private void injectTraceId(SentryEvent event) {
        try {
            String traceId = traceIdProvider.getTraceId();
            if (StringUtils.isNotBlank(traceId)) {
                // 设置到 tags 中，方便在 Sentry UI 中搜索和过滤
                event.setTag("traceId", traceId);
                event.setTag("trace_id", traceId); // 兼容不同命名习惯

                // 设置到 contexts 中，提供更丰富的上下文信息
                Contexts contexts = event.getContexts();
                contexts.put("trace", new TraceContext(traceId, traceIdProvider.getProviderName()));

                log.debug("Injected traceId to Sentry event: {} (source: {})",
                        traceId, traceIdProvider.getProviderName());
            }
        } catch (Exception e) {
            log.warn("Failed to inject traceId to Sentry event", e);
        }
    }

    /**
     * 判断是否应该过滤该异常
     */
    private boolean shouldFilterException(SentryEvent event) {
        if (!properties.getExceptionFilter().getEnabled()) {
            return false;
        }

        String exceptionClassName = getExceptionClassName(event);
        if (exceptionClassName == null) {
            return false;
        }

        RafSentryProperties.ExceptionFilter filter = properties.getExceptionFilter();

        // 1. 检查排除列表（优先级最高）
        if (filter.getExcludeExceptions().contains(exceptionClassName)) {
            return true;
        }

        // 2. 检查是否是 BusinessException
        if (exceptionClassName.endsWith("BusinessException")) {
            return !filter.getReportBusinessException();
        }

        // 3. 检查包含列表
        if (!filter.getIncludeExceptions().isEmpty()) {
            return !filter.getIncludeExceptions().contains(exceptionClassName);
        }

        return false;
    }

    /**
     * 增强事件上下文
     */
    private void enhanceContext(SentryEvent event) {
        try {
            // 添加框架标识
            event.setTag("framework", "raf-framework");
            event.setTag("framework.version", "3.0.0");

            // 可以在这里添加更多上下文信息
            // 例如：租户ID、用户ID、请求路径等
        } catch (Exception e) {
            log.warn("Failed to enhance Sentry event context", e);
        }
    }

    /**
     * 获取异常类名
     */
    private String getExceptionClassName(SentryEvent event) {
        List<SentryException> exceptions = event.getExceptions();
        if (exceptions != null && !exceptions.isEmpty()) {
            SentryException firstException = exceptions.get(0);
            return firstException.getType();
        }
        return null;
    }

    /**
     * 追踪上下文（用于 Sentry contexts）
     */
    private static class TraceContext {
        private final String traceId;
        private final String source;

        public TraceContext(String traceId, String source) {
            this.traceId = traceId;
            this.source = source;
        }

        public String getTraceId() {
            return traceId;
        }

        public String getSource() {
            return source;
        }
    }
}
