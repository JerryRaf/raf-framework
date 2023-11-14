package com.raf.framework.autoconfigure.servlet.log;

import org.springframework.core.NamedThreadLocal;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public class LogHolder {
    private static ThreadLocal<Boolean> CURRENT_LOG_RESPONSE = new NamedThreadLocal<>("ACCESS_LOG");

    public static boolean currentLogResponse() {
        return CURRENT_LOG_RESPONSE.get();
    }

    public static void setCurrentLogResponse(boolean currentLogResponse) {
        CURRENT_LOG_RESPONSE.set(currentLogResponse);
    }

    public static void remove() {
        CURRENT_LOG_RESPONSE.remove();
    }

}
