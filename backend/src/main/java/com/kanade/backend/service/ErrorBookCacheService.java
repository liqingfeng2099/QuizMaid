package com.kanade.backend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.kanade.backend.model.entity.ErrorBook;
import com.mybatisflex.core.paginate.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * 错题本双层缓存服务 (C4修复: Caffeine L1 + Redis L2)
 * - L1: Caffeine 本地缓存 (30min TTL, 快速命中)
 * - L2: Redis 分布式缓存 (12h TTL, 跨实例共享)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorBookCacheService {

    private final RedisTemplate<String, Object> jsonRedisTemplate;
    private final Cache<Long, Page<ErrorBook>> errorBookListCache;       // Caffeine L1
    private final Cache<Long, Map<String, Object>> errorBookStatsCache;  // Caffeine L1
    private static final Duration REDIS_TTL = Duration.ofHours(12);

    // ===== 错题列表缓存 =====

    @SuppressWarnings("unchecked")
    public Page<ErrorBook> getCachedErrorList(Long userId, int pageNum) {
        // L1: Caffeine
        Page<ErrorBook> cached = errorBookListCache.getIfPresent(userId);
        if (cached != null) {
            log.debug("[错题缓存] L1命中: userId={}", userId);
            return cached;
        }
        // L2: Redis
        String key = "cache:errorbook:list:" + userId + ":" + pageNum;
        Object val = jsonRedisTemplate.opsForValue().get(key);
        if (val instanceof Page<?> page) {
            errorBookListCache.put(userId, (Page<ErrorBook>) page); // 回填L1
            log.debug("[错题缓存] L2命中: userId={}", userId);
            return (Page<ErrorBook>) page;
        }
        return null;
    }

    public void cacheErrorList(Long userId, int pageNum, Page<ErrorBook> page) {
        // L1
        errorBookListCache.put(userId, page);
        // L2
        String key = "cache:errorbook:list:" + userId + ":" + pageNum;
        jsonRedisTemplate.opsForValue().set(key, page, REDIS_TTL);
    }

    // ===== 错题统计缓存 =====

    @SuppressWarnings("unchecked")
    public Map<String, Object> getCachedErrorStats(Long userId) {
        // L1
        Map<String, Object> cached = errorBookStatsCache.getIfPresent(userId);
        if (cached != null) return cached;
        // L2
        String key = "cache:errorbook:stats:" + userId;
        Object val = jsonRedisTemplate.opsForValue().get(key);
        if (val instanceof Map<?, ?> map) {
            errorBookStatsCache.put(userId, (Map<String, Object>) map);
            return (Map<String, Object>) map;
        }
        return null;
    }

    public void cacheErrorStats(Long userId, Map<String, Object> stats) {
        // L1
        errorBookStatsCache.put(userId, stats);
        // L2
        String key = "cache:errorbook:stats:" + userId;
        jsonRedisTemplate.opsForValue().set(key, stats, REDIS_TTL);
    }

    public void evictUserCache(Long userId) {
        // L1
        errorBookListCache.invalidate(userId);
        errorBookStatsCache.invalidate(userId);
        // L2
        jsonRedisTemplate.delete("cache:errorbook:stats:" + userId);
        log.debug("[错题缓存] 已清除用户{}的双层缓存", userId);
    }
}
