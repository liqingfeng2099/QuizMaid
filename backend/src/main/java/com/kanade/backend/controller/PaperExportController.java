package com.kanade.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.mapper.ExamPaperMapper;
import com.kanade.backend.model.dto.ExportConfigDTO;
import com.kanade.backend.model.entity.ExamPaper;
import com.kanade.backend.model.entity.User;
import com.kanade.backend.model.vo.ExportFileVO;
import com.kanade.backend.service.PaperExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/examPaper/export")
@RequiredArgsConstructor
@Tag(name = "试卷导出")
public class PaperExportController {

    private final PaperExportService exportService;
    private final ExamPaperMapper examPaperMapper;

    @PostMapping("/word")
    @SaCheckLogin
    @Operation(summary = "导出Word")
    public void exportWord(@RequestBody ExportConfigDTO config, HttpServletResponse response) throws IOException {
        byte[] data = exportService.exportWord(config.getPaperId(), config);
        ExamPaper paper = examPaperMapper.selectOneById(config.getPaperId());
        String fileName = buildFileName(paper, "docx");
        sendFile(response, data, fileName, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        saveFile(data, fileName);
    }

    @PostMapping("/pdf")
    @SaCheckLogin
    @Operation(summary = "导出PDF")
    public void exportPdf(@RequestBody ExportConfigDTO config, HttpServletResponse response) throws IOException {
        byte[] data = exportService.exportPdf(config.getPaperId(), config);
        ExamPaper paper = examPaperMapper.selectOneById(config.getPaperId());
        String fileName = buildFileName(paper, "pdf");
        sendFile(response, data, fileName, "application/pdf");
        saveFile(data, fileName);
    }

    @PostMapping("/preview")
    @SaCheckLogin
    @Operation(summary = "预览试卷HTML")
    public BaseResponse<String> preview(@RequestBody ExportConfigDTO config) {
        String html = exportService.previewHtml(config.getPaperId(), config);
        return ResultUtils.success(html);
    }

    @PostMapping("/batch")
    @SaCheckLogin
    @Operation(summary = "批量导出ZIP")
    public void batchExport(@RequestBody ExportConfigDTO config, HttpServletResponse response) throws IOException {
        List<Long> ids = config.getPaperIds();
        if (ids == null || ids.isEmpty()) throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择试卷");
        byte[] data = exportService.batchExport(ids, config);
        String fileName = "试卷批量导出-" + java.time.LocalDate.now() + ".zip";
        sendFile(response, data, fileName, "application/zip");
    }

    @GetMapping("/files")
    @SaCheckLogin
    @Operation(summary = "导出文件列表")
    public BaseResponse<List<ExportFileVO>> listFiles() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(exportService.listExportFiles(userId));
    }

    @PostMapping("/delete/{index}")
    @SaCheckLogin
    @Operation(summary = "删除导出文件(按索引)")
    public BaseResponse<Boolean> deleteFile(@PathVariable Long index) {
        return ResultUtils.success(exportService.deleteExportFile(index));
    }

    @GetMapping("/download/{fileName}")
    @SaCheckLogin
    @Operation(summary = "下载导出文件")
    public void downloadFile(@PathVariable String fileName, HttpServletResponse response) throws IOException {
        Path path = Path.of("./exports/" + fileName);
        if (!Files.exists(path)) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件不存在");
        byte[] data = Files.readAllBytes(path);
        String contentType = fileName.endsWith(".pdf") ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        sendFile(response, data, fileName, contentType);
    }

    private String buildFileName(ExamPaper paper, String ext) {
        String subject = paper.getSubject() != null ? paper.getSubject() : "综合";
        String type = paper.getPaperType() != null && paper.getPaperType() == 3 ? "AI组卷"
                : paper.getPaperType() != null && paper.getPaperType() == 2 ? "自动组卷" : "手动组卷";
        String date = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return subject + "-" + type + "-" + date + "." + ext;
    }

    private void sendFile(HttpServletResponse response, byte[] data, String fileName, String contentType) throws IOException {
        response.setContentType(contentType);
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.setContentLength(data.length);
        try (OutputStream os = response.getOutputStream()) {
            os.write(data);
            os.flush();
        }
    }

    private void saveFile(byte[] data, String fileName) {
        try {
            Files.createDirectories(Path.of("./exports"));
            Files.write(Path.of("./exports/" + fileName), data);
        } catch (IOException ignored) {}
    }
}
