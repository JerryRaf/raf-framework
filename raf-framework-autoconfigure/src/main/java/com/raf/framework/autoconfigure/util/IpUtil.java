package com.raf.framework.autoconfigure.util;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public class IpUtil {
    private final static String UNKNOWN = "unknown";
    private final static String[] IP_HEADS = new String[]{
            "x-forwarded-for",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR",
    };

    private IpUtil(){}

    /**
     * 获取ip地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        for (String header : IP_HEADS) {
            String ip = request.getHeader(header);
            if (StringUtils.isNotBlank(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.substring(0, ip.indexOf(","));
                }
                return ip;
            }
        }
        String ip = request.getRemoteAddr();
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }

    /**
     * 获取本地ip地址
     *
     */
    public static InetAddress getLocalHostAddress() {
        try {
            InetAddress candidateAddress = null;
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
                 ifaces.hasMoreElements(); ) {
                NetworkInterface iface = ifaces.nextElement();
                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses();
                     inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {
                        if (inetAddr.isSiteLocalAddress()) {
                            return inetAddr;
                        } else if (candidateAddress == null) {
                            candidateAddress = inetAddr;
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress;
            }
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException(
                        "The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to determine LAN address: " + ex);
        }
    }

    /**
     * 获取ip地址
     *
     */
    public static String getHostIp(InetAddress netAddress) {
        if (null == netAddress) {
            return null;
        }
        return netAddress.getHostAddress();
    }

    /**
     * getHost
     *
     */
    public static String getHost(String pidHost) {
        int index = pidHost.indexOf('@');
        String retHost = String.valueOf(System.currentTimeMillis());
        if (index == -1) {
            retHost = pidHost;
        } else if (index + 1 <= pidHost.length()) {
            retHost = pidHost.substring(index + 1);
        }
        return retHost.replaceAll("\\.", "_");
    }

    /**
     * getMxBeanName
     *
     */
    public static String getMxBeanName() {
        String pidHost = ManagementFactory.getRuntimeMXBean().getName();
        if (Strings.isNullOrEmpty(pidHost)) {
            return String.valueOf(System.currentTimeMillis());
        }
        return pidHost;
    }
}
