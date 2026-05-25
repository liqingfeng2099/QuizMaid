package com.kanade.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.mapper.ErrorBookGroupItemMapper;
import com.kanade.backend.mapper.ErrorBookGroupMapper;
import com.kanade.backend.mapper.ErrorBookNoteMapper;
import com.kanade.backend.model.dto.RecommendQueryDTO;
import com.kanade.backend.model.entity.ErrorBook;
import com.kanade.backend.model.entity.ErrorBookGroup;
import com.kanade.backend.model.entity.ErrorBookGroupItem;
import com.kanade.backend.model.entity.ErrorBookNote;
import com.kanade.backend.model.vo.ExamPaperVO;
import com.kanade.backend.model.vo.QuestionVO;
import com.kanade.backend.model.entity.ErrorBookExportLog;
import com.kanade.backend.service.*;
import com.mybatisflex.core.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/error-book")
@RequiredArgsConstructor
@Tag(name = "错题本高级功能")
public class ErrorBookExtController {

    private final RecommendService recommendService;
    private final ErrorBookService errorBookService;
    private final ErrorBookExportService exportService;
    private final ErrorBookAssemblyService assemblyService;
    private final ErrorBookExportLogService exportLogService;
    private final ErrorBookGroupMapper groupMapper;
    private final ErrorBookGroupItemMapper groupItemMapper;
    private final ErrorBookNoteMapper noteMapper;

    // ===== 同类错题推荐 =====

    @PostMapping(value = "/recommend", consumes = {"application/json", "application/x-www-form-urlencoded"})
    @SaCheckLogin
    @Operation(summary = "同类错题智能推荐")
    public BaseResponse<List<QuestionVO>> recommend(@RequestBody RecommendQueryDTO dto) {
        return ResultUtils.success(recommendService.recommendSimilarQuestions(dto));
    }

    @PostMapping(value = "/recommend/feedback/{questionId}", consumes = {"application/json", "application/x-www-form-urlencoded"})
    @SaCheckLogin
    @Operation(summary = "推荐效果反馈")
    public BaseResponse<Boolean> feedback(@PathVariable Long questionId,
            @RequestBody(required = false) Map<String, Boolean> body,
            @RequestParam(value = "correct", required = false) Boolean correct) {
        Boolean c = correct;
        if (c == null && body != null) c = body.getOrDefault("correct", false);
        recommendService.recordFeedback(questionId, c != null && c);
        return ResultUtils.success(true);
    }

    // ===== 批量操作 =====

    @PostMapping(value = "/batch/delete", consumes = {"application/json", "application/x-www-form-urlencoded"})
    @SaCheckLogin
    @Operation(summary = "批量删除错题")
    public BaseResponse<Integer> batchDelete(@RequestBody List<Long> ids) {
        int count = 0;
        for (Long id : ids) {
            try { errorBookService.deleteError(id); count++; } catch (Exception ignored) {}
        }
        return ResultUtils.success(count);
    }

