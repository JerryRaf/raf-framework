package com.raf.framework.web.async;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * 上下文装饰器
 * 用于将主线程的上下文（如 Log TraceId, UserInfo, RequestHeader）传递给子线程
 */
public class RafContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // 1. 【主线程执行】获取当前线程的上下文
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        // 2. 返回一个包装后的 Runnable
        return () -> {
            // 3. 【子线程执行】恢复上下文
            try {
                // 恢复 MDC (日志 TraceID)
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                }
                // 恢复 Web 请求上下文 (如果需要在子线程获取 HttpServletRequest)
                // 注意：这里传递的是引用，子线程尽量只读，不要修改 Response
                if (requestAttributes != null) {
                    RequestContextHolder.setRequestAttributes(requestAttributes, true);
                }

                // 4. 执行真正的业务逻辑
                runnable.run();

            } finally {
                // 5. 【子线程执行】清理上下文，防止线程复用导致的数据污染
                MDC.clear();
                RequestContextHolder.resetRequestAttributes();
            }
        };
    }
}