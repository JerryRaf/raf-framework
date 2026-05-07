package com.raf.framework.web.servlet.log;

import org.springframework.core.NamedThreadLocal;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public class LogHolder {
    public static ThreadLocal<Boolean> currentLogResponse = new NamedThreadLocal<>("ACCESS_LOG");

    /**
     * 私有构造器，防止实例化工具类
     */
    private LogHolder() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean currentLogResponse() {
        return currentLogResponse.get();
    }

    public static void setCurrentLogResponse(boolean enabled) {
        currentLogResponse.set(enabled);
    }

    public static void remove() {
        currentLogResponse.remove();
    }
}
