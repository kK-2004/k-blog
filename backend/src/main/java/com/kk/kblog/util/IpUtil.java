package com.kk.kblog.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @ Author：YongKang
 * @ Date：2026-01-13-23:14
 */
public class IpUtil {
    public static String getClientIp(HttpServletRequest request) {
        String ip;

        // 1️⃣ X-Forwarded-For（最常见）
        ip = request.getHeader("X-Forwarded-For");
        if (isValidIp(ip)) {
            // 可能是：client, proxy1, proxy2
            return ip.split(",")[0].trim();
        }

        // 2️⃣ X-Real-IP（Nginx 常用）
        ip = request.getHeader("X-Real-IP");
        if (isValidIp(ip)) {
            return ip;
        }

        // 3️⃣ 其他代理头（兼容）
        ip = request.getHeader("Proxy-Client-IP");
        if (isValidIp(ip)) return ip;

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (isValidIp(ip)) return ip;

        // 4️⃣ 最后兜底
        ip = request.getRemoteAddr();

        // 5️⃣ 本地回环处理
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            return "127.0.0.1";
        }

        return ip;
    }

    public static boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
    }
}