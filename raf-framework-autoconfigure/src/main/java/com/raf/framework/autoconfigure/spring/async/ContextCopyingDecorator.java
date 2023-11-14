package com.raf.framework.autoconfigure.spring.async;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Map;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public class ContextCopyingDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        try {
            RequestAttributes context = RequestContextHolder.currentRequestAttributes();
            Map<String, String> previous = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    RequestContextHolder.setRequestAttributes(context);
                    MDC.setContextMap(previous);
                    runnable.run();
                } finally {
                    RequestContextHolder.resetRequestAttributes();
                    MDC.clear();
                }
            };
        } catch (IllegalStateException e) {
            return runnable;
        }
    }
}
