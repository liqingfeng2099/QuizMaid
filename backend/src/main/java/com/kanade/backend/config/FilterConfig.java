package com.kanade.backend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<Filter> cacheRequestBodyFilter() {
        FilterRegistrationBean<Filter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new BodyCacheFilter());
        reg.addUrlPatterns("/api/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE); // -2147483648
        reg.setName("bodyCacheFilter");
        return reg;
    }

    /**
     * 在所有 Filter/Interceptor 之前运行：
     * 1) 缓存 POST body 为字节数组
     * 2) 若 Content-Type 是 form-urlencoded 但 body 是 JSON，修正为 application/json
     * 3) 用 HttpServletRequestWrapper 确保后续 getInputStream() 返回缓存数据
     */
    public static class BodyCacheFilter implements Filter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            HttpServletRequest req = (HttpServletRequest) request;
            byte[] body = new byte[0];
            String newCt = req.getContentType();

            if (isModifiableMethod(req.getMethod())) {
                body = req.getInputStream().readAllBytes();
                // 修正 Content-Type
                if (body.length > 0 && newCt != null && newCt.contains("x-www-form-urlencoded")) {
                    String preview = new String(body, 0, Math.min(body.length, 100), StandardCharsets.UTF_8).trim();
                    if (preview.startsWith("{") || preview.startsWith("[")) {
                        newCt = "application/json;charset=UTF-8";
                    }
                }
            }

            String finalCt = newCt;
            byte[] finalBody = body;
            HttpServletRequest wrapper = new HttpServletRequestWrapper(req) {
                @Override public String getContentType() { return finalCt; }
                @Override public int getContentLength() { return finalBody.length; }
                @Override public ServletInputStream getInputStream() { return new CachedStream(finalBody); }
                @Override public BufferedReader getReader() { return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8)); }
            };
            chain.doFilter(wrapper, response);
        }

        private boolean isModifiableMethod(String m) {
            return "POST".equalsIgnoreCase(m) || "PUT".equalsIgnoreCase(m) || "PATCH".equalsIgnoreCase(m);
        }

        private static class CachedStream extends ServletInputStream {
            private final ByteArrayInputStream bis;
            CachedStream(byte[] data) { this.bis = new ByteArrayInputStream(data); }
            @Override public int read() { return bis.read(); }
            @Override public boolean isFinished() { return bis.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener listener) {}
        }
    }
}
