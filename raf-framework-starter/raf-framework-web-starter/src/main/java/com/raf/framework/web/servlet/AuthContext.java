package com.raf.framework.web.servlet;

import com.raf.framework.core.common.RafConstant;
import com.raf.framework.core.common.exception.ProtocolException;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author Jerry
 * @date 2020/03/16
 */
public class AuthContext {

    /**
     * 私有构造器，防止实例化工具类
     */
    private AuthContext() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * @param headerName
     * @return
     */
    private static String getRequestHeader(String headerName) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            return request.getHeader(headerName);
        }
        return null;
    }

    /**
     * @return
     */
    public static String getAuthorization() {
        return getRequestHeader(RafConstant.AUTHORIZATION);
    }

    /**
     * 获取当前登录用户UserId
     *
     * @return
     */
    public static Long getUserId() {
        String userIdStr = getRequestHeader(RafConstant.USER_ID_HEADER);
        if (StringUtils.isEmpty(userIdStr)) {
            return 0L;
        }

        return Long.valueOf(userIdStr);
    }

    /**
     * 获取当前登录用户UserId-获取不到throw 401
     *
     * @return
     */
    public static Long getUserIdAndThrow() {
        String userIdStr = getRequestHeader(RafConstant.USER_ID_HEADER);
        if (StringUtils.isEmpty(userIdStr)) {
            throw new ProtocolException(null);
        }

        return Long.valueOf(userIdStr);
    }
}