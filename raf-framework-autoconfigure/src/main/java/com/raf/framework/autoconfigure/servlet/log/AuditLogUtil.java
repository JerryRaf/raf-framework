package com.raf.framework.autoconfigure.servlet.log;

import com.google.common.collect.Maps;
import com.raf.framework.autoconfigure.jackson.Json;
import com.raf.framework.autoconfigure.spring.bean.SpringContext;
import com.raf.framework.autoconfigure.util.DateExtUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public class AuditLogUtil {

    private static Json json = SpringContext.getBean(Json.class);

    private static List<String> whiteHeaders = Arrays.asList("x-user-id", "x-tenant-id", "x-version", "x-device", "x-forwarded-prefix", "x-forwarded-host", "x-forwarded-for");
    private static List<String> sensitiveParams = Arrays.asList("password");

    public static String getAllHeaders(HttpServletRequest request) {
        Map<String, Object> headers = Maps.newHashMapWithExpectedSize(8);
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (whiteHeaders.contains(headerName)) {
                headers.put(headerName, request.getHeader(headerName));
            }
        }
        return json.objToString(headers);
    }

    public static String getAllCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (ArrayUtils.isNotEmpty(cookies)) {
            return json.objToString(cookies);
        }
        return StringUtils.EMPTY;
    }

    public static String getRequestParams(HttpServletRequest request) {
        Map<String, Object> map = Maps.newHashMapWithExpectedSize(8);
        Enumeration<String> enumeration = request.getParameterNames();
        while (enumeration.hasMoreElements()) {
            String paramName = enumeration.nextElement();
            if (sensitiveParams.contains(paramName.toLowerCase())) {
                map.put(paramName, "***");
            } else {
                String paramValue = request.getParameter(paramName);
                map.put(paramName, paramValue);
            }
        }

        return json.objToString(map);
    }

    public static String getRequestUrl(HttpServletRequest request) {
        return request.getRequestURL().toString();
    }

    public static String getRequestUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    public static String getAllHeaders(HttpServletResponse response) {
        Map<String, String> headers = Maps.newHashMapWithExpectedSize(4);
        headers.put("length", response.getHeader("Content-Length"));
        headers.put("date", DateExtUtil.gmtFormat(response.getHeader("Date")));
        return json.objToString(headers);
    }

    public static String getTrace(Throwable t) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        t.printStackTrace(writer);
        StringBuffer buffer = stringWriter.getBuffer();
        return buffer.toString();
    }
}
