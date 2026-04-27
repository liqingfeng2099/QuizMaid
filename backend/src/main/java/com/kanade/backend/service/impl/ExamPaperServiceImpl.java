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
import com.kanade.backend.model.dto.AIPaperAssemblyDTO;
import com.kanade.backend.model.dto.ExamPaperQueryDTO;
import com.kanade.backend.model.entity.ExamPaper;
import com.kanade.backend.model.entity.Paperquestion;
import com.kanade.backend.model.entity.Question;
import com.kanade.backend.model.vo.AIPaperAssemblyResultVO;
import com.kanade.backend.model.vo.ExamPaperVO;
import com.kanade.backend.model.vo.PaperQuestionVO;
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
    public boolean updateStatus(Long id, Integer status) {
        ExamPaper paper = new ExamPaper();
        paper.setId(id);
        paper.setStatus(status);
        return updateExamPaper(paper); // 复用权限校验
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
        // 逻辑删除，MyBatis-Flex 自动处理
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
            List<Question> questions = questionService.listByIds(questionIds);
            List<PaperQuestionVO> questionVOs = relations.stream().map(rel -> {
                PaperQuestionVO qvo = new PaperQuestionVO();
                qvo.setId(rel.getId());
                qvo.setQuestionId(rel.getQuestionId());
                qvo.setQuestionScore(rel.getQuestionScore());
                qvo.setSort(rel.getSort());
                // 补充题干和题型
                questions.stream().filter(q -> q.getId().equals(rel.getQuestionId())).findFirst().ifPresent(q -> {
                    qvo.setQuestionContent(q.getContent());
                    qvo.setType(q.getType());
                });
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
}