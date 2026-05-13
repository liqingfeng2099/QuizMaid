package com.kanade.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.model.dto.ExamSubmitDTO;
import com.kanade.backend.model.vo.ExamRecordVO;
import com.kanade.backend.model.vo.ExamResultVO;
import com.kanade.backend.service.ExamRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/exam")
@RequiredArgsConstructor
@Tag(name = "在线考试")
public class ExamController {

    private final ExamRecordService examRecordService;

    @PostMapping("/start/{paperId}")
    @SaCheckLogin
    @Operation(summary = "开始考试（创建考试记录并返回试题）")
    public BaseResponse<ExamRecordVO> startExam(@PathVariable Long paperId) {
        ExamRecordVO vo = examRecordService.startExam(paperId);
        return ResultUtils.success(vo);
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/submit")
    @SaCheckLogin
    @Operation(summary = "提交考试（保存答案并自动批改客观题）")
    public BaseResponse<ExamResultVO> submitExam(@RequestBody ExamSubmitDTO body) {
        if (body.getRecordId() == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "考试记录ID不能为空");

        Map<Long, String> answers = new HashMap<>();
        Object ansObj = body.getAnswers();

        if (ansObj instanceof List<?> list) {
            // 格式: [{"questionId": 1, "userAnswer": "A"}, ...]
            for (Object item : list) {
                if (item instanceof Map<?,?> m) {
                    Long qid = m.get("questionId") instanceof Number n ? n.longValue() : null;
                    String ua = m.get("userAnswer") != null ? m.get("userAnswer").toString() : "";
                    if (qid != null) answers.put(qid, ua);
                }
            }
        } else if (ansObj instanceof Map<?,?> map) {
            // 格式: {"1": "A", "2": "BCD", ...}
            map.forEach((k, v) -> {
                try {
                    Long qid = Long.parseLong(k.toString());
                    answers.put(qid, v != null ? v.toString() : "");
                } catch (NumberFormatException ignored) {}
            });
        }

        ExamResultVO vo = examRecordService.submitExam(body.getRecordId(), answers);
        return ResultUtils.success(vo);
    }

    @PostMapping("/auto-submit/{recordId}")
    @SaCheckLogin
    @Operation(summary = "超时自动交卷")
    public BaseResponse<ExamResultVO> autoSubmit(@PathVariable Long recordId) {
        ExamResultVO vo = examRecordService.autoSubmit(recordId);
        return ResultUtils.success(vo);
    }

    @GetMapping("/result/{recordId}")
    @SaCheckLogin
    @Operation(summary = "查看考试成绩")
    public BaseResponse<ExamResultVO> getExamResult(@PathVariable Long recordId) {
        ExamResultVO vo = examRecordService.getExamResult(recordId);
        return ResultUtils.success(vo);
    }

    @GetMapping("/ongoing/{paperId}")
    @SaCheckLogin
    @Operation(summary = "获取进行中的考试（用于恢复考试）")
    public BaseResponse<ExamRecordVO> getOngoingExam(@PathVariable Long paperId) {
        ExamRecordVO vo = examRecordService.getOngoingExam(paperId);
        return ResultUtils.success(vo);
    }

    @GetMapping("/records")
    @SaCheckLogin
    @Operation(summary = "获取考试记录列表")
    public BaseResponse<List<ExamRecordVO>> getExamRecords(
            @RequestParam(required = false) Long paperId) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<ExamRecordVO> list = examRecordService.getUserExamRecords(userId, paperId);
        return ResultUtils.success(list);
    }
}