    @PostMapping(value = "/batch/review-status", consumes = {"application/json", "application/x-www-form-urlencoded"})
    @SaCheckLogin
    @Operation(summary = "批量更新复习状态")
    public BaseResponse<Integer> batchReviewStatus(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) body.get("ids");
        int status = body.get("reviewStatus") instanceof Number ?
                ((Number) body.get("reviewStatus")).intValue() : 0;
        int count = 0;
        for (Number id : ids) {
            try { errorBookService.updateReviewStatus(id.longValue(), status); count++; } catch (Exception ignored) {}
        }
        return ResultUtils.success(count);
    }

    @PostMapping(value = "/batch/group", consumes = {"application/json", "application/x-www-form-urlencoded"})
    @SaCheckLogin
    @Operation(summary = "批量分组")
    public BaseResponse<Integer> batchGroup(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) body.get("ids");
        Long groupId = body.get("groupId") instanceof Number ?
                ((Number) body.get("groupId")).longValue() : null;
        if (groupId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "分组ID不能为空");
        int count = 0;
        for (Number id : ids) {
            try {
                ErrorBookGroupItem item = new ErrorBookGroupItem();
                item.setGroupId(groupId); item.setErrorBookId(id.longValue());
                item.setCreateTime(java.time.LocalDateTime.now());
                groupItemMapper.insert(item);
                count++;
            } catch (Exception ignored) {}
        }
        return ResultUtils.success(count);
    }

    @GetMapping("/group/items/{groupId}")
    @SaCheckLogin
    @Operation(summary = "查询分组内的错题")
    public BaseResponse<List<ErrorBookGroupItem>> listGroupItems(@PathVariable Long groupId) {
        return ResultUtils.success(groupItemMapper.selectListByQuery(
                QueryWrapper.create().eq("groupId", groupId)));
    }

    @PostMapping(value = "/group/item/remove/{itemId}")
    @SaCheckLogin
    @Operation(summary = "从分组中移除错题")
    public BaseResponse<Boolean> removeGroupItem(@PathVariable Long itemId) {
        groupItemMapper.deleteById(itemId);
        return ResultUtils.success(true);
    }

    // ===== 错题分组 =====

    @PostMapping("/group/create")
    @SaCheckLogin
    @Operation(summary = "创建错题分组")
    public BaseResponse<ErrorBookGroup> createGroup(jakarta.servlet.http.HttpServletRequest request) throws java.io.IOException {
        Map<String, Object> body = parseBody(request);
        ErrorBookGroup group = new ErrorBookGroup();
        group.setUserId(StpUtil.getLoginIdAsLong());
        group.setGroupName(getStr(body, "groupName"));
        group.setDescription(getStr(body, "description"));
        group.setSort(0);
        group.setCreateTime(java.time.LocalDateTime.now());
        group.setUpdateTime(java.time.LocalDateTime.now());
        groupMapper.insert(group);
        return ResultUtils.success(group);
    }

    @GetMapping("/group/list")
    @SaCheckLogin
    @Operation(summary = "查询用户错题分组")
    public BaseResponse<List<ErrorBookGroup>> listGroups() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(groupMapper.selectListByQuery(
                QueryWrapper.create().eq("userId", userId)));
    }

    @PostMapping(value = "/group/delete/{id}")
    @SaCheckLogin
    @Operation(summary = "删除分组")
    public BaseResponse<Boolean> deleteGroup(@PathVariable Long id) {
        groupMapper.deleteById(id);
        return ResultUtils.success(true);
    }

    // ===== 错题备注 =====

    @PostMapping("/note/add")
    @SaCheckLogin
    @Operation(summary = "添加错题备注")
    public BaseResponse<ErrorBookNote> addNote(jakarta.servlet.http.HttpServletRequest request) throws java.io.IOException {
        Map<String, Object> body = parseBody(request);
        ErrorBookNote note = new ErrorBookNote();
        note.setErrorBookId(getLong(body, "errorBookId"));
        note.setUserId(StpUtil.getLoginIdAsLong());
        note.setNoteType(getInt(body, "noteType", 1));
        note.setContent(getStr(body, "content"));
        note.setImageUrl(getStr(body, "imageUrl"));
        note.setCreateTime(java.time.LocalDateTime.now());
        note.setUpdateTime(java.time.LocalDateTime.now());
        noteMapper.insert(note);
        return ResultUtils.success(note);
    }

    @GetMapping("/note/list/{errorBookId}")
    @SaCheckLogin
    @Operation(summary = "查询错题备注")
    public BaseResponse<List<ErrorBookNote>> listNotes(@PathVariable Long errorBookId) {
        return ResultUtils.success(noteMapper.selectListByQuery(
                QueryWrapper.create().eq("errorBookId", errorBookId)));
    }

    // ===== 导出 =====

    @PostMapping(value = "/export/excel", consumes = {"application/json", "application/x-www-form-urlencoded"})
    @SaCheckLogin
    @Operation(summary = "导出错题集为Excel(CSV)")
    public void exportExcel(HttpServletResponse response) throws IOException {
        exportService.exportExcel(response, null);
    }

    @PostMapping(value = "/export/word", consumes = {"application/json", "application/x-www-form-urlencoded"})
    @SaCheckLogin
    @Operation(summary = "导出错题集为Word(.docx)")
    public void exportWord(HttpServletResponse response) throws IOException {
        exportService.exportWord(response);
    }

    @PostMapping(value = "/export/pdf", consumes = {"application/json", "application/x-www-form-urlencoded"})
    @SaCheckLogin
    @Operation(summary = "导出错题集为PDF")
    public void exportPdf(HttpServletResponse response) throws IOException {
        exportService.exportPdf(response);
    }

    @PostMapping(value = "/export/batch", consumes = {"application/json", "application/x-www-form-urlencoded"})
    @SaCheckLogin
    @Operation(summary = "批量导出错题集(ZIP)")
    public void batchExport(@RequestBody Map<String, Object> body, HttpServletResponse response) throws IOException {
        String exportType = body.get("exportType") != null ? body.get("exportType").toString() : "Word";
        exportService.batchExport(response, exportType);
    }

    @GetMapping("/export/preview")
    @SaCheckLogin
    @Operation(summary = "导出前预览HTML")
    public BaseResponse<String> preview() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(exportService.previewHtml(userId));
    }

    // ===== 专项强化组卷 =====

    @PostMapping("/assemble/reinforce")
    @SaCheckLogin
    @Operation(summary = "错题专项强化组卷（≥70%知识点来自错题本）")
    public BaseResponse<ExamPaperVO> reinforceAssemble(jakarta.servlet.http.HttpServletRequest request) throws java.io.IOException {
        Map<String, Object> body = parseBody(request);
        String paperName = getStr(body, "paperName");
        if (paperName == null) paperName = "错题强化卷";
        int questionCount = getInt(body, "questionCount", 15);
        Integer difficulty = getInt(body, "difficultyAvg", null);
        Integer duration = getInt(body, "duration", 45);
        return ResultUtils.success(assemblyService.assembleFromErrors(
                paperName, questionCount, difficulty, duration));
    }

    // ===== 导出日志 =====

    @GetMapping("/export/logs")
    @SaCheckLogin
    @Operation(summary = "获取错题集导出日志")
    public BaseResponse<List<ErrorBookExportLog>> getExportLogs() {
        return ResultUtils.success(exportLogService.getUserLogs());
    }

    // ===== 通用解析 =====

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseBody(jakarta.servlet.http.HttpServletRequest request) throws java.io.IOException {
        byte[] raw = request.getInputStream().readAllBytes();
        if (raw.length > 0) {
            String text = new String(raw, java.nio.charset.StandardCharsets.UTF_8).trim();
            if (text.startsWith("{")) {
                try { return new com.fasterxml.jackson.databind.ObjectMapper().readValue(text, Map.class); } catch (Exception ignored) {}
            }
        }
        // 回退 form params
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        java.util.Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            map.put(name, request.getParameter(name));
        }
        return map;
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        if (v != null) return Long.parseLong(v.toString());
        return null;
    }

    private Integer getInt(Map<String, Object> map, String key, Integer def) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        if (v != null) return Integer.parseInt(v.toString());
        return def;
    }

    private String getStr(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }
}
