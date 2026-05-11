package com.kanade.backend.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.kanade.backend.model.vo.QuestionVO;
import com.kanade.backend.service.QuestionCacheService;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QuestionCacheServiceImpl implements QuestionCacheService {

    private static final String DETAIL_PREFIX = "question:detail:";
    private static final String LIST_PREFIX = "question:list:";
    private static final String LOCK_DETAIL_PREFIX = "question:lock:detail:";
    private static final String HOT_ZSET_KEY = "question:hot:access";

    private static final int DETAIL_TTL_SECONDS = 30 * 60;
    private static final int LIST_TTL_SECONDS = 5 * 60;
    private static final int NULL_TTL_SECONDS = 60;
    private static final int TTL_JITTER_SECONDS = 300;

    @Resource
    private Cache<Long, QuestionVO> questionDetailCache;

    @Resource
    private Cache<String, Page<QuestionVO>> questionListCache;

    @Resource(name = "jsonRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    // =========== Question Detail ===========

    @Override
    public QuestionVO getCachedQuestionDetail(Long id) {
        // 1. L1: Caffeine
        QuestionVO vo = questionDetailCache.getIfPresent(id);
        if (vo != null) {
            return isNullMarker(vo) ? null : vo;
        }

        // 2. L2: Redis
        String redisKey = DETAIL_PREFIX + id;
        Object cachedValue = redisTemplate.opsForValue().get(redisKey);
        if (cachedValue != null) {
            if (cachedValue instanceof String s && "NULL".equals(s)) {
                questionDetailCache.put(id, nullMarker());
                return null;
            }
            vo = JSON.parseObject(cachedValue.toString(), QuestionVO.class);
            questionDetailCache.put(id, vo);
            return vo;
        }

        return null;
    }

    @Override
    public void cacheQuestionDetail(Long id, QuestionVO vo) {
        String redisKey = DETAIL_PREFIX + id;
        if (vo == null) {
            redisTemplate.opsForValue().set(redisKey, "NULL", Duration.ofSeconds(NULL_TTL_SECONDS));
            questionDetailCache.put(id, nullMarker());
        } else {
            int ttl = DETAIL_TTL_SECONDS + RandomUtil.randomInt(-TTL_JITTER_SECONDS, TTL_JITTER_SECONDS);
            ttl = Math.max(60, ttl);
            redisTemplate.opsForValue().set(redisKey, JSON.toJSONString(vo), Duration.ofSeconds(ttl));
            questionDetailCache.put(id, vo);
        }
    }

    @Override
    public void removeQuestionDetailCache(Long id) {
        redisTemplate.delete(DETAIL_PREFIX + id);
        questionDetailCache.invalidate(id);
        log.debug("Cleared detail cache for question: {}", id);
    }

    // =========== Question List ===========

    @Override
    public Page<QuestionVO> getCachedQuestionList(String cacheKey) {
        // 1. L1: Caffeine
        Page<QuestionVO> page = questionListCache.getIfPresent(cacheKey);
        if (page != null) {
            return page.getRecords() == null || page.getRecords().isEmpty() ? null : page;
        }

        // 2. L2: Redis
        String redisKey = LIST_PREFIX + cacheKey;
        Object cachedValue = redisTemplate.opsForValue().get(redisKey);
        if (cachedValue != null) {
            if (cachedValue instanceof String s && "NULL".equals(s)) {
                questionListCache.put(cacheKey, emptyPage());
                return null;
            }
            page = parsePage(cachedValue.toString());
            questionListCache.put(cacheKey, page);
            return page;
        }

        return null;
    }

    @Override
    public void cacheQuestionList(String cacheKey, Page<QuestionVO> voPage) {
        String redisKey = LIST_PREFIX + cacheKey;
        if (voPage == null || voPage.getRecords() == null || voPage.getRecords().isEmpty()) {
            redisTemplate.opsForValue().set(redisKey, "NULL", Duration.ofSeconds(NULL_TTL_SECONDS));
            questionListCache.put(cacheKey, emptyPage());
        } else {
            int ttl = LIST_TTL_SECONDS + RandomUtil.randomInt(-TTL_JITTER_SECONDS, TTL_JITTER_SECONDS);
            ttl = Math.max(30, ttl);
            redisTemplate.opsForValue().set(redisKey, JSON.toJSONString(voPage), Duration.ofSeconds(ttl));
            questionListCache.put(cacheKey, voPage);
        }
    }

    // =========== Anti-Breakdown Lock ===========

    public RLock getDetailLock(Long id) {
        return redissonClient.getLock(LOCK_DETAIL_PREFIX + id);
    }

    // =========== Hot Questions (ZSET) ===========

    @Override
    public void recordQuestionAccess(Long id) {
        redisTemplate.opsForZSet().incrementScore(HOT_ZSET_KEY, id.toString(), 1);
    }

    public List<Long> getHotQuestionIds(int topN) {
        var entries = redisTemplate.opsForZSet()
                .reverseRangeWithScores(HOT_ZSET_KEY, 0, topN - 1);
        if (entries == null) return List.of();
        return entries.stream()
                .map(t -> Long.valueOf(t.getValue().toString()))
                .toList();
    }

    // =========== Helpers ===========

    private QuestionVO nullMarker() {
        return new QuestionVO();
    }

    private boolean isNullMarker(QuestionVO vo) {
        return vo.getId() == null && vo.getContent() == null;
    }

    private Page<QuestionVO> emptyPage() {
        return new Page<>();
    }

    /**
     * Manually parse MyBatis-Flex Page from JSON.
     * FastJSON 1.x TypeReference may not reliably handle generic types.
     */
    private Page<QuestionVO> parsePage(String json) {
        JSONObject pageJson = JSON.parseObject(json);
        Page<QuestionVO> page = new Page<>();
        page.setPageNumber(pageJson.getLongValue("pageNumber"));
        page.setPageSize(pageJson.getLongValue("pageSize"));
        page.setTotalRow(pageJson.getLongValue("totalRow"));
        JSONArray recordsArray = pageJson.getJSONArray("records");
        if (recordsArray != null && !recordsArray.isEmpty()) {
            List<QuestionVO> records = recordsArray.stream()
                    .map(o -> JSON.parseObject(o.toString(), QuestionVO.class))
                    .collect(Collectors.toList());
            page.setRecords(records);
        }
        return page;
    }
}
