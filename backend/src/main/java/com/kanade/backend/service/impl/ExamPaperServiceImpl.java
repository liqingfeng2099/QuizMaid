package com.kanade.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.kanade.backend.ai.AiService;
import com.kanade.backend.ai.AiServiceFactory;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.mapper.ExamPaperMapper;
import com.kanade.backend.mapper.PaperquestionMapper;
import com.kanade.backend.mapper.UserMapper;
import com.kanade.backend.model.dto.AIPaperAssemblyDTO;
import com.kanade.backend.model.dto.AIPaperAssemblyV2DTO;
import com.kanade.backend.model.dto.ExamPaperQueryDTO;
import com.kanade.backend.model.entity.*;
import com.kanade.backend.model.vo.*;
import com.kanade.backend.service.AiPaperChatService;
import com.kanade.backend.service.ExamPaperService;
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

    // ==================== AI增强组卷（任务5） ====================

    /**
     * 增强版AI组卷：个性化提示词 + 重试机制 + 对话记录
     */
    @Transactional(rollbackFor = Exception.class)
    public AIAssemblyStrategyVO aiAssemblePaperV2(AIPaperAssemblyV2DTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 1. 构建用户学习画像
        AIProfileVO profile = null;
        if (Boolean.TRUE.equals(dto.getUsePersonalization())) {
            profile = buildUserProfile(userId);
        }

        // 2. 过滤候选题目
        List<Question> candidates = filterQuestionsV2(dto, userId);

        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有符合条件的题目，请调整筛选条件或先添加题目");
        }

        // 3. 构建增强提示词
        String prompt = buildEnhancedPrompt(dto, candidates, profile);

        // 4. 重试调用AI（最多3次）
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
                log.warn("[AI组卷] 第{}次调用失败: {}", attempt + 1, e.getMessage());
                retryCount = attempt + 1;
                if (attempt < 2) {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }
            }
        }

        if (aiResponse == null || aiResponse.isBlank()) {
            // 保存失败记录
            aiPaperChatService.saveChat(userId, null, null, prompt, lastError != null ? lastError.getMessage() : "无响应", 2, retryCount);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "AI组卷失败（已重试" + retryCount + "次），建议切换手动组卷模式");
        }

        // 5. 解析AI响应
        AIAssemblyStrategyVO result = parseAIResponseV2(aiResponse, candidates);

        // 6. 保存对话记录
        aiPaperChatService.saveChat(userId, null, null, dto.getUserRequirement(), aiResponse, 1, retryCount);

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

    /**
     * 构建增强版AI提示词（含个性化数据）
     */
    private String buildEnhancedPrompt(AIPaperAssemblyV2DTO dto, List<Question> candidates, AIProfileVO profile) {
        StringBuilder sb = new StringBuilder();

        // 个性化画像
        if (profile != null) {
            sb.append("【用户学习画像】\n");
            sb.append("- 总答题数: ").append(profile.getAnswerNum())
              .append(", 正确率: ").append(profile.getAccuracy() != null ? profile.getAccuracy() : "N/A").append("%\n");
            if (profile.getWeakPoints() != null && !profile.getWeakPoints().isEmpty()) {
                sb.append("- 薄弱知识点: ");
                profile.getWeakPoints().stream().limit(5).forEach(wp ->
                    sb.append(wp.getKnowledgePoint()).append("(正确率").append(wp.getAccuracy()).append("%) "));
                sb.append("\n");
            }
            sb.append("\n");
        }

        // 组卷要求
        sb.append("【组卷要求】\n");
        if (StrUtil.isNotBlank(dto.getSubject())) sb.append("- 学科：").append(dto.getSubject()).append("\n");
        if (StrUtil.isNotBlank(dto.getChapter())) sb.append("- 章节：").append(dto.getChapter()).append("\n");
        if (dto.getDifficulty() != null) sb.append("- 难度：").append(getDifficultyText(dto.getDifficulty())).append("\n");
        if (dto.getTotalScore() != null) sb.append("- 目标总分：").append(dto.getTotalScore()).append("\n");
        if (Boolean.TRUE.equals(dto.getIncludeWeakAreas()) && profile != null && !profile.getWeakPoints().isEmpty()) {
            sb.append("- 重点：请优先选择薄弱知识点相关的题目\n");
        }
        sb.append("\n");

        // 用户需求
        if (StrUtil.isNotBlank(dto.getUserRequirement())) {
            sb.append("【用户需求】\n").append(dto.getUserRequirement()).append("\n\n");
        }

        // 候选题目
        sb.append("【候选题目列表】\n");
        sb.append("以下是可供选择的题目信息（ID, 题型, 难度, 学科, 章节, 知识点, 标签）：\n");
        for (int i = 0; i < candidates.size(); i++) {
            Question q = candidates.get(i);
            sb.append(String.format("%d. ID=%d, 题型=%s, 难度=%s, 学科=%s, 章节=%s, 知识点=%s, 标签=%s\n",
                    i + 1, q.getId(), getQuestionTypeText(q.getType()),
                    getDifficultyText(q.getDifficulty()),
                    q.getSubject() != null ? q.getSubject() : "未分类",
                    q.getChapter() != null ? q.getChapter() : "未分类",
                    q.getKnowledgePoints() != null ? q.getKnowledgePoints() : "无",
                    q.getTags() != null ? q.getTags() : "无"));
        }

        sb.append("\n请返回JSON格式（含strategy和questionIds）：\n");
        sb.append("{\"strategy\":{\"difficultyAvg\":3,\"difficultyConfig\":[{\"level\":1,\"ratio\":0.2}],\"questionTypeConfig\":[{\"type\":1,\"count\":10,\"score\":5}]},\"questionIds\":[1,2,3]}");
        return sb.toString();
    }

    /**
     * 解析AI v2响应（含strategy）
     */
    private AIAssemblyStrategyVO parseAIResponseV2(String aiResponse, List<Question> candidates) {
        try {
            String json = aiResponse.trim();
            if (json.startsWith("```")) json = json.substring(json.indexOf("\n") + 1);
            if (json.endsWith("```")) json = json.substring(0, json.lastIndexOf("```")).trim();
            if (json.startsWith("json")) json = json.substring(4).trim();

            Map<String, Object> map = JSONUtil.toBean(json, Map.class);
            AIAssemblyStrategyVO vo = new AIAssemblyStrategyVO();

            // 解析 strategy
            Object strategyObj = map.get("strategy");
            if (strategyObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sMap = (Map<String, Object>) strategyObj;
                vo.setDifficultyAvg(sMap.get("difficultyAvg") instanceof Number ? ((Number) sMap.get("difficultyAvg")).intValue() : 3);
            }

            // 解析 questionIds
            Object idsObj = map.get("questionIds");
            if (idsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Number> ids = (List<Number>) idsObj;
                vo.setQuestionIds(ids.stream().map(Number::longValue).collect(Collectors.toList()));
            } else {
                vo.setQuestionIds(Collections.emptyList());
            }

            vo.setTotalQuestions(vo.getQuestionIds().size());
            vo.setActualTotalScore(vo.getTotalQuestions() * 10);

            return vo;
        } catch (Exception e) {
            log.error("[AI组卷v2] 解析响应失败: {}", aiResponse, e);
            // 降级：尝试旧版parseAIResponse
            List<Long> ids = parseAIResponse(aiResponse);
            return AIAssemblyStrategyVO.builder()
                    .questionIds(ids)
                    .totalQuestions(ids.size())
                    .actualTotalScore(ids.size() * 10)
                    .build();
        }
    }
}