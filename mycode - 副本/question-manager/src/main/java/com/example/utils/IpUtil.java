// src/main/java/com/example/utils/IpUtil.java（完整版）
package com.example.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IP地址工具类
 */
public class IpUtil {

    /**
     * 获取当前请求的IP地址
     */
    public static String getCurrentIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return getIpAddr(request);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    /**
     * 获取请求IP地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }

        String ip = null;

        // 1. X-Forwarded-For：Squid 服务代理
        ip = request.getHeader("X-Forwarded-For");
        if (isIpValid(ip)) {
            // 多次代理的情况下，第一个IP为客户端真实IP
            if (ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        }

        // 2. Proxy-Client-IP：Apache 代理
        ip = request.getHeader("Proxy-Client-IP");
        if (isIpValid(ip)) {
            return ip;
        }

        // 3. WL-Proxy-Client-IP：WebLogic 代理
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (isIpValid(ip)) {
            return ip;
        }

        // 4. HTTP_CLIENT_IP：一些代理服务器
        ip = request.getHeader("HTTP_CLIENT_IP");
        if (isIpValid(ip)) {
            return ip;
        }

        // 5. HTTP_X_FORWARDED_FOR：一些代理服务器
        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (isIpValid(ip)) {
            return ip;
        }

        // 6. X-Real-IP：Nginx 代理
        ip = request.getHeader("X-Real-IP");
        if (isIpValid(ip)) {
            return ip;
        }

        // 7. 如果没有代理，直接获取远程地址
        ip = request.getRemoteAddr();

        // 处理IPv6本地回环地址
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }

        // 8. 获取本机IP
        if ("127.0.0.1".equals(ip) || "localhost".equalsIgnoreCase(ip)) {
            try {
                InetAddress inet = InetAddress.getLocalHost();
                ip = inet.getHostAddress();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        return ip;
    }

    /**
     * 判断IP是否有效
     */
    private static boolean isIpValid(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
    }

    /**
     * 获取真实IP（不考虑代理）
     */
    public static String getRealIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }

    /**
     * 获取IP地址的地理位置（可以集成IP库）
     */
    public static String getIpLocation(String ip) {
        // 这里可以集成IP地理位置库，如：ip2region、纯真IP库等
        // 简化返回
        return "未知";
    }
}