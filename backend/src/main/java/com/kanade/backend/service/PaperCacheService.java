package com.kanade.backend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.kanade.backend.model.vo.ExamPaperVO;
import com.kanade.backend.model.vo.PaperStrategyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 试卷双层缓存服务 (A4修复: Caffeine L1 + Redis L2)
 * - L1: Caffeine 本地缓存 (1h TTL)
 * - L2: Redis 分布式缓存 (24h TTL)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaperCacheService {

    private final RedisTemplate<String, Object> jsonRedisTemplate;
    private final Cache<Long, ExamPaperVO> paperDetailCache;       // Caffeine L1
    private final Cache<Long, PaperStrategyVO> strategyDetailCache; // Caffeine L1
    private static final Duration REDIS_TTL = Duration.ofHours(24);

    // ===== 试卷缓存 =====

    public ExamPaperVO getPaperCached(Long paperId) {
        // L1: Caffeine
        ExamPaperVO cached = paperDetailCache.getIfPresent(paperId);
        if (cached != null) return cached;
        // L2: Redis
        Object val = jsonRedisTemplate.opsForValue().get("cache:paper:" + paperId);
        if (val instanceof ExamPaperVO) {
            paperDetailCache.put(paperId, (ExamPaperVO) val);
            return (ExamPaperVO) val;
        }
        return null;
    }

    public void cachePaper(ExamPaperVO paper) {
        paperDetailCache.put(paper.getId(), paper);
        jsonRedisTemplate.opsForValue().set("cache:paper:" + paper.getId(), paper, REDIS_TTL);
    }

    public void evictPaper(Long paperId) {
        paperDetailCache.invalidate(paperId);
        jsonRedisTemplate.delete("cache:paper:" + paperId);
    }

    // ===== 策略缓存 =====

    public PaperStrategyVO getStrategyCached(Long strategyId) {
        PaperStrategyVO cached = strategyDetailCache.getIfPresent(strategyId);
        if (cached != null) return cached;
        Object val = jsonRedisTemplate.opsForValue().get("cache:strategy:" + strategyId);
        if (val instanceof PaperStrategyVO) {
            strategyDetailCache.put(strategyId, (PaperStrategyVO) val);
            return (PaperStrategyVO) val;
        }
        return null;
    }

    public void cacheStrategy(PaperStrategyVO strategy) {
        strategyDetailCache.put(strategy.getId(), strategy);
        jsonRedisTemplate.opsForValue().set("cache:strategy:" + strategy.getId(), strategy, REDIS_TTL);
    }

    public void evictStrategy(Long strategyId) {
        strategyDetailCache.invalidate(strategyId);
        jsonRedisTemplate.delete("cache:strategy:" + strategyId);
    }

    public void markStrategyAsHighFreq(Long strategyId) {
        String key = "stats:strategy:usage";
        jsonRedisTemplate.opsForZSet().incrementScore(key, strategyId.toString(), 1);
        jsonRedisTemplate.expire(key, 7, TimeUnit.DAYS);
    }

    public List<Long> getHighFreqStrategyIds(int limit) {
        var set = jsonRedisTemplate.opsForZSet().reverseRange("stats:strategy:usage", 0, limit - 1);
        if (set == null) return List.of();
        return set.stream().map(s -> Long.parseLong(s.toString())).toList();
    }
}
