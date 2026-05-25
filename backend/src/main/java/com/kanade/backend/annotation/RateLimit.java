package com.kanade.backend.annotation;

import com.kanade.backend.model.enums.RateLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    RateLevel level() default RateLevel.L2_MEDIUM;
    int timeWindow() default 60;
    int maxRequests() default 0;
    String message() default "请求过于频繁，请稍后再试";
}
