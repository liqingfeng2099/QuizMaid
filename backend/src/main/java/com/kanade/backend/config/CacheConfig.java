package com.kanade.backend.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kanade.backend.model.entity.ErrorBook;
import com.kanade.backend.model.vo.ExamPaperVO;
import com.kanade.backend.model.vo.PaperStrategyVO;
import com.kanade.backend.model.vo.QuestionVO;
import com.mybatisflex.core.paginate.Page;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    // ===== Question caches =====

    @Bean
    public Cache<Long, QuestionVO> questionDetailCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Bean
    public Cache<String, Page<QuestionVO>> questionListCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    // ===== Paper cache (A4: Caffeine L1) =====

    @Bean
    public Cache<Long, ExamPaperVO> paperDetailCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .build();
    }

    @Bean
    public Cache<Long, PaperStrategyVO> strategyDetailCache() {
        return Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(2, TimeUnit.HOURS)
                .recordStats()
                .build();
    }

    // ===== ErrorBook cache (C4: Caffeine L1) =====

    @Bean
    public Cache<Long, Page<ErrorBook>> errorBookListCache() {
        return Caffeine.newBuilder()
                .maximumSize(300)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Bean
    public Cache<Long, Map<String, Object>> errorBookStatsCache() {
        return Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .build();
    }
}
