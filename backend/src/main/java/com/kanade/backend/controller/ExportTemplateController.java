package com.kanade.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.model.entity.ExportTemplate;
import com.kanade.backend.service.ExportTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/export-template")
@RequiredArgsConstructor
@SaCheckLogin
@Tag(name = "导出模板管理")
public class ExportTemplateController {

    private final ExportTemplateService templateService;

    @PostMapping(value = "/create", consumes = {"application/json", "application/x-www-form-urlencoded"})
    @Operation(summary = "创建导出模板")
    public BaseResponse<ExportTemplate> create(@RequestBody(required = false) Map<String, String> body,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "exportType", required = false) String exportType,
            @RequestParam(value = "config", required = false) String config) {
        return ResultUtils.success(templateService.createTemplate(
                body != null && !body.isEmpty() ? body.get("name") : name,
                body != null && !body.isEmpty() ? body.get("exportType") : exportType,
                body != null && !body.isEmpty() ? body.get("config") : config));
    }

    @PostMapping(value = "/update/{id}", consumes = {"application/json", "application/x-www-form-urlencoded"})
    @Operation(summary = "更新导出模板")
    public BaseResponse<ExportTemplate> update(@PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "config", required = false) String config) {
        return ResultUtils.success(templateService.updateTemplate(id,
                body != null && !body.isEmpty() ? body.get("name") : name,
                body != null && !body.isEmpty() ? body.get("config") : config));
    }

    @PostMapping("/delete/{id}")
    @Operation(summary = "删除导出模板")
    public BaseResponse<Boolean> delete(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResultUtils.success(true);
    }

    @PostMapping("/set-default/{id}")
    @Operation(summary = "设为默认模板")
    public BaseResponse<Boolean> setDefault(@PathVariable Long id) {
        templateService.setDefault(id);
        return ResultUtils.success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "获取用户导出模板列表")
    public BaseResponse<List<ExportTemplate>> list(@RequestParam(required = false) String exportType) {
        return ResultUtils.success(templateService.getUserTemplates(exportType));
    }
}
