package com.kanade.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.mapper.ErrorBookMapper;
import com.kanade.backend.model.entity.ErrorBook;
import com.kanade.backend.model.entity.Question;
import com.kanade.backend.model.vo.PersonalDimensionVO;
import com.kanade.backend.service.ErrorBookCacheService;
import com.kanade.backend.service.ErrorBookService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ErrorBookServiceImpl extends ServiceImpl<ErrorBookMapper, ErrorBook>
        implements ErrorBookService {

    private final ErrorBookMapper errorBookMapper;
    private final com.kanade.backend.service.QuestionService questionService;
    private final ErrorBookCacheService cacheService;

    @Override
    @Transactional
    public void syncFromExam(Long userId, Long questionId) {
        // 检查题目是否已逻辑删除
        Question q = questionService.getById(questionId);
        if (q == null || (q.getIsDeleted() != null && q.getIsDeleted() == 1)) {
            log.info("[错题同步] 题目已失效，跳过: questionId={}", questionId);
            return;
        }
        // MD5查重：确保题目存在于题库
        QueryWrapper qw = QueryWrapper.create()
                .eq("userId", userId).eq("questionId", questionId);
        ErrorBook exist = this.getOne(qw);
        if (exist != null) {
            exist.setErrorCount((exist.getErrorCount() != null ? exist.getErrorCount() : 0) + 1);
            exist.setLastErrorTime(LocalDateTime.now());
            if (exist.getReviewStatus() == null || exist.getReviewStatus() == 2) {
                exist.setReviewStatus(0); // 重新标记为未复习
            }
            this.updateById(exist);
            cacheService.evictUserCache(userId);
        } else {
            ErrorBook eb = new ErrorBook();
            eb.setUserId(userId);
            eb.setQuestionId(questionId);
            eb.setErrorType(1);
            eb.setReviewStatus(0);
            eb.setErrorCount(1);
            eb.setFirstErrorTime(LocalDateTime.now());
            eb.setLastErrorTime(LocalDateTime.now());
            eb.setIsArchived(0);
            eb.setCreateTime(LocalDateTime.now());
            eb.setUpdateTime(LocalDateTime.now());
            this.save(eb);
            cacheService.evictUserCache(userId);
        }
    }

    @Override
    public Page<ErrorBook> getErrorPage(int pageNum, int pageSize, Integer errorType,
                                         String knowledgePoint, Integer reviewStatus, String sortBy) {
        Long userId = StpUtil.getLoginIdAsLong();
        QueryWrapper qw = QueryWrapper.create()
                .eq("userId", userId).eq("isArchived", 0);

        if (errorType != null) qw.eq("errorType", errorType);
        if (reviewStatus != null) qw.eq("reviewStatus", reviewStatus);

        // 知识点筛选：先查符合知识点的questionId列表
        if (StrUtil.isNotBlank(knowledgePoint)) {
            com.mybatisflex.core.query.QueryWrapper kpQw = com.mybatisflex.core.query.QueryWrapper.create()
                    .select("id")
                    .like("knowledgePoints", knowledgePoint);
            List<Question> kpQuestions = questionService.list(kpQw);
            List<Long> matchedIds = kpQuestions.stream().map(Question::getId).toList();
            if (!matchedIds.isEmpty()) qw.in("questionId", matchedIds);
            else qw.eq("questionId", -1L); // 无匹配结果
        }

        String sort = sortBy != null ? sortBy : "lastErrorTime";
        String orderCol;
        if ("firstErrorTime".equals(sort)) orderCol = "first_error_time";
        else if ("errorCount".equals(sort)) orderCol = "error_count";
        else orderCol = "last_error_time";
        qw.orderBy(orderCol, false);

        return this.page(Page.of(pageNum, pageSize), qw);
    }

    @Override
    public void updateReviewStatus(Long id, Integer reviewStatus) {
        Long userId = StpUtil.getLoginIdAsLong();
        ErrorBook eb = this.getById(id);
        if (eb == null || !eb.getUserId().equals(userId))
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        eb.setReviewStatus(reviewStatus);
        this.updateById(eb);
        cacheService.evictUserCache(userId);
    }

    @Override
    public void updateErrorType(Long id, Integer errorType) {
        Long userId = StpUtil.getLoginIdAsLong();
        ErrorBook eb = this.getById(id);
        if (eb == null || !eb.getUserId().equals(userId))
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        eb.setErrorType(errorType);
        this.updateById(eb);
        cacheService.evictUserCache(userId);
    }

    @Override
    public void toggleArchive(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        ErrorBook eb = this.getById(id);
        if (eb == null || !eb.getUserId().equals(userId))
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        eb.setIsArchived(eb.getIsArchived() != null && eb.getIsArchived() == 1 ? 0 : 1);
        this.updateById(eb);
        cacheService.evictUserCache(userId);
    }

    @Override
    public void deleteError(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        ErrorBook eb = this.getById(id);
        if (eb == null || !eb.getUserId().equals(userId))
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        this.removeById(id);
        cacheService.evictUserCache(userId);
    }

    @Override
    public Map<String, Object> getErrorStats() {
        Long userId = StpUtil.getLoginIdAsLong();
        // 尝试从缓存获取
        Map<String, Object> cached = cacheService.getCachedErrorStats(userId);
        if (cached != null) return cached;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalErrors", errorBookMapper.totalErrors(userId));

        List<PersonalDimensionVO> typeStats = errorBookMapper.errorTypeStats(userId);
        Map<String, String> typeNames = Map.of("1","概念错误","2","计算错误","3","思路错误","4","审题错误");
        for (var v : typeStats) {
            v.setDimensionKey(typeNames.getOrDefault(v.getDimensionKey(), v.getDimensionKey()));
        }
        stats.put("byErrorType", typeStats);

        List<PersonalDimensionVO> kpRaw = errorBookMapper.errorKnowledgeStats(userId);
        stats.put("byKnowledgePoint", splitKnowledgePoints(kpRaw));
        cacheService.cacheErrorStats(userId, stats);
        return stats;
    }

    @Override
    public List<PersonalDimensionVO> getWeakKnowledgePoints() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<PersonalDimensionVO> raw = errorBookMapper.errorKnowledgeStats(userId);
        return splitKnowledgePoints(raw);
    }

    private List<PersonalDimensionVO> splitKnowledgePoints(List<PersonalDimensionVO> raw) {
        Map<String, PersonalDimensionVO> agg = new LinkedHashMap<>();
        for (var r : raw) {
            if (StrUtil.isBlank(r.getDimensionKey())) continue;
            for (String kp : r.getDimensionKey().split(",")) {
                String key = kp.trim();
                if (key.isEmpty()) continue;
                agg.compute(key, (k, v) -> {
                    if (v == null) { v = new PersonalDimensionVO(); v.setDimensionKey(key); v.setTotalCount(0L); }
                    v.setTotalCount(v.getTotalCount() + r.getTotalCount());
                    return v;
                });
            }
        }
        List<PersonalDimensionVO> result = new ArrayList<>(agg.values());
        result.sort((a, b) -> Long.compare(b.getTotalCount(), a.getTotalCount()));
        return result;
    }
}
