package com.kanade.backend.utils;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

public class IpUtils {

    private static final String[] HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
    };

    public static String getClientIp(HttpServletRequest request) {
        for (String header : HEADERS) {
            String ip = request.getHeader(header);
            if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
                int commaIndex = ip.indexOf(',');
                if (commaIndex > 0) {
                    ip = ip.substring(0, commaIndex).trim();
                }
                return ip;
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return StrUtil.isBlank(remoteAddr) ? "127.0.0.1" : remoteAddr;
    }
}
