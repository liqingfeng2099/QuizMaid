package com.kanade.backend.aop;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kanade.backend.annotation.ApiSignature;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;

import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;

@Aspect
@Component
@Order(1)
@Slf4j
public class ApiSignatureAspect {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(com.kanade.backend.annotation.ApiSignature) || " +
            "@within(com.kanade.backend.annotation.ApiSignature)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        ApiSignature annotation = getAnnotation(joinPoint);
        if (annotation == null || !annotation.enabled()) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes()).getRequest();

        // 1. Extract headers
        String timestamp = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        String incomingSignature = request.getHeader("X-Signature");
        String appId = request.getHeader("X-App-Id");

        if (StrUtil.hasBlank(timestamp, nonce, incomingSignature, appId)) {
            throw new BusinessException(ErrorCode.SIGNATURE_ERROR, "缺少必要的签名参数");
        }

        // 2. Validate timestamp
        long serverTime = System.currentTimeMillis() / 1000;
        long requestTime = Long.parseLong(timestamp);
        if (Math.abs(serverTime - requestTime) > annotation.timestampExpire()) {
            throw new BusinessException(ErrorCode.SIGNATURE_ERROR, "请求已过期");
        }

        // 3. Validate nonce (anti-replay)
        if (annotation.requireNonce()) {
            String nonceKey = "signature:nonce:" + appId + ":" + nonce;
            Boolean exists = stringRedisTemplate.hasKey(nonceKey);
            if (Boolean.TRUE.equals(exists)) {
                throw new BusinessException(ErrorCode.SIGNATURE_ERROR, "重复的请求");
            }
            stringRedisTemplate.opsForValue().set(nonceKey, "1", Duration.ofSeconds(annotation.timestampExpire()));
        }

        // 4. Get secretKey
        String secretKey = getSecretKey(appId);
        if (secretKey == null) {
            throw new BusinessException(ErrorCode.SIGNATURE_ERROR, "无效的AppId");
        }

        // 5. Server-side signature calculation
        Map<String, String> params = new TreeMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            String[] values = entry.getValue();
            if (values != null && values.length > 0) {
                params.put(entry.getKey(), values[0]);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!sb.isEmpty()) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        sb.append("&timestamp=").append(timestamp);
        sb.append("&nonce=").append(nonce);
        sb.append("&secretKey=").append(secretKey);
        String expectedSignature = DigestUtil.md5Hex(sb.toString());

        // 6. Compare
        if (!expectedSignature.equals(incomingSignature)) {
            log.warn("Signature mismatch for appId={}, uri={}", appId, request.getRequestURI());
            throw new BusinessException(ErrorCode.SIGNATURE_ERROR, "签名验证失败");
        }

        return joinPoint.proceed();
    }

    private ApiSignature getAnnotation(ProceedingJoinPoint joinPoint) {
        var methodSig = (org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature();
        ApiSignature annotation = methodSig.getMethod().getAnnotation(ApiSignature.class);
        if (annotation == null) {
            annotation = joinPoint.getTarget().getClass().getAnnotation(ApiSignature.class);
        }
        return annotation;
    }

    private String getSecretKey(String appId) {
        Object credentials = stringRedisTemplate.opsForHash().get("app:credentials", appId);
        if (credentials == null) {
            return null;
        }
        JSONObject cred = JSON.parseObject(credentials.toString());
        if (!"enabled".equals(cred.getString("status"))) {
            return null;
        }
        return cred.getString("secretKey");
    }
}
