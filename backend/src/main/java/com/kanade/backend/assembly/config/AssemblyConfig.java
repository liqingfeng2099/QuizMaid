package com.kanade.backend.assembly.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableAsync
public class AssemblyConfig {

    /**
     * 组卷专用线程池
     * 核心线程数 20，最大 50，隔离组卷请求避免阻塞其他接口
     */
    @Bean("assemblyExecutor")
    public Executor assemblyExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("assembly-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Caffeine 本地缓存：候选题目池（单次组卷请求内复用）
     */
    @Bean("candidateCache")
    public Cache<String, Object> candidateCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    /**
     * Caffeine 本地缓存：组卷结果（相同条件下直接返回）
     */
    @Bean("assemblyResultCache")
    public Cache<String, Object> assemblyResultCache() {
        return Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
    }
}
