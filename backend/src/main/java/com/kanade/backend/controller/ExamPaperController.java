package com.kanade.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.DeleteRequest;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.mapper.PaperquestionMapper;
import com.kanade.backend.model.dto.*;
import com.kanade.backend.model.entity.ExamPaper;
import com.kanade.backend.model.entity.Paperquestion;
import com.kanade.backend.model.vo.*;
import com.kanade.backend.service.AiPaperChatService;
import com.kanade.backend.service.ExamPaperService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/examPaper")
@RequiredArgsConstructor
@Tag(name = "试卷管理")
public class ExamPaperController {

    private final ExamPaperService examPaperService;
    private final PaperquestionMapper paperquestionMapper;
    private final AiPaperChatService aiPaperChatService;

    @PostMapping("/add")
    @SaCheckLogin
    @Operation(summary = "添加试卷")
    public BaseResponse<Long> addExamPaper(@RequestBody ExamPaperAddDTO addDTO) {
        ExamPaper paper = new ExamPaper();
        BeanUtils.copyProperties(addDTO, paper);
        Long id = examPaperService.addExamPaper(paper);
        return ResultUtils.success(id);
    }

    @PostMapping("/update")
    @SaCheckLogin
    @Operation(summary = "更新试卷")
    public BaseResponse<Boolean> updateExamPaper(@RequestBody ExamPaperUpdateDTO updateDTO) {
        if (updateDTO.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "试卷ID不能为空");
        }
        ExamPaper paper = new ExamPaper();
        BeanUtils.copyProperties(updateDTO, paper);
        boolean result = examPaperService.updateExamPaper(paper);
        return ResultUtils.success(result);
    }

    @PostMapping("/status")
    @SaCheckLogin
    @Operation(summary = "修改试卷状态")
    public BaseResponse<Boolean> updateStatus(@RequestBody ExamPaperStatusDTO statusDTO) {
        if (statusDTO.getId() == null || statusDTO.getStatus() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不完整");
        }
        boolean result = examPaperService.updateStatus(statusDTO.getId(), statusDTO.getStatus());
        return ResultUtils.success(result);
    }

    @PostMapping("/delete")
    @SaCheckLogin
    @Operation(summary = "逻辑删除试卷")
    public BaseResponse<Boolean> deleteExamPaper(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "试卷ID不能为空");
        }
        boolean result = examPaperService.deleteExamPaper(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    @PostMapping("/copy/{id}")
    @SaCheckLogin
    @Operation(summary = "复制试卷（含题目关联），新试卷状态为草稿")
    public BaseResponse<ExamPaperVO> copyExamPaper(@PathVariable Long id) {
        ExamPaperVO vo = examPaperService.copyExamPaper(id);
        return ResultUtils.success(vo);
    }

    @GetMapping("/get/{id}")
    @SaCheckLogin
    @Operation(summary = "根据ID获取试卷详情（含试题列表）")
    public BaseResponse<ExamPaperVO> getExamPaperById(@PathVariable Long id) {
        ExamPaperVO vo = examPaperService.getExamPaperVOById(id);
        return ResultUtils.success(vo);
    }

    @PostMapping("/list/page")
    @SaCheckLogin
    @Operation(summary = "分页查询试卷（普通用户只能看到自己的，管理员可查所有）")
    public BaseResponse<Page<ExamPaperVO>> listExamPaperByPage(@RequestBody ExamPaperQueryDTO queryDTO) {
        Page<ExamPaperVO> page = examPaperService.getExamPaperPage(queryDTO);
        return ResultUtils.success(page);
    }

    @PostMapping("/ai/assemble")
    @SaCheckLogin // ← Sa-Token拦截器验证登录状态
    @Operation(summary = "AI智能组卷")
    public BaseResponse<AIPaperAssemblyResultVO> aiAssemblePaper(@RequestBody AIPaperAssemblyDTO assemblyDTO) {
        // 调用Service层
        AIPaperAssemblyResultVO result = examPaperService.aiAssemblePaper(assemblyDTO);
        return ResultUtils.success(result);
    }

    @PostMapping("/assemble/save")
    @SaCheckLogin
    @Operation(summary = "保存组卷结果为试卷")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<ExamPaperVO> saveAssembleResult(@Valid @RequestBody AssemblySaveDTO saveDTO) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 1. 创建试卷
        ExamPaper paper = new ExamPaper();
        paper.setPaperName(saveDTO.getPaperName());
        paper.setSubject(saveDTO.getSubject() != null ? saveDTO.getSubject() : "综合");
        paper.setTotalScore(0);
        paper.setCreatorId(userId);
        paper.setStatus(saveDTO.getStatus() != null ? saveDTO.getStatus() : 0);
        paper.setStrategyId(saveDTO.getStrategyId());
        paper.setPaperType(1); // 手动组卷
        examPaperService.save(paper);

        // 2. 批量创建题目关联
        LocalDateTime now = LocalDateTime.now();
        int totalScore = 0;
        int sort = 1;
        for (AssemblySaveDTO.QuestionItem item : saveDTO.getQuestions()) {
            Paperquestion pq = Paperquestion.builder()
                    .paperId(paper.getId())
                    .questionId(item.getQuestionId())
                    .questionScore(item.getQuestionScore())
                    .sort(item.getSort() != null ? item.getSort() : sort++)
                    .isAutoAdd(1)
                    .createTime(now)
                    .build();
            paperquestionMapper.insert(pq);
            totalScore += item.getQuestionScore();
        }

        // 3. 更新总分
        paper.setTotalScore(totalScore);
        examPaperService.updateById(paper);

        // 4. 返回试卷详情
        ExamPaperVO vo = examPaperService.getExamPaperVOById(paper.getId());
        return ResultUtils.success(vo);
    }

    // ==================== AI增强组卷（任务5） ====================

    @PostMapping("/ai/profile")
    @SaCheckLogin
    @Operation(summary = "获取用户学习画像（AI组卷个性化预填）")
    public BaseResponse<AIProfileVO> getAIProfile() {
        Long userId = StpUtil.getLoginIdAsLong();
        AIProfileVO profile = examPaperService.buildUserProfile(userId);
        return ResultUtils.success(profile);
    }

    @PostMapping("/ai/assemble/v2")
    @SaCheckLogin
    @Operation(summary = "增强版AI组卷（个性化提示词+重试机制+回显策略）")
    public BaseResponse<AIAssemblyStrategyVO> aiAssemblePaperV2(@Valid @RequestBody AIPaperAssemblyV2DTO dto) {
        AIAssemblyStrategyVO result = examPaperService.aiAssemblePaperV2(dto);
        return ResultUtils.success(result);
    }

    @PostMapping("/ai/confirm")
    @SaCheckLogin
    @Operation(summary = "确认AI组卷方案并创建试卷")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<ExamPaperVO> confirmAIAssembly(jakarta.servlet.http.HttpServletRequest request) throws java.io.IOException {
        Map<String, Object> body = parseJsonOrForm(request);

        AIPaperAssemblyV2DTO dto = new AIPaperAssemblyV2DTO();
        dto.setPaperName(getString(body, "paperName"));
        dto.setSubject(getString(body, "subject"));
        dto.setStatus(getInt(body, "status", 0));
        dto.setTotalScore(getInt(body, "totalScore", null));

        // 解析 strategy（可能是 Map 或 JSON 字符串）
        AIAssemblyStrategyVO strategy = new AIAssemblyStrategyVO();
        Object strategyObj = body.get("strategy");
        if (strategyObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> sMap = (Map<String, Object>) strategyObj;
            parseStrategyIds(sMap, strategy);
        } else if (strategyObj instanceof String) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> sMap = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue((String) strategyObj, Map.class);
                parseStrategyIds(sMap, strategy);
            } catch (Exception ignored) {}
        }

        ExamPaperVO vo = examPaperService.confirmAIAssembly(dto, strategy);
        return ResultUtils.success(vo);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonOrForm(jakarta.servlet.http.HttpServletRequest request) throws java.io.IOException {
        byte[] raw = request.getInputStream().readAllBytes();
        if (raw.length > 0) {
            String text = new String(raw, java.nio.charset.StandardCharsets.UTF_8).trim();
            if (text.startsWith("{")) {
                try { return new com.fasterxml.jackson.databind.ObjectMapper().readValue(text, Map.class); } catch (Exception ignored) {}
            }
        }
        Map<String, Object> map = new LinkedHashMap<>();
        java.util.Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            map.put(name, request.getParameter(name));
        }
        return map;
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private Integer getInt(Map<String, Object> map, String key, Integer defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) {
            try { return Integer.parseInt((String) val); } catch (NumberFormatException e) { return defaultVal; }
        }
        return defaultVal;
    }

    @SuppressWarnings("unchecked")
    private void parseStrategyIds(Map<String, Object> sMap, AIAssemblyStrategyVO strategy) {
        if (sMap != null && sMap.get("questionIds") instanceof List) {
            List<Number> ids = (List<Number>) sMap.get("questionIds");
            strategy.setQuestionIds(ids.stream().map(Number::longValue).collect(java.util.stream.Collectors.toList()));
        }
    }

    @PostMapping("/ai/chat/history")
    @SaCheckLogin
    @Operation(summary = "查询AI组卷对话记录")
    public BaseResponse<List<AIChatVO>> getAIChatHistory(@RequestBody AIChatQueryDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<AIChatVO> history = aiPaperChatService.getChatHistory(userId, dto.getLimit());
        return ResultUtils.success(history);
    }

    @PostMapping("/ai/chat/reuse/{chatId}")
    @SaCheckLogin
    @Operation(summary = "从历史对话复用组卷策略")
    public BaseResponse<AIAssemblyStrategyVO> reuseChatStrategy(@PathVariable Long chatId) {
        Long userId = StpUtil.getLoginIdAsLong();
        AIChatVO chat = aiPaperChatService.getChatById(chatId, userId);
        if (chat == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "对话记录不存在");
        }
        AIAssemblyStrategyVO strategy = new AIAssemblyStrategyVO();
        try {
            Map<String, Object> map = cn.hutool.json.JSONUtil.toBean(chat.getAiResponse(), Map.class);
            Object idsObj = map.get("questionIds");
            if (idsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Number> ids = (List<Number>) idsObj;
                strategy.setQuestionIds(ids.stream().map(Number::longValue).collect(java.util.stream.Collectors.toList()));
                strategy.setTotalQuestions(strategy.getQuestionIds().size());
                strategy.setActualTotalScore(strategy.getTotalQuestions() * 10);
            }
        } catch (Exception e) {
            strategy.setQuestionIds(java.util.Collections.emptyList());
        }
        return ResultUtils.success(strategy);
    }
}