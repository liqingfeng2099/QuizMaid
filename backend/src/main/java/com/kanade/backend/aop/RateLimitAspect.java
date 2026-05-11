package com.kanade.backend.aop;

import cn.dev33.satoken.stp.StpUtil;
import com.kanade.backend.annotation.RateLimit;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.model.enums.RateLevel;
import com.kanade.backend.service.CounterManager;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Order(2)
@Slf4j
public class RateLimitAspect {

    @Resource
    private CounterManager counterManager;

    @Around("@annotation(com.kanade.backend.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RateLimit rateLimit = signature.getMethod().getAnnotation(RateLimit.class);

        long userId = StpUtil.getLoginIdAsLong();

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes()).getRequest();
        String apiPath = request.getRequestURI();

        RateLevel level = rateLimit.level();
        int timeWindow = rateLimit.timeWindow() > 0 ? rateLimit.timeWindow() : level.getDefaultTimeWindow();
        int maxRequests = rateLimit.maxRequests() > 0 ? rateLimit.maxRequests() : level.getDefaultMaxRequests();

        String key = String.format("rate:%s:%d:%s", level.name(), userId, apiPath);
        long count = counterManager.incrAndGetCounter(key, timeWindow, TimeUnit.SECONDS);

        if (count > maxRequests) {
            log.warn("Rate limit triggered: key={}, count={}, max={}", key, count, maxRequests);
            throw new BusinessException(429, rateLimit.message());
        }

        return joinPoint.proceed();
    }
}
