package com.raf.framework.core.util;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public class IpUtil {
    private static final String UNKNOWN = "unknown";
    private static final List<String> IP_HEADERS = Arrays.asList("x-forwarded-for", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR");

    /**
     * 私有构造器，防止实例化工具类
     */
    private IpUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 通用IP获取逻辑
     */
    private static String resolveIp(Function<String, String> headerResolver, Supplier<String> remoteAddrSupplier) {
        // 遍历所有可能包含真实IP的Header
        for (String header : IP_HEADERS) {
            String ip = headerResolver.apply(header);
            if (isValidIp(ip)) {
                return cleanIp(ip);
            }
        }

        // 从远程地址获取
        String remoteAddr = remoteAddrSupplier.get();
        return cleanIp(remoteAddr);
    }

    /**
     * 验证IP有效性
     */
    private static boolean isValidIp(String ip) {
        return StringUtils.isNotBlank(ip) && !UNKNOWN.equalsIgnoreCase(ip);
    }

    /**
     * 清理IP格式
     */
    private static String cleanIp(String ip) {
        if (ip == null) {
            return "";
        }

        // 处理多IP情况（取第一个）
        String cleanedIp = StringUtils.substringBefore(ip, ",");

        // 处理IPv6本地地址
        if ("0:0:0:0:0:0:0:1".equals(cleanedIp)) {
            return "127.0.0.1";
        }

        // 去除端口号（如果有）
        return StringUtils.substringBefore(cleanedIp, ":");
    }

    /**
     * 适配Servlet请求
     */
    public static String getIpAddr(HttpServletRequest request) {
        return resolveIp(request::getHeader, request::getRemoteAddr);
    }

    /**
     * 适配Reactive请求
     */
    public static String getIpAddr2(ServerHttpRequest request) {
        return resolveIp(header -> request.getHeaders().getFirst(header), () -> {
            InetSocketAddress remoteAddress = request.getRemoteAddress();
            return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "";
        });
    }
}
