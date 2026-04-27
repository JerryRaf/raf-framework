package com.raf.framework.web.servlet.log;

import com.raf.framework.core.jackson.JsonService;
import com.raf.framework.core.spring.bean.SpringContext;
import com.raf.framework.core.util.DateExtUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import com.google.common.collect.Maps;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public class AuditLogUtil {

    /**
     * 私有构造器,防止实例化工具类
     */
    private AuditLogUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static volatile JsonService json;

    private static JsonService json() {
        if (json == null) {
            json = SpringContext.getBean(JsonService.class);
        }
        return json;
    }

    private static List<String> whiteHeaders = Arrays.asList(
            "x-user-id",
            "x-tenant-id",
            "x-version",
            "x-device",
            "x-forwarded-prefix",
            "x-forwarded-host",
            "x-forwarded-for");
    private static List<String> sensitiveParams = Arrays.asList("password");

    // Sensitive data masking patterns
    private static final Pattern PHONE_PATTERN = Pattern.compile("(1[3-9]\\d)(\\d{4})(\\d{4})");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{6})(\\d{8})(\\d{4})");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([\\w.]{1,3})[\\w.]*@([\\w.]+)");
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("(\\d{4})(\\d{8,12})(\\d{4})");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{2,}省)([\\u4e00-\\u9fa5]{2,}市)([\\u4e00-\\u9fa5]{2,}区)(.{4,})");

    public static String getAllHeaders(HttpServletRequest request) {
        Map<String, Object> headers = Maps.newHashMapWithExpectedSize(8);
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (whiteHeaders.contains(headerName)) {
                headers.put(headerName, request.getHeader(headerName));
            }
        }
        return json().toJson(headers);
    }

    public static String getAllCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (ArrayUtils.isNotEmpty(cookies)) {
            return json().toJson(cookies);
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

        return json().toJson(map);
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
        return json().toJson(headers);
    }

    public static String getTrace(Throwable t) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        t.printStackTrace(writer);
        StringBuffer buffer = stringWriter.getBuffer();
        return buffer.toString();
    }

    /**
     * Mask sensitive data in content
     *
     * @param content content to mask
     * @return masked content
     */
    public static String maskSensitiveData(String content) {
        if (StringUtils.isEmpty(content)) {
            return content;
        }

        // Mask phone numbers: 138****1234
        content = PHONE_PATTERN.matcher(content).replaceAll("$1****$3");

        // Mask ID card numbers: 110101********1234
        content = ID_CARD_PATTERN.matcher(content).replaceAll("$1********$3");

        // Mask email addresses: abc***@example.com
        content = EMAIL_PATTERN.matcher(content).replaceAll("$1***@$2");

        // Mask bank card numbers: 6222********1234
        content = BANK_CARD_PATTERN.matcher(content).replaceAll("$1********$3");

        // Mask addresses: 北京市朝阳区****
        content = ADDRESS_PATTERN.matcher(content).replaceAll("$1$2$3****");

        return content;
    }
}
