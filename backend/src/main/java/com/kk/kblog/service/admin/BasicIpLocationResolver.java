package com.kk.kblog.service.admin;

import java.net.InetAddress;

/**
 * Fallback resolver when external IP service is unavailable.
 * Returns coarse-grained labels only: LOCAL / LAN / PUBLIC / UNKNOWN.
 */
public class BasicIpLocationResolver implements IpLocationResolver {

    @Override
    public String resolveLocation(String ip) {
        if (ip == null || ip.isBlank()) {
            return "UNKNOWN";
        }
        var normalized = ip.trim();
        if (isLoopback(normalized)) {
            return "LOCAL";
        }

        try {
            var addr = InetAddress.getByName(normalized);
            if (addr.isSiteLocalAddress()) {
                return "LAN";
            }
            if (addr.isLoopbackAddress()) {
                return "LOCAL";
            }
            if (addr.isAnyLocalAddress()) {
                return "UNKNOWN";
            }

            var host = addr.getCanonicalHostName();
            if (host != null && !host.isBlank() && !host.equals(normalized)) {
                return host;
            }
            return "PUBLIC";
        } catch (Exception ignored) {
            return "UNKNOWN";
        }
    }

    private boolean isLoopback(String ip) {
        return "127.0.0.1".equals(ip)
                || "::1".equals(ip)
                || "0:0:0:0:0:0:0:1".equals(ip);
    }
}

