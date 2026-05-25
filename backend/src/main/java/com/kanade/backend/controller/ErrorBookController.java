package com.kanade.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.PageRequest;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.model.entity.ErrorBook;
import com.kanade.backend.model.vo.PersonalDimensionVO;
import com.kanade.backend.service.ErrorBookService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/error-book")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "错题本管理")
public class ErrorBookController {

    private final ErrorBookService errorBookService;
    private final StringRedisTemplate stringRedisTemplate;

    @PostMapping("/list")
    @SaCheckLogin
    @Operation(summary = "分页查询错题")
    public BaseResponse<Page<ErrorBook>> listErrors(jakarta.servlet.http.HttpServletRequest request) throws java.io.IOException {
        Map<String, Object> body = parseJsonOrForm(request);
        int pageNum = body.get("pageNum") instanceof Number ? ((Number) body.get("pageNum")).intValue() : 1;
        int pageSize = body.get("pageSize") instanceof Number ? ((Number) body.get("pageSize")).intValue() : 10;
        Integer errorType = body.get("errorType") != null ? ((Number) body.get("errorType")).intValue() : null;
        String kp = body.get("knowledgePoint") != null ? body.get("knowledgePoint").toString() : null;
        Integer reviewStatus = body.get("reviewStatus") != null ? ((Number) body.get("reviewStatus")).intValue() : null;
        String sortBy = body.get("sortBy") != null ? body.get("sortBy").toString() : null;

        Page<ErrorBook> page = errorBookService.getErrorPage(pageNum, pageSize, errorType, kp, reviewStatus, sortBy);
        return ResultUtils.success(page);
    }

    @PostMapping("/review-status/{id}")
    @SaCheckLogin
    @Operation(summary = "更新错题复习状态")
    public BaseResponse<Boolean> updateReviewStatus(@PathVariable Long id,
            jakarta.servlet.http.HttpServletRequest request) throws java.io.IOException {
        Integer status = getIntParam(request, "reviewStatus");
        if (status == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "reviewStatus不能为空");
        errorBookService.updateReviewStatus(id, status);
        return ResultUtils.success(true);
    }

    @PostMapping("/error-type/{id}")
    @SaCheckLogin
    @Operation(summary = "更新错题错误类型")
    public BaseResponse<Boolean> updateErrorType(@PathVariable Long id,
            jakarta.servlet.http.HttpServletRequest request) throws java.io.IOException {
        Integer etype = getIntParam(request, "errorType");
        if (etype == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "errorType不能为空");
        errorBookService.updateErrorType(id, etype);
        return ResultUtils.success(true);
    }

    private Integer getIntParam(jakarta.servlet.http.HttpServletRequest request, String key) throws java.io.IOException {
        String val = request.getParameter(key);
        if (val != null && !val.isEmpty()) return Integer.parseInt(val);
        Map<String, Object> body = parseJsonOrForm(request);
        Object obj = body.get(key);
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj != null) return Integer.parseInt(obj.toString());
        return null;
    }

    @PostMapping("/delete")
    @SaCheckLogin
    @Operation(summary = "删除错题")
    public BaseResponse<Boolean> deleteError(jakarta.servlet.http.HttpServletRequest request) throws java.io.IOException {
        String idStr = request.getParameter("id");
        if (idStr == null || idStr.isEmpty()) {
            Map<String, Object> body = parseJsonOrForm(request);
            Object idObj = body.get("id");
            if (idObj instanceof Number) idStr = String.valueOf(((Number) idObj).longValue());
            else if (idObj != null) idStr = idObj.toString();
        }
        if (idStr == null || idStr.isBlank()) throw new BusinessException(ErrorCode.PARAMS_ERROR, "id不能为空");
        errorBookService.deleteError(Long.parseLong(idStr));
        return ResultUtils.success(true);
    }

    /** 通用：从 JSON body 或 form-urlencoded 中提取 Map */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonOrForm(jakarta.servlet.http.HttpServletRequest request) throws java.io.IOException {
        byte[] raw = request.getInputStream().readAllBytes();
        if (raw.length > 0) {
            String text = new String(raw, java.nio.charset.StandardCharsets.UTF_8).trim();
            if (text.startsWith("{")) {
                try { return new com.fasterxml.jackson.databind.ObjectMapper().readValue(text, Map.class); } catch (Exception ignored) {}
            }
        }
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        java.util.Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            map.put(name, request.getParameter(name));
        }
        return map;
    }

    @PostMapping("/archive/{id}")
    @SaCheckLogin
    @Operation(summary = "归档/取消归档错题")
    public BaseResponse<Boolean> toggleArchive(@PathVariable Long id) {
        errorBookService.toggleArchive(id);
        return ResultUtils.success(true);
    }

    @GetMapping("/stats")
    @SaCheckLogin
    @Operation(summary = "获取错题统计")
    public BaseResponse<Map<String, Object>> getErrorStats() {
        return ResultUtils.success(errorBookService.getErrorStats());
    }

    @GetMapping("/weak-points")
    @SaCheckLogin
    @Operation(summary = "获取薄弱知识点（雷达图数据）")
    public BaseResponse<List<PersonalDimensionVO>> getWeakPoints() {
        return ResultUtils.success(errorBookService.getWeakKnowledgePoints());
    }

    @PostMapping("/share-to-teacher/{teacherId}")
    @SaCheckLogin
    @Operation(summary = "分享错题集给教师（设置分享权限）")
    public BaseResponse<Boolean> shareToTeacher(@PathVariable Long teacherId) {
        // 权限控制：错题集默认仅本人可见，支持分享给教师
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("[错题分享] userId={} 分享错题集给 teacherId={}", userId, teacherId);
        // 实现：在Redis中记录分享关系，12小时有效
        stringRedisTemplate.opsForValue().set(
                "errorbook:share:" + userId + ":" + teacherId, "1",
                java.time.Duration.ofHours(12));
        return ResultUtils.success(true);
    }

    @GetMapping("/is-shared-to/{teacherId}")
    @SaCheckLogin
    @Operation(summary = "检查是否已分享给指定教师")
    public BaseResponse<Boolean> isSharedTo(@PathVariable Long teacherId) {
        Long userId = StpUtil.getLoginIdAsLong();
        String val = stringRedisTemplate.opsForValue()
                .get("errorbook:share:" + userId + ":" + teacherId);
        return ResultUtils.success("1".equals(val));
    }
}
