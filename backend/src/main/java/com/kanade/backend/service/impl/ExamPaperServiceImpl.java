package com.kanade.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.kanade.backend.ai.AiService;
import com.kanade.backend.ai.AiServiceFactory;
import com.kanade.backend.ai.strategy.StrategyInferenceService;
import com.kanade.backend.assembly.constraint.ConstraintValidator;
import com.kanade.backend.assembly.model.AssemblyConstraint;
import com.kanade.backend.assembly.model.AssemblyContext;
import com.kanade.backend.assembly.model.IndicatorEnum;
import com.kanade.backend.assembly.model.QuestionScore;
import com.kanade.backend.assembly.scorer.CompositeScorer;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.mapper.ExamPaperMapper;
import com.kanade.backend.mapper.PaperquestionMapper;
import com.kanade.backend.mapper.StrategyWeightMapper;
import com.kanade.backend.mapper.UserMapper;
import com.kanade.backend.model.dto.AIPaperAssemblyDTO;
import com.kanade.backend.model.dto.AIPaperAssemblyV2DTO;
import com.kanade.backend.model.dto.AssemblyDegradeHintDTO;
import com.kanade.backend.model.dto.ExamPaperQueryDTO;
import com.kanade.backend.model.entity.*;
import com.kanade.backend.model.vo.*;
import com.kanade.backend.service.AiPaperChatService;
import com.kanade.backend.service.ExamPaperService;
import com.kanade.backend.service.PaperStrategyService;
import com.kanade.backend.service.QuestionService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamPaperServiceImpl extends ServiceImpl<ExamPaperMapper, ExamPaper> implements ExamPaperService {

    private final PaperquestionMapper paperquestionMapper;
    private final QuestionService questionService;
    private final AiServiceFactory aiServiceFactory;
    private final AiPaperChatService aiPaperChatService;
    private final UserMapper userMapper;
    private final StrategyInferenceService strategyInferenceService;
    private final PaperStrategyService paperStrategyService;
    private final StrategyWeightMapper strategyWeightMapper;

    @Override
    public Long addExamPaper(ExamPaper examPaper) {
        if (StrUtil.isBlank(examPaper.getPaperName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "试卷名称不能为空");
        }
        if (StrUtil.isBlank(examPaper.getSubject())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学科不能为空");
        }
        if (examPaper.getTotalScore() == null) {
            examPaper.setTotalScore(0);
        }
        examPaper.setCreatorId(StpUtil.getLoginIdAsLong());
        if (examPaper.getStatus() == null) {
            examPaper.setStatus(0); // 草稿
        }
        this.save(examPaper);
        return examPaper.getId();
    }

    @Override
    public boolean updateExamPaper(ExamPaper examPaper) {
        Long id = examPaper.getId();
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "试卷ID不能为空");
        }
        ExamPaper old = this.getById(id);
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "试卷不存在");
        }
        // 已发布试卷不可直接编辑，仅支持复制后修改
        if (old.getStatus() != null && old.getStatus() == 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "已发布试卷不可直接编辑，请使用复制功能修改");
        }
        // 校验权限：创建人才能修改
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!old.getCreatorId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权修改他人试卷");
        }
        // 不允许修改创建人
        examPaper.setCreatorId(null);
        return this.updateById(examPaper);
    }

    @Override
    public boolean updateStatus(Long id, Integer newStatus) {
        ExamPaper old = this.getById(id);
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "试卷不存在");
        }
        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = StpUtil.hasRole("admin");
        if (!isAdmin && !old.getCreatorId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权修改他人试卷");
        }

        int current = old.getStatus() != null ? old.getStatus() : 0;
        if (!isValidStatusTransition(current, newStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "无效的状态切换：" + getStatusName(current) + " → " + getStatusName(newStatus) +
                    "。允许的切换：草稿→已发布→已归档→已停用");
        }

        ExamPaper paper = new ExamPaper();
        paper.setId(id);
        paper.setStatus(newStatus);
        return this.updateById(paper);
    }

    /** 状态切换规则：0 草稿 → 1 已发布 → 2 已归档 → 3 已停用 */
    private boolean isValidStatusTransition(int from, int to) {
        if (from == to) return true;
        if (from == 0 && to == 1) return true; // 草稿→已发布
        if (from == 1 && to == 2) return true; // 已发布→已归档
        if (from == 2 && to == 3) return true; // 已归档→已停用
        if (from == 0 && to == 2) return true; // 草稿→已归档
        if (from == 1 && to == 3) return true; // 已发布→已停用
        if (from == 2 && to == 1) return true; // 已归档→已发布（重新启用）
        return false;
    }

    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "草稿";
            case 1: return "已发布";
            case 2: return "已归档";
            case 3: return "已停用";
            default: return "未知";
        }
    }

    @Override
    public ExamPaperVO copyExamPaper(Long id) {
        ExamPaper old = this.getById(id);
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "试卷不存在");
        }
        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = StpUtil.hasRole("admin");
        if (!isAdmin && !old.getCreatorId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权复制他人试卷");
        }

        // 复制试卷（新状态为草稿）
        ExamPaper copy = new ExamPaper();
        BeanUtils.copyProperties(old, copy, "id", "createTime", "updateTime", "isDeleted");
        copy.setPaperName(old.getPaperName() + " (副本)");
        copy.setCreatorId(currentUserId);
        copy.setStatus(0); // 草稿
        copy.setExportStatus(0);
        this.save(copy);

        // 复制题目关联
        QueryWrapper qw = QueryWrapper.create().eq("paperId", id).orderBy("sort", true);
        List<Paperquestion> pqList = paperquestionMapper.selectListByQuery(qw);
        if (!pqList.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            int totalScore = 0;
            for (Paperquestion pq : pqList) {
                Paperquestion newPq = new Paperquestion();
                newPq.setPaperId(copy.getId());
                newPq.setQuestionId(pq.getQuestionId());
                newPq.setQuestionScore(pq.getQuestionScore());
                newPq.setSort(pq.getSort());
                newPq.setIsAutoAdd(0);
                newPq.setCreateTime(now);
                paperquestionMapper.insert(newPq);
                totalScore += pq.getQuestionScore() != null ? pq.getQuestionScore() : 0;
            }
            copy.setTotalScore(totalScore);
            this.updateById(copy);
        }

        return getExamPaperVOById(copy.getId());
    }

    @Override
    public boolean deleteExamPaper(Long id) {
        ExamPaper paper = this.getById(id);
        if (paper == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "试卷不存在");
        }
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!paper.getCreatorId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权删除他人试卷");
        }
        return this.removeById(id);
    }

    @Override
    public ExamPaperVO getExamPaperVOById(Long id) {
        ExamPaper paper = this.getById(id);
        if (paper == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "试卷不存在");
        }
        ExamPaperVO vo = new ExamPaperVO();
        BeanUtils.copyProperties(paper, vo);
        // 获取关联的试题
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("paperId",id).orderBy("sort",true);
        List<Paperquestion> relations = paperquestionMapper.selectListWithRelationsByQuery(queryWrapper);
        if (!relations.isEmpty()) {
            List<Long> questionIds = relations.stream().map(Paperquestion::getQuestionId).collect(Collectors.toList());
            // listByIds 会自动过滤 isDeleted=0 的题目，用于判断题目是否有效
            List<Question> activeQuestions = questionService.listByIds(questionIds);
            Set<Long> activeIds = activeQuestions.stream().map(Question::getId).collect(Collectors.toSet());
            // 构建 id→Question 的映射（包含有效题目）
            Map<Long, Question> questionMap = new HashMap<>();
            for (Question q : activeQuestions) {
                questionMap.put(q.getId(), q);
            }
            List<PaperQuestionVO> questionVOs = relations.stream().map(rel -> {
                PaperQuestionVO qvo = new PaperQuestionVO();
                qvo.setId(rel.getId());
                qvo.setQuestionId(rel.getQuestionId());
                qvo.setQuestionScore(rel.getQuestionScore());
                qvo.setSort(rel.getSort());
                Question q = questionMap.get(rel.getQuestionId());
                if (q != null && activeIds.contains(rel.getQuestionId())) {
                    qvo.setQuestionStatus(0); // 正常
                    qvo.setQuestionContent(q.getContent());
                    qvo.setType(q.getType());
                } else {
                    qvo.setQuestionStatus(1); // 已失效
                    qvo.setQuestionContent("[题目已失效]");
                }
                return qvo;
            }).collect(Collectors.toList());
            vo.setQuestions(questionVOs);
        }
        return vo;
    }

    @Override
    public Page<ExamPaperVO> getExamPaperPage(ExamPaperQueryDTO queryDTO) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (StrUtil.isNotBlank(queryDTO.getPaperName())) {
            wrapper.like("paperName", queryDTO.getPaperName());
        }
        if (StrUtil.isNotBlank(queryDTO.getSubject())) {
            wrapper.eq("subject", queryDTO.getSubject());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq("status", queryDTO.getStatus());
        }
        // 权限控制：普通用户只能查自己的，管理员查所有
        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = StpUtil.hasRole("admin");
        if (!isAdmin) {
            wrapper.eq("creatorId", currentUserId);
        } else if (queryDTO.getCreatorId() != null) {
            wrapper.eq("creatorId", queryDTO.getCreatorId());
        }

        if (StrUtil.isNotBlank(queryDTO.getSortField())) {
            String sortField = queryDTO.getSortField();
            boolean isAsc = "ascend".equals(queryDTO.getSortOrder());
            wrapper.orderBy(sortField, isAsc);
        } else {
            wrapper.orderBy("createTime", false);
        }

        Page<ExamPaper> page = this.page(Page.of(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
        Page<ExamPaperVO> voPage = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize(), page.getTotalRow());
        List<ExamPaperVO> voList = page.getRecords().stream().map(paper -> {
            ExamPaperVO vo = new ExamPaperVO();
            BeanUtils.copyProperties(paper, vo);
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * AI智能组卷
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AIPaperAssemblyResultVO aiAssemblePaper(AIPaperAssemblyDTO assemblyDTO) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        long startTime = System.currentTimeMillis();

        // 1. 参数校验
        if (StrUtil.isBlank(assemblyDTO.getPaperName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "试卷名称不能为空");
        }
        if (assemblyDTO.getStatus() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "试卷状态不能为空");
        }

        log.info("[AI组卷] 开始组卷，用户ID: {}, 试卷名称: {}", currentUserId, assemblyDTO.getPaperName());

        // 2. 根据条件过滤题目（只查询当前用户的题目）
        List<Question> candidateQuestions = filterQuestions(assemblyDTO, currentUserId);
        log.info("[AI组卷] 过滤后候选题目数量: {}", candidateQuestions.size());

        if (candidateQuestions.isEmpty()) {
            // 提供更详细的错误信息
            String errorMsg = "没有符合条件的题目。";
            if (StrUtil.isNotBlank(assemblyDTO.getSubject())) {
                errorMsg += "当前学科【" + assemblyDTO.getSubject() + "】";
            }
            if (assemblyDTO.getDifficulty() != null) {
                errorMsg += "、难度【" + getDifficultyText(assemblyDTO.getDifficulty()) + "】";
            }
            errorMsg += "下没有可用题目，请调整筛选条件或先添加题目。";
            throw new BusinessException(ErrorCode.PARAMS_ERROR, errorMsg);
        }

        // 3. 构建AI提示词
        String prompt = buildAIPrompt(assemblyDTO, candidateQuestions);
        log.info("[AI组卷] 提示词长度: {}", prompt.length());

        // 4. 调用AI服务
        AiService aiService = aiServiceFactory.createAiCodeGeneratorService();
        String aiResponse;
        try {
            aiResponse = aiService.generatePaperAssembly(prompt);
            log.info("[AI组卷] AI响应: {}", aiResponse);
        } catch (Exception e) {
            log.error("[AI组卷] AI调用失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI组卷失败，请稍后重试");
        }

        // 5. 解析AI返回的题目ID列表
        List<Long> selectedQuestionIds = parseAIResponse(aiResponse);
        log.info("[AI组卷] AI选中的题目数量: {}", selectedQuestionIds.size());

        if (selectedQuestionIds.isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI未能选出合适的题目，请调整需求描述");
        }

        // 6. 创建试卷
        ExamPaper examPaper = new ExamPaper();
        examPaper.setPaperName(assemblyDTO.getPaperName());
        // 如果用户未指定学科，从候选题目中取第一个题目的学科
        String subject = assemblyDTO.getSubject();
        if (StrUtil.isBlank(subject) && !candidateQuestions.isEmpty()) {
            subject = candidateQuestions.get(0).getSubject();
        }
        examPaper.setSubject(subject != null ? subject : "");
        examPaper.setTotalScore(assemblyDTO.getTotalScore() != null ? assemblyDTO.getTotalScore() : 0);
        examPaper.setCreatorId(currentUserId);
        examPaper.setStatus(assemblyDTO.getStatus());
        this.save(examPaper);

        // 7. 添加试题到试卷
        int sort = 1;
        int totalScore = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Long questionId : selectedQuestionIds) {
            Paperquestion paperQuestion = new Paperquestion();
            paperQuestion.setPaperId(examPaper.getId());
            paperQuestion.setQuestionId(questionId);
            paperQuestion.setQuestionScore(10); // 默认每题10分
            paperQuestion.setSort(sort++);
            paperQuestion.setCreateTime(now);
            paperquestionMapper.insert(paperQuestion);
            totalScore += 10;
        }

        // 8. 更新试卷总分
        examPaper.setTotalScore(totalScore);
        this.updateById(examPaper);

        long endTime = System.currentTimeMillis();
        log.info("[AI组卷] 组卷完成，耗时: {}ms, 试卷ID: {}, 题目数: {}, 总分: {}",
                endTime - startTime, examPaper.getId(), selectedQuestionIds.size(), totalScore);

        // 9. 返回结果
        return AIPaperAssemblyResultVO.builder()
                .paperId(examPaper.getId())
                .questionIds(selectedQuestionIds)
                .totalQuestions(selectedQuestionIds.size())
                .actualTotalScore(totalScore)
                .build();
    }

    /**
     * 根据条件过滤题目
     */
    private List<Question> filterQuestions(AIPaperAssemblyDTO assemblyDTO, Long userId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("creatorId", userId);
    
        // 如果指定了学科,则过滤
        if (StrUtil.isNotBlank(assemblyDTO.getSubject())) {
            wrapper.eq("subject", assemblyDTO.getSubject());
        }
    
        // 如果指定了章节,则过滤
        if (StrUtil.isNotBlank(assemblyDTO.getChapter())) {
            wrapper.like("chapter", assemblyDTO.getChapter());
        }
    
        // 如果指定了难度,则过滤
        if (assemblyDTO.getDifficulty() != null) {
            wrapper.eq("difficulty", assemblyDTO.getDifficulty());
        }
    
        // 限制最大查询数量,避免性能问题
        wrapper.limit(500);
    
        List<Question> questions = questionService.list(wrapper);
        log.info("[AI组卷] 查询到题目数量: {}, 用户ID: {}", questions.size(), userId);
            
        return questions;
    }

    /**
     * 构建AI提示词
     */
    private String buildAIPrompt(AIPaperAssemblyDTO assemblyDTO, List<Question> candidateQuestions) {
        StringBuilder prompt = new StringBuilder();

        // 第一部分：表单固定模板
        prompt.append("【组卷要求】\n");
        if (StrUtil.isNotBlank(assemblyDTO.getSubject())) {
            prompt.append("- 学科：").append(assemblyDTO.getSubject()).append("\n");
        }
        if (StrUtil.isNotBlank(assemblyDTO.getChapter())) {
            prompt.append("- 章节：").append(assemblyDTO.getChapter()).append("\n");
        }
        if (assemblyDTO.getDifficulty() != null) {
            String difficultyText = getDifficultyText(assemblyDTO.getDifficulty());
            prompt.append("- 难度：").append(difficultyText).append("\n");
        }
        if (assemblyDTO.getTotalScore() != null) {
            prompt.append("- 目标总分：").append(assemblyDTO.getTotalScore()).append("\n");
        }
        prompt.append("\n");

        // 第二部分：用户自定义需求
        if (StrUtil.isNotBlank(assemblyDTO.getUserRequirement())) {
            prompt.append("【用户需求】\n");
            prompt.append(assemblyDTO.getUserRequirement()).append("\n\n");
        }

        // 第三部分：候选题目信息（关键字段）
        prompt.append("【候选题目列表】\n");
        prompt.append("以下是可供选择的题目信息（ID, 题型, 难度, 学科, 章节, 知识点, 标签）：\n");

        for (int i = 0; i < candidateQuestions.size(); i++) {
            Question q = candidateQuestions.get(i);
            prompt.append(String.format("%d. ID=%d, 题型=%s, 难度=%s, 学科=%s, 章节=%s, 知识点=%s, 标签=%s\n",
                    i + 1,
                    q.getId(),
                    getQuestionTypeText(q.getType()),
                    getDifficultyText(q.getDifficulty()),
                    q.getSubject() != null ? q.getSubject() : "未分类",
                    q.getChapter() != null ? q.getChapter() : "未分类",
                    q.getKnowledgePoints() != null ? q.getKnowledgePoints() : "无",
                    q.getTags() != null ? q.getTags() : "无"
            ));
        }

        prompt.append("\n请从以上候选题目中选择合适的题目ID，返回JSON格式：{\"questionIds\": [id1, id2, ...]}");

        return prompt.toString();
    }

    /**
     * 解析AI响应
     */
    private List<Long> parseAIResponse(String aiResponse) {
        try {
            // 尝试提取JSON部分（去除可能的Markdown代码块标记）
            String jsonStr = aiResponse.trim();
            if (jsonStr.startsWith("```") && jsonStr.endsWith("```")) {
                jsonStr = jsonStr.substring(3, jsonStr.length() - 3).trim();
            }
            if (jsonStr.startsWith("json")) {
                jsonStr = jsonStr.substring(4).trim();
            }

            // 解析JSON
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = JSONUtil.toBean(jsonStr, Map.class);
            Object questionIdsObj = resultMap.get("questionIds");

            if (questionIdsObj instanceof List) {
                List<?> rawList = (List<?>) questionIdsObj;
                return rawList.stream()
                        .filter(id -> id instanceof Number)
                        .map(id -> ((Number) id).longValue())
                        .collect(Collectors.toList());
            }

            return Collections.emptyList();
        } catch (Exception e) {
            log.error("[AI组卷] 解析AI响应失败: {}", aiResponse, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取难度文本
     */
    private String getDifficultyText(Integer difficulty) {
        if (difficulty == null) return "未知";
        switch (difficulty) {
            case 1: return "简单";
            case 2: return "中等";
            case 3: return "困难";
            default: return "未知";
        }
    }

    /**
     * 获取题型文本
     */
    private String getQuestionTypeText(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "单选题";
            case 2: return "多选题";
            case 3: return "填空题";
            case 4: return "简答题";
            default: return "未知";
        }
    }

    // ==================== AI增强组卷 A+C 混合模式（任务5重构） ====================

    /** 算法预筛的 Top-N 截断值 */
    private static final int PRESCREEN_TOP_N = 60;

    /**
     * A+C 混合组卷（三阶段）：
     * 阶段1 (C): LLM 推断用户自然语言需求 → 结构化策略参数 → 存入 PaperStrategy + StrategyWeight
     * 阶段2 (A): CompositeScorer 算法评分排序 → Top 60 截断 → ConstraintValidator 预校验
     * 阶段3 (C): LLM 基于 Top 候选做二次精选 → 后置约束校验 → 贪心补位
     */
    @Transactional(rollbackFor = Exception.class)
    public AIAssemblyStrategyVO aiAssemblePaperV2(AIPaperAssemblyV2DTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        long startTime = System.currentTimeMillis();

        // 构建用户学习画像
        AIProfileVO profile = null;
        if (Boolean.TRUE.equals(dto.getUsePersonalization())) {
            profile = buildUserProfile(userId);
        }

        // ==================== 阶段1: LLM 推断策略参数 (C) ====================
        log.info("[AI组卷A+C] 阶段1: LLM 推断策略参数");
        StrategyInferenceService.StrategyInferenceResult inferenceResult =
                strategyInferenceService.infer(dto, profile);
        StrategyInferenceService.InferredParams params = inferenceResult.getParams();

        // 持久化策略
        PaperStrategy strategy = persistInferredStrategy(inferenceResult, dto, userId);
        List<StrategyWeight> weights = persistInferredWeights(inferenceResult, strategy.getId());
        log.info("[AI组卷A+C] 策略已保存: id={}, 难度均值={}, 题型数={}, 推断来源={}",
                strategy.getId(), params.getDifficultyAvg(),
                params.getQuestionTypeConfig() != null ? params.getQuestionTypeConfig().size() : 0,
                inferenceResult.isInferenceSuccess() ? "LLM" : "默认");

        // ==================== 阶段2: 算法预筛评分 (A) ====================
        log.info("[AI组卷A+C] 阶段2: 算法预筛评分");
        List<Question> candidates = filterQuestionsV2(dto, userId);
        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有符合条件的题目，请调整筛选条件或先添加题目");
        }

        PreScreenResult preScreen = preScreenAndScore(candidates, strategy, weights);
        log.info("[AI组卷A+C] 预筛: {}道候选 → Top {}道 (得分范围 {:.3f}~{:.3f})",
                candidates.size(), preScreen.topCandidates.size(),
                preScreen.topCandidates.isEmpty() ? 0 : preScreen.topCandidates.get(preScreen.topCandidates.size() - 1).getCompositeScore(),
                preScreen.topCandidates.isEmpty() ? 0 : preScreen.topCandidates.get(0).getCompositeScore());

        if (preScreen.topCandidates.isEmpty()) {
            // 预筛无结果 → 返回空但保留策略信息
            aiPaperChatService.saveChat(userId, null, strategy.getId(), dto.getUserRequirement(),
                    "预筛无结果", 2, 0);
            return buildEmptyResult(inferenceResult, strategy, "候选题目评分均为0，请调整筛选条件");
        }

        // ==================== 阶段3: LLM 二次精选 (C) ====================
        log.info("[AI组卷A+C] 阶段3: LLM 二次精选");
        String prompt = buildStage3Prompt(dto, profile, preScreen, params);

        String aiResponse = null;
        int retryCount = 0;
        Exception lastError = null;

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                AiService ai = aiServiceFactory.createAiCodeGeneratorService();
                aiResponse = ai.generatePaperAssembly(prompt);
                if (aiResponse != null && !aiResponse.isBlank()) {
                    retryCount = attempt;
                    break;
                }
            } catch (Exception e) {
                lastError = e;
                log.warn("[AI组卷A+C] 第{}次调用失败: {}", attempt + 1, e.getMessage());
                retryCount = attempt + 1;
                if (attempt < 2) {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }
            }
        }

        List<Long> aiSelectedIds;
        boolean llmSuccess = (aiResponse != null && !aiResponse.isBlank());

        if (llmSuccess) {
            aiSelectedIds = parseAIResponse(aiResponse);
            log.info("[AI组卷A+C] LLM 选中 {} 道题", aiSelectedIds.size());
        } else {
            aiSelectedIds = Collections.emptyList();
            log.warn("[AI组卷A+C] LLM 调用失败，降级为纯算法结果");
        }

        // 后置校验 + 贪心补位
        List<QuestionScore> finalSelection = validateAndFillGaps(
                aiSelectedIds, preScreen.topCandidates, preScreen.constraints);

        // 保存对话记录
        aiPaperChatService.saveChat(userId, null, strategy.getId(),
                dto.getUserRequirement(),
                aiResponse != null ? aiResponse : (lastError != null ? lastError.getMessage() : "无响应"),
                llmSuccess ? 1 : 2, retryCount);

        // 构建返回结果
        AIAssemblyStrategyVO result = buildResultVO(finalSelection, inferenceResult, strategy, preScreen,
                llmSuccess, retryCount);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[AI组卷A+C] 完成, 耗时: {}ms | 推断:{} → 预筛:{}→{}道 → LLM:{}道 → 最终:{}道 | 策略id:{}",
                elapsed, inferenceResult.isInferenceSuccess() ? "LLM" : "默认",
                candidates.size(), preScreen.topCandidates.size(),
                aiSelectedIds.size(), finalSelection.size(), strategy.getId());

        return result;
    }

    /**
     * 确认AI方案并创建试卷
     */
    @Transactional(rollbackFor = Exception.class)
    public ExamPaperVO confirmAIAssembly(AIPaperAssemblyV2DTO dto, AIAssemblyStrategyVO strategy) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 过滤候选题目
        List<Question> candidates = filterQuestionsV2(dto, userId);

        // 创建试卷
        ExamPaper paper = new ExamPaper();
        paper.setPaperName(dto.getPaperName() != null && !dto.getPaperName().isBlank()
                ? dto.getPaperName() : "AI组卷-" + System.currentTimeMillis());
        paper.setSubject(dto.getSubject() != null ? dto.getSubject() : "综合");
        paper.setTotalScore(dto.getTotalScore() != null ? dto.getTotalScore() : 0);
        paper.setCreatorId(userId);
        paper.setStatus(dto.getStatus());
        paper.setPaperType(3); // AI组卷
        this.save(paper);

        // 关联题目
        int sort = 1;
        int totalScore = 0;
        LocalDateTime now = LocalDateTime.now();
        if (strategy.getQuestionIds() != null) {
            for (Long qid : strategy.getQuestionIds()) {
                Paperquestion pq = Paperquestion.builder()
                        .paperId(paper.getId()).questionId(qid)
                        .questionScore(10).sort(sort++).isAutoAdd(1).createTime(now).build();
                paperquestionMapper.insert(pq);
                totalScore += 10;
            }
        }
        paper.setTotalScore(totalScore);
        paper.setStrategyId(dto.getPreviousChatId());
        this.updateById(paper);

        return getExamPaperVOById(paper.getId());
    }

    /**
     * 构建用户学习画像
     */
    public AIProfileVO buildUserProfile(Long userId) {
        User user = userMapper.selectOneById(userId);
        if (user == null) return null;

        BigDecimal acc = BigDecimal.ZERO;
        if (user.getAnswerNum() != null && user.getAnswerNum() > 0 && user.getCorrectNum() != null) {
            acc = BigDecimal.valueOf(user.getCorrectNum() * 100.0 / user.getAnswerNum())
                    .setScale(2, java.math.RoundingMode.HALF_UP);
        }
        AIProfileVO profile = AIProfileVO.builder()
                .answerNum(user.getAnswerNum() != null ? user.getAnswerNum().longValue() : 0L)
                .correctNum(user.getCorrectNum() != null ? user.getCorrectNum().longValue() : 0L)
                .accuracy(acc)
                .weakPoints(new ArrayList<>())
                .build();

        // 查询正确率<60%的知识点作为薄弱点
        QueryWrapper wq = QueryWrapper.create()
                .eq("creatorId", userId)
                .lt("accuracy", new BigDecimal("60.00"))
                .isNotNull("knowledgePoints")
                .orderBy("accuracy", true)
                .limit(10);
        List<Question> weakQuestions = questionService.list(wq);
        for (Question q : weakQuestions) {
            if (q.getKnowledgePoints() != null && !q.getKnowledgePoints().isBlank()) {
                for (String kp : q.getKnowledgePoints().split(",")) {
                    profile.getWeakPoints().add(AIProfileVO.WeakPoint.builder()
                            .knowledgePoint(kp.trim())
                            .accuracy(q.getAccuracy())
                            .totalCount(q.getTotalCount())
                            .build());
                }
            }
        }
        return profile;
    }

    /**
     * 增强版候选过滤
     */
    private List<Question> filterQuestionsV2(AIPaperAssemblyV2DTO dto, Long userId) {
        QueryWrapper wrapper = QueryWrapper.create().eq("creatorId", userId);
        if (StrUtil.isNotBlank(dto.getSubject())) wrapper.eq("subject", dto.getSubject());
        if (StrUtil.isNotBlank(dto.getChapter())) wrapper.like("chapter", dto.getChapter());
        if (dto.getDifficulty() != null) wrapper.eq("difficulty", dto.getDifficulty());
        wrapper.limit(500);
        return questionService.list(wrapper);
    }

    // ==================== A+C 阶段方法 ====================

    /**
     * 持久化 LLM 推断的策略
     */
    private PaperStrategy persistInferredStrategy(StrategyInferenceService.StrategyInferenceResult result,
                                                    AIPaperAssemblyV2DTO dto, Long userId) {
        StrategyInferenceService.InferredParams params = result.getParams();
        LocalDateTime now = LocalDateTime.now();

        String typeConfigJson = params.getQuestionTypeConfig() != null
                ? JSONUtil.toJsonStr(params.getQuestionTypeConfig().stream()
                    .map(StrategyInferenceService.TypeConfigItem::toMap)
                    .collect(Collectors.toList()))
                : "[]";

        String diffConfigJson = params.getDifficultyConfig() != null
                ? JSONUtil.toJsonStr(params.getDifficultyConfig().stream()
                    .map(StrategyInferenceService.DiffConfigItem::toMap)
                    .collect(Collectors.toList()))
                : "[]";

        String kpScopeJson = params.getKnowledgePointScope() != null
                ? JSONUtil.toJsonStr(params.getKnowledgePointScope())
                : "[]";

        int totalScore = params.getQuestionTypeConfig() != null
                ? params.getQuestionTypeConfig().stream().mapToInt(t -> t.getCount() * t.getScore()).sum()
                : (dto.getTotalScore() != null ? dto.getTotalScore() : 150);

        PaperStrategy strategy = PaperStrategy.builder()
                .strategyName("AI推断-" + (dto.getPaperName() != null ? dto.getPaperName() : "组卷"))
                .userId(userId)
                .totalScore(totalScore)
                .difficultyAvg(params.getDifficultyAvg())
                .duration(params.getDuration())
                .questionTypeConfig(typeConfigJson)
                .difficultyConfig(diffConfigJson)
                .knowledgePointScope(kpScopeJson)
                .isDefault(0)
                .createTime(now)
                .updateTime(now)
                .build();

        paperStrategyService.save(strategy);
        return strategy;
    }

    /**
     * 持久化 LLM 推断的权重
     */
    private List<StrategyWeight> persistInferredWeights(StrategyInferenceService.StrategyInferenceResult result,
                                                         Long strategyId) {
        LocalDateTime now = LocalDateTime.now();
        List<StrategyWeight> weights = new ArrayList<>();

        if (result.getParams().getWeights() != null) {
            for (StrategyInferenceService.WeightItem w : result.getParams().getWeights()) {
                StrategyWeight sw = StrategyWeight.builder()
                        .strategyId(strategyId)
                        .weightType(w.getWeightType())
                        .weightValue(w.getWeightValue())
                        .createTime(now)
                        .updateTime(now)
                        .build();
                strategyWeightMapper.insert(sw);
                weights.add(sw);
            }
        }
        return weights;
    }

    /**
     * 阶段2: 算法预筛评分
     * 用 CompositeScorer 对全量候选打分 → 按综合得分降序 → Top N 截断
     */
    private PreScreenResult preScreenAndScore(List<Question> candidates, PaperStrategy strategy,
                                               List<StrategyWeight> weights) {
        AssemblyContext context = AssemblyContext.from(strategy, weights, Collections.emptyList());
        CompositeScorer scorer = new CompositeScorer(context);

        List<QuestionScore> scored = candidates.stream()
                .map(scorer::scoreWithDetail)
                .filter(qs -> qs.getCompositeScore() > 0)
                .sorted(Comparator.comparingDouble(QuestionScore::getCompositeScore).reversed())
                .collect(Collectors.toList());

        // 确保覆盖所有题型 — 每种题型至少保留前3道
        Set<Integer> coveredTypes = new HashSet<>();
        List<QuestionScore> topN = new ArrayList<>();
        List<QuestionScore> rest = new ArrayList<>();

        for (QuestionScore qs : scored) {
            if (topN.size() >= PRESCREEN_TOP_N) {
                break;
            }
            Integer type = qs.getType();
            if (!coveredTypes.contains(type)) {
                topN.add(qs);
                coveredTypes.add(type);
            } else if (topN.size() < PRESCREEN_TOP_N - 4) {
                // 前 N-4 个位置按得分填充，留 4 个给未覆盖题型
                topN.add(qs);
            } else {
                rest.add(qs);
            }
        }
        // 如果还有空间，从 rest 补充
        for (QuestionScore qs : rest) {
            if (topN.size() >= PRESCREEN_TOP_N) break;
            topN.add(qs);
        }

        return new PreScreenResult(topN, context.getConstraints(), scored.size());
    }

    /**
     * 阶段3: 构建带评分的候选题目 prompt
     */
    private String buildStage3Prompt(AIPaperAssemblyV2DTO dto, AIProfileVO profile,
                                      PreScreenResult preScreen, StrategyInferenceService.InferredParams params) {
        StringBuilder sb = new StringBuilder();

        // 策略摘要
        sb.append("【组卷策略】（由AI推断）\n");
        sb.append("- 描述: ").append(params.getStrategyDescription() != null ? params.getStrategyDescription() : "均衡组卷").append("\n");
        sb.append("- 平均难度: ").append(params.getDifficultyAvg()).append("/5\n");

        if (params.getQuestionTypeConfig() != null) {
            sb.append("- 题型配置: ");
            for (StrategyInferenceService.TypeConfigItem tc : params.getQuestionTypeConfig()) {
                sb.append(getQuestionTypeText(tc.getType())).append("×").append(tc.getCount()).append(" ");
            }
            sb.append("\n");
        }
        if (params.getDifficultyConfig() != null) {
            sb.append("- 难度分布: ");
            for (StrategyInferenceService.DiffConfigItem dc : params.getDifficultyConfig()) {
                sb.append(getDifficultyText(dc.getLevel())).append(" ").append((int)(dc.getRatio() * 100)).append("% ");
            }
            sb.append("\n");
        }
        if (params.getKnowledgePointScope() != null && !params.getKnowledgePointScope().isEmpty()) {
            sb.append("- 重点知识点: ").append(String.join(", ", params.getKnowledgePointScope())).append("\n");
        }
        sb.append("\n");

        // 个性化画像
        if (profile != null) {
            sb.append("【用户学习画像】\n");
            sb.append("- 总答题数: ").append(profile.getAnswerNum())
              .append(", 正确率: ").append(profile.getAccuracy() != null ? profile.getAccuracy() : "N/A").append("%\n");
            if (profile.getWeakPoints() != null && !profile.getWeakPoints().isEmpty()) {
                sb.append("- 薄弱知识点: ");
                profile.getWeakPoints().stream().limit(5).forEach(wp ->
                    sb.append(wp.getKnowledgePoint()).append("(").append(wp.getAccuracy()).append("%) "));
                sb.append("\n");
            }
            sb.append("\n");
        }

        // 用户需求
        if (StrUtil.isNotBlank(dto.getUserRequirement())) {
            sb.append("【用户需求】\n").append(dto.getUserRequirement()).append("\n\n");
        }

        // 预筛候选（含评分）
        sb.append("【算法预筛候选题目】（已按综合评分降序排列，共").append(preScreen.topCandidates.size()).append("道）\n");
        sb.append("每道题格式: 序号. ID=xxx, 题型=xx, 难度=xx, 综合分=x.xxxx, 知识点=xx\n");
        sb.append("说明: 综合分(compositeScore)越高代表越匹配组卷策略，请优先选择高分题。\n\n");

        for (int i = 0; i < preScreen.topCandidates.size(); i++) {
            QuestionScore qs = preScreen.topCandidates.get(i);
            Question q = qs.getQuestion();
            sb.append(String.format("%d. ID=%d, 题型=%s, 难度=%s, 综合分=%.4f, 学科=%s, 知识点=%s\n",
                    i + 1, q.getId(),
                    getQuestionTypeText(q.getType()),
                    getDifficultyText(q.getDifficulty()),
                    qs.getCompositeScore(),
                    q.getSubject() != null ? q.getSubject() : "未分类",
                    q.getKnowledgePoints() != null ? q.getKnowledgePoints() : "无"));
        }

        sb.append("\n请从以上候选题目中选择题目ID，返回JSON格式：{\"questionIds\": [id1, id2, ...]}");
        return sb.toString();
    }

    /**
     * 后置校验 + 贪心补位
     * 1. 过滤 LLM 返回的无效 ID（不在预筛池中的）
     * 2. ConstraintValidator 检查题型/分数等硬约束
     * 3. 不足时从剩余候选中按得分贪心补位
     */
    private List<QuestionScore> validateAndFillGaps(List<Long> aiSelectedIds,
                                                     List<QuestionScore> topCandidates,
                                                     AssemblyConstraint constraints) {
        // 构建 ID → QuestionScore 映射
        Map<Long, QuestionScore> poolMap = new LinkedHashMap<>();
        for (QuestionScore qs : topCandidates) {
            poolMap.putIfAbsent(qs.getQuestionId(), qs);
        }

        // 过滤有效 ID
        List<QuestionScore> selected = new ArrayList<>();
        Set<Long> selectedIds = new HashSet<>();
        for (Long id : aiSelectedIds) {
            QuestionScore qs = poolMap.get(id);
            if (qs != null && selectedIds.add(id)) {
                selected.add(qs);
            }
        }
        log.info("[AI组卷A+C] LLM 返回 {} 个ID, 有效 {} 个", aiSelectedIds.size(), selected.size());

        // 约束校验
        ConstraintValidator validator = new ConstraintValidator(constraints);
        List<AssemblyDegradeHintDTO> hints = validator.validateWithHints(selected);
        if (!hints.isEmpty()) {
            log.info("[AI组卷A+C] 后置校验发现 {} 条降级提示", hints.size());
        }

        // 贪心补位：从剩余候选中按得分补足
        boolean needsFill = !validator.validate(selected, true);
        if (needsFill) {
            for (QuestionScore qs : topCandidates) {
                if (selectedIds.contains(qs.getQuestionId())) continue;
                if (validator.canAdd(selected, qs)) {
                    selected.add(qs);
                    selectedIds.add(qs.getQuestionId());
                }
            }
            log.info("[AI组卷A+C] 贪心补位后共 {} 道题", selected.size());
        }

        return selected;
    }

    /**
     * 构建最终返回的 VO
     */
    private AIAssemblyStrategyVO buildResultVO(List<QuestionScore> finalSelection,
                                                StrategyInferenceService.StrategyInferenceResult inferenceResult,
                                                PaperStrategy strategy, PreScreenResult preScreen,
                                                boolean llmSuccess, int retryCount) {
        StrategyInferenceService.InferredParams params = inferenceResult.getParams();

        List<Long> questionIds = finalSelection.stream()
                .map(QuestionScore::getQuestionId)
                .collect(Collectors.toList());

        int totalScore = 0;
        if (params.getQuestionTypeConfig() != null) {
            // 按策略中的题型分值计算实际总分
            Map<Integer, Integer> typeScoreMap = params.getQuestionTypeConfig().stream()
                    .collect(Collectors.toMap(
                            StrategyInferenceService.TypeConfigItem::getType,
                            StrategyInferenceService.TypeConfigItem::getScore,
                            (a, b) -> a));
            for (QuestionScore qs : finalSelection) {
                totalScore += typeScoreMap.getOrDefault(qs.getType(), 10);
            }
        } else {
            totalScore = finalSelection.size() * 10;
        }

        // 转换难度配置
        List<AIAssemblyStrategyVO.DifficultyConfig> diffConfigs = null;
        if (params.getDifficultyConfig() != null) {
            diffConfigs = params.getDifficultyConfig().stream()
                    .map(d -> AIAssemblyStrategyVO.DifficultyConfig.builder()
                            .level(d.getLevel()).ratio(d.getRatio()).build())
                    .collect(Collectors.toList());
        }

        // 转换题型配置
        List<AIAssemblyStrategyVO.TypeConfig> typeConfigs = null;
        if (params.getQuestionTypeConfig() != null) {
            typeConfigs = params.getQuestionTypeConfig().stream()
                    .map(t -> AIAssemblyStrategyVO.TypeConfig.builder()
                            .type(t.getType()).count(t.getCount()).score(t.getScore()).build())
                    .collect(Collectors.toList());
        }

        String kpScope = params.getKnowledgePointScope() != null
                ? String.join(",", params.getKnowledgePointScope()) : null;

        // 阶段详情
        String stageDetail = (inferenceResult.isInferenceSuccess() ? "AI推断策略" : "默认策略")
                + " → 算法预筛" + preScreen.totalCandidates + "→" + preScreen.topCandidates.size() + "道"
                + (llmSuccess ? " → LLM精选" : " → LLM失败降级")
                + " → 最终" + questionIds.size() + "道"
                + (retryCount > 0 ? " (重试" + retryCount + "次)" : "");

        return AIAssemblyStrategyVO.builder()
                .strategyId(strategy.getId())
                .strategyDescription(params.getStrategyDescription())
                .stageDetail(stageDetail)
                .difficultyAvg(params.getDifficultyAvg())
                .difficultyConfig(diffConfigs)
                .questionTypeConfig(typeConfigs)
                .knowledgePointScope(kpScope)
                .questionIds(questionIds)
                .totalQuestions(questionIds.size())
                .actualTotalScore(totalScore)
                .build();
    }

    /**
     * LLM 失败时降级为纯贪心结果
     */
    private AIAssemblyStrategyVO buildFallbackResult(PreScreenResult preScreen,
                                                      StrategyInferenceService.StrategyInferenceResult inferenceResult) {
        // 用贪心算法从 Top 候选中选题
        com.kanade.backend.assembly.GreedyAlgorithm greedy =
                new com.kanade.backend.assembly.GreedyAlgorithm(
                        new CompositeScorer(AssemblyContext.from(
                                PaperStrategy.builder()
                                        .difficultyAvg(inferenceResult.getParams().getDifficultyAvg())
                                        .totalScore(150).build(),
                                Collections.emptyList(), Collections.emptyList())),
                        preScreen.constraints);

        com.kanade.backend.assembly.GreedyAlgorithm.GreedyResult greedyResult =
                greedy.assemble(new ArrayList<>(preScreen.topCandidates));

        List<QuestionScore> selected = greedyResult.selected();
        List<Long> questionIds = selected.stream()
                .map(QuestionScore::getQuestionId).collect(Collectors.toList());

        return AIAssemblyStrategyVO.builder()
                .questionIds(questionIds)
                .totalQuestions(questionIds.size())
                .actualTotalScore(questionIds.size() * 10)
                .difficultyAvg(inferenceResult.getParams().getDifficultyAvg())
                .build();
    }

    /**
     * 候选为空时的空结果
     */
    private AIAssemblyStrategyVO buildEmptyResult(StrategyInferenceService.StrategyInferenceResult inferenceResult,
                                                   PaperStrategy strategy, String errorMsg) {
        return AIAssemblyStrategyVO.builder()
                .questionIds(Collections.emptyList())
                .totalQuestions(0)
                .actualTotalScore(0)
                .difficultyAvg(inferenceResult.getParams().getDifficultyAvg())
                .build();
    }

    /**
     * 预筛结果内部类
     */
    @lombok.Value
    private static class PreScreenResult {
        List<QuestionScore> topCandidates;
        AssemblyConstraint constraints;
        int totalCandidates;
    }
}