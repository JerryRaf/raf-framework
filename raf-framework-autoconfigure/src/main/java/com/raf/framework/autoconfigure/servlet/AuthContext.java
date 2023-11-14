package com.raf.framework.autoconfigure.servlet;

import com.raf.framework.autoconfigure.common.RafConstant;
import com.raf.framework.autoconfigure.common.exception.AuthException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Jerry
 * @date 2020/03/16
 */
public class AuthContext {

    /**
     *
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
     *
     * @return
     */
    public static String getAuthorization() {
        return getRequestHeader(RafConstant.AUTHORIZATION);
    }

    /**
     * 获取当前登录用户UserId
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
     * @return
     */
    public static Long getUserIdAndThrow() {
        String userIdStr = getRequestHeader(RafConstant.USER_ID_HEADER);
        if (StringUtils.isEmpty(userIdStr)) {
            throw new AuthException();
        }

        return Long.valueOf(userIdStr);
    }
}