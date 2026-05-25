package com.kanade.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.kanade.backend.ai.AiService;
import com.kanade.backend.ai.AiServiceFactory;
import com.kanade.backend.ai.model.LabelResult;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.mapper.QuestionMapper;
import com.kanade.backend.model.dto.QuestionQueryDTO;
import com.kanade.backend.model.entity.Question;
import com.kanade.backend.model.enums.TaskEnum;
import com.kanade.backend.model.es.QuestionDocument;
import com.kanade.backend.model.vo.QuestionVO;
import com.kanade.backend.service.QuestionCacheService;
import com.kanade.backend.service.QuestionEsService;
import com.kanade.backend.service.QuestionService;
import com.mybatisflex.core.logicdelete.LogicDeleteManager;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.kanade.backend.utils.NormalizationUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.redisson.api.RLock;

@Service
@Slf4j
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Resource
    private AiServiceFactory aiServiceFactory;

    @Resource
    private QuestionCacheService questionCacheService;

    @Resource
    private QuestionEsService questionEsService;

    @Override
    public Long addQuestion(Question question) {
        if (StrUtil.isBlank(question.getContent())) {
            throw new BusinessException(400, "题干不能为空");
        }
        String md5 = DigestUtil.md5Hex(question.getContent());
        question.setCompositeMd5(NormalizationUtils.generateCompositeMd5(question));

        Question existQuestion = LogicDeleteManager.execWithoutLogicDelete(() -> {
            QueryWrapper wrapper = QueryWrapper.create()
                    .eq(Question::getQuestionMd5, md5);
            return this.getOne(wrapper);
        });

        if (existQuestion != null && existQuestion.getIsDeleted() == 0) {
            throw new BusinessException(400, "该试题已存在");
        }

        if (existQuestion != null && existQuestion.getIsDeleted() == 1) {
            LogicDeleteManager.execWithoutLogicDelete(() -> {
                existQuestion.setIsDeleted(0);
                existQuestion.setCreatorId(StpUtil.getLoginIdAsLong());
                existQuestion.setStatus(1);
                this.updateById(existQuestion);
            });
            return existQuestion.getId();
        }

        question.setQuestionMd5(md5);
        question.setCreatorId(StpUtil.getLoginIdAsLong());
        question.setStatus(question.getStatus() == null ? 1 : question.getStatus());

        this.save(question);

        if (question.getTags() == null || question.getKnowledgePoints() == null) {
            aiAddLabelAsync(question);
        }

        syncQuestionToEsAsync(question);

        return question.getId();
    }

    @Override
    public Long addQuestion(Question question, Long creatorId) {
        if (StrUtil.isBlank(question.getContent())) {
            throw new BusinessException(400, "题干不能为空");
        }
        String md5 = DigestUtil.md5Hex(question.getContent());
        question.setCompositeMd5(NormalizationUtils.generateCompositeMd5(question));

        Question existQuestion = LogicDeleteManager.execWithoutLogicDelete(() -> {
            QueryWrapper wrapper = QueryWrapper.create()
                    .eq(Question::getQuestionMd5, md5);
            return this.getOne(wrapper);
        });

        if (existQuestion != null && existQuestion.getIsDeleted() == 0) {
            throw new BusinessException(400, "该试题已存在");
        }

        if (existQuestion != null && existQuestion.getIsDeleted() == 1) {
            LogicDeleteManager.execWithoutLogicDelete(() -> {
                existQuestion.setIsDeleted(0);
                existQuestion.setCreatorId(creatorId != null ? creatorId : StpUtil.getLoginIdAsLong());
                existQuestion.setStatus(1);
                this.updateById(existQuestion);
            });
            return existQuestion.getId();
        }

        question.setQuestionMd5(md5);
        question.setCreatorId(creatorId != null ? creatorId : StpUtil.getLoginIdAsLong());
        question.setStatus(question.getStatus() == null ? 1 : question.getStatus());

        this.save(question);

        if (question.getTags() == null || question.getKnowledgePoints() == null) {
            aiAddLabelAsync(question);
        }

        syncQuestionToEsAsync(question);

        return question.getId();
    }

    @Override
    public boolean updateQuestion(Question question) {
        Long id = question.getId();
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "试题ID不能为空");
        }
        Question old = this.getById(id);
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "试题不存在");
        }

        if (StrUtil.isNotBlank(question.getContent()) && !question.getContent().equals(old.getContent())) {
            String newMd5 = DigestUtil.md5Hex(question.getContent());
            QueryWrapper wrapper = QueryWrapper.create()
                    .eq("questionMd5", newMd5)
                    .ne("id", id);
            if (this.count(wrapper) > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "修改后的试题已存在");
            }
            question.setQuestionMd5(newMd5);
        }

        question.setCreatorId(null);
        boolean result = this.updateById(question);

        // 清除缓存
        if (result) {
            questionCacheService.removeQuestionDetailCache(id);
            // 异步同步到ES
            syncQuestionToEsAsync(this.getById(id));
        }

        return result;
    }

    @Override
    public Page<QuestionVO> getQuestionPage(QuestionQueryDTO queryDTO) {
        // 生成缓存key（基于查询条件）
        String cacheKey = generateCacheKey(queryDTO);
        
        // 1. 先查缓存
        Page<QuestionVO> cachedPage = questionCacheService.getCachedQuestionList(cacheKey);
        if (cachedPage != null) {
            return cachedPage;
        }

        // 2. 缓存未命中，查数据库
        QueryWrapper wrapper = QueryWrapper.create();
        if (queryDTO.getId() != null) {
            wrapper.eq("id", queryDTO.getId());
        }
        if (queryDTO.getType() != null) {
            wrapper.eq("type", queryDTO.getType());
        }
        if (StrUtil.isNotBlank(queryDTO.getSubject())) {
            wrapper.eq("subject", queryDTO.getSubject());
        }
        if (StrUtil.isNotBlank(queryDTO.getChapter())) {
            wrapper.like("chapter", queryDTO.getChapter());
        }
        if (queryDTO.getDifficulty() != null) {
            wrapper.eq("difficulty", queryDTO.getDifficulty());
        }
        if (StrUtil.isNotBlank(queryDTO.getKnowledgePoints())) {
            wrapper.like("knowledgePoints", queryDTO.getKnowledgePoints());
        }
        if (StrUtil.isNotBlank(queryDTO.getTags())) {
            wrapper.like("tags", queryDTO.getTags());
        }
        if (StrUtil.isNotBlank(queryDTO.getContent())) {
            wrapper.like("content", queryDTO.getContent());
        }
        if (queryDTO.getCreatorId() != null) {
            wrapper.eq("creatorId", queryDTO.getCreatorId());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq("status", queryDTO.getStatus());
        }

        if (StrUtil.isNotBlank(queryDTO.getSortField())) {
            String sortField = queryDTO.getSortField();
            boolean isAsc = "ascend".equals(queryDTO.getSortOrder());
            wrapper.orderBy(sortField, isAsc);
        } else {
            wrapper.orderBy("createTime", false);
        }

        Page<Question> page = this.page(Page.of(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);

        List<QuestionVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        Page<QuestionVO> voPage = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize(), page.getTotalRow());
        voPage.setRecords(voList);

        // 3. 写入缓存
        questionCacheService.cacheQuestionList(cacheKey, voPage);

        return voPage;
    }

    /**
     * 生成缓存key
     */
    private String generateCacheKey(QuestionQueryDTO queryDTO) {
        return String.format("%d_%d_%s_%s_%d_%s_%s_%s_%d_%d_%s_%s",
                queryDTO.getPageNum(),
                queryDTO.getPageSize(),
                queryDTO.getId(),
                queryDTO.getType(),
                queryDTO.getDifficulty(),
                queryDTO.getSubject(),
                queryDTO.getChapter(),
                queryDTO.getKnowledgePoints(),
                queryDTO.getTags(),
                queryDTO.getContent(),
                queryDTO.getCreatorId(),
                queryDTO.getStatus(),
                queryDTO.getSortField()
        );
    }

    @Override
    public QuestionVO getQuestionVOById(Long id) {
        // 1. 先查缓存
        QuestionVO cachedVO = questionCacheService.getCachedQuestionDetail(id);
        if (cachedVO != null) {
            questionCacheService.recordQuestionAccess(id);
            return cachedVO;
        }

        // 2. 缓存未命中，获取分布式锁防止击穿
        RLock lock = questionCacheService.getDetailLock(id);
        try {
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                try {
                    // 3. 双重检查缓存
                    cachedVO = questionCacheService.getCachedQuestionDetail(id);
                    if (cachedVO != null) {
                        questionCacheService.recordQuestionAccess(id);
                        return cachedVO;
                    }

                    // 4. 查询数据库
                    Question question = this.getById(id);
                    if (question == null) {
                        questionCacheService.cacheQuestionDetail(id, null);
                        throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "试题不存在");
                    }
                    QuestionVO vo = toVO(question);

                    // 5. 写入缓存
                    questionCacheService.cacheQuestionDetail(id, vo);
                    questionCacheService.recordQuestionAccess(id);

                    return vo;
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 6. 获取锁失败，降级查数据库
        Question question = this.getById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "试题不存在");
        }
        QuestionVO vo = toVO(question);
        questionCacheService.recordQuestionAccess(id);
        return vo;
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        Question question = new Question();
        question.setId(id);
        question.setStatus(status);
        return this.updateById(question);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchAddQuestion(List<Question> questionList) {

        // 最终返回的ID集合
        List<Long> resultIdList = new ArrayList<>();
        // 需要批量插入的新试题
        List<Question> needInsertList = new ArrayList<>();

        for (Question question : questionList) {
            Long recoveredId = processQuestion(question);
            if (recoveredId != null) {
                // 已恢复数据，直接加入结果
                resultIdList.add(recoveredId);
            } else {
                // 新数据，加入批量插入列表
                needInsertList.add(question);
                resultIdList.add(null);
            }
        }

        if (CollUtil.isNotEmpty(needInsertList)) {
            this.saveBatch(needInsertList);
            // 异步同步新增试题到ES
            for (Question q : needInsertList) {
                syncQuestionToEsAsync(q);
            }
        }

        int insertIndex = 0;
        for (int i = 0; i < resultIdList.size(); i++) {
            if (resultIdList.get(i) == null) {
                resultIdList.set(i, needInsertList.get(insertIndex).getId());
                insertIndex++;
            }
        }
        return resultIdList;
    }

    @Override
    public boolean deleteQuestion(Long id) {
        boolean result = this.removeById(id);
        if (result) {
            questionCacheService.removeQuestionDetailCache(id);
            CompletableFuture.runAsync(() -> {
                try {
                    questionEsService.deleteQuestionFromEs(id);
                } catch (Exception e) {
                    log.error("从ES删除试题失败, id: {}", id, e);
                }
            });
        }
        return result;
    }

    private QuestionVO toVO(Question question) {
        if (question == null) return null;
        QuestionVO vo = new QuestionVO();
        BeanUtils.copyProperties(question, vo);
        return vo;
    }

    private Long processQuestion(Question question) {
        // 1. 题干校验
        if (StrUtil.isBlank(question.getContent())) {
            throw new BusinessException(400, "题干不能为空");
        }
        String md5 = DigestUtil.md5Hex(question.getContent());
        question.setCompositeMd5(NormalizationUtils.generateCompositeMd5(question));

        // 2. 查询重复数据（含已删除）
        Question existQuestion = LogicDeleteManager.execWithoutLogicDelete(() -> {
            QueryWrapper wrapper = QueryWrapper.create()
                    .eq(Question::getQuestionMd5, md5);
            return this.getOne(wrapper);
        });

        // 3. 已存在未删除 → 报错
        if (existQuestion != null && existQuestion.getIsDeleted() == 0) {
            throw new BusinessException(400, "该试题已存在");
        }

        // 4. 已删除 → 恢复数据，返回ID
        if (existQuestion != null && existQuestion.getIsDeleted() == 1) {
            LogicDeleteManager.execWithoutLogicDelete(() -> {
                existQuestion.setIsDeleted(0);
                existQuestion.setCreatorId(StpUtil.getLoginIdAsLong());
                existQuestion.setStatus(1);
                this.updateById(existQuestion);
            });
            return existQuestion.getId();
        }

        // 5. 新试题 → 填充公共字段
        question.setQuestionMd5(md5);
        question.setCreatorId(StpUtil.getLoginIdAsLong());
        question.setStatus(question.getStatus() == null ? 1 : question.getStatus());

        // 6. AI自动标签
        // todo 题目保存后才能更新
        if (question.getTags() == null || question.getKnowledgePoints() == null) {
            aiAddLabelAsync(question);
        }

        return null;
    }
    // ai加标签
    private void aiAddLabel(Question question){
        try {
            AiService aiCodeGeneratorService = aiServiceFactory.createAiCodeGeneratorService();
            LabelResult labelResult = aiCodeGeneratorService.generateQuestionLabel(question.toString());

            question.setDifficulty(labelResult.getDifficult());
            question.setKnowledgePoints(labelResult.getKnowledgePoints());
            question.setSubject(labelResult.getSubject());
            question.setChapter(labelResult.getChapter());
            question.setTags(JSONUtil.toJsonStr(labelResult.getTags()));

            this.updateById(question);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"ai响应失败或数据库异常");
        }
    }

    public void aiAddLabelAsync(Question question){
        // 核心：Java原生异步执行，不阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                aiAddLabel(question);
            } catch (Exception ignored) {
                // 异步线程异常，不影响主线程
            }
        });
    }

    /**
     * 异步同步试题到ES
     */
    private void syncQuestionToEsAsync(Question question) {
        if (question == null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                QuestionDocument document = convertToDocument(question);
                questionEsService.syncQuestionToEs(document);
            } catch (Exception e) {
                log.error("异步同步试题到ES失败, id: {}", question.getId(), e);
            }
        });
    }

    /**
     * 将Question转换为QuestionDocument
     */
    private QuestionDocument convertToDocument(Question question) {
        QuestionDocument document = new QuestionDocument();
        BeanUtils.copyProperties(question, document);
        // knowledgePoints: comma-separated String → List<String>
        if (StrUtil.isNotBlank(question.getKnowledgePoints())) {
            document.setKnowledgePoints(
                    StrUtil.split(question.getKnowledgePoints(), ",")
                            .stream().map(String::trim).toList());
        }
        // tags: JSON array String → List<String>
        if (StrUtil.isNotBlank(question.getTags())) {
            document.setTags(cn.hutool.json.JSONUtil.toList(question.getTags(), String.class));
        }
        return document;
    }
}