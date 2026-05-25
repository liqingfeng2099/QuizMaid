package com.kanade.backend.service;

import cn.dev33.satoken.stp.StpUtil;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.model.entity.ErrorBook;
import com.kanade.backend.model.entity.Question;
import com.kanade.backend.service.impl.ErrorBookServiceImpl;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorBookExportService {

    private final ErrorBookServiceImpl errorBookService;
    private final QuestionService questionService;
    private final ErrorBookExportLogService exportLogService;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    // ==================== CSV 导出 ====================

    public void exportExcel(HttpServletResponse response, List<String> fields) throws IOException {
        Long userId = StpUtil.getLoginIdAsLong();
        List<ErrorBook> errors = errorBookService.list(QueryWrapper.create().eq("userId", userId));
        List<Long> qids = errors.stream().map(ErrorBook::getQuestionId).toList();
        List<Question> questions = questionService.listByIds(qids);
        var qMap = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));

        StringBuilder sb = new StringBuilder();
        sb.append("题号,题型,题干,知识点,正确答案,解析,错误类型,错误次数,首次错误,最近错误\n");
        for (ErrorBook eb : errors) {
            Question q = qMap.get(eb.getQuestionId());
            if (q == null) continue;
            sb.append(eb.getQuestionId()).append(",")
              .append(escapeCsv("\"" + getTypeName(q.getType()) + "\""))
              .append(escapeCsv(q.getContent())).append(",")
              .append(escapeCsv(q.getKnowledgePoints())).append(",")
              .append(escapeCsv(q.getAnswer())).append(",")
              .append(escapeCsv(q.getAnalysis())).append(",")
              .append(getErrorTypeName(eb.getErrorType())).append(",")
              .append(eb.getErrorCount()).append(",")
              .append(eb.getFirstErrorTime() != null ? eb.getFirstErrorTime().format(DF) : "").append(",")
              .append(eb.getLastErrorTime() != null ? eb.getLastErrorTime().format(DF) : "").append("\n");
        }

        String fileName = "错题集-" + LocalDateTime.now().format(DF) + ".csv";
        try { exportLogService.saveLog("CSV", 2, fileName, "./exports/" + fileName, null); } catch (Exception ignored) {}
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.getOutputStream().write(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    // ==================== Word 导出（C1修复） ====================

    public void exportWord(HttpServletResponse response) throws IOException {
        Long userId = StpUtil.getLoginIdAsLong();
        List<ErrorBook> errors = errorBookService.list(QueryWrapper.create().eq("userId", userId));
        List<Long> qids = errors.stream().map(ErrorBook::getQuestionId).toList();
        List<Question> questions = questionService.listByIds(qids);
        var qMap = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));

        try (XWPFDocument doc = new XWPFDocument()) {
            // 标题
            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setBold(true); titleRun.setFontSize(18);
            titleRun.setText("错题集");

            // 信息行
            XWPFParagraph info = doc.createParagraph();
            info.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun infoRun = info.createRun();
            infoRun.setFontSize(10); infoRun.setColor("666666");
            infoRun.setText("导出时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) +
                    " | 错题数量：" + errors.size());

            int num = 1;
            for (ErrorBook eb : errors) {
                Question q = qMap.get(eb.getQuestionId());
                if (q == null) continue;

                // 题号+题型
                XWPFParagraph qPara = doc.createParagraph();
                XWPFRun qRun = qPara.createRun();
                qRun.setBold(true); qRun.setFontSize(12);
                qRun.setText(String.format("%d. [%s] %s（错%d次）", num++, getTypeName(q.getType()),
                        getErrorTypeName(eb.getErrorType()), eb.getErrorCount()));

                // 题干
                XWPFParagraph content = doc.createParagraph();
                XWPFRun cRun = content.createRun();
                cRun.setFontSize(11);
                cRun.setText(q.getContent() != null ? q.getContent() : "");

                // 选项（选择题）
                if ((q.getType() == 1 || q.getType() == 2) && q.getOptions() != null && !q.getOptions().isBlank()) {
                    XWPFParagraph optP = doc.createParagraph();
                    XWPFRun optR = optP.createRun();
                    optR.setFontSize(10); optR.setColor("555555");
                    optR.setText("选项：" + q.getOptions());
                }

                // 正确答案
                XWPFParagraph ansP = doc.createParagraph();
                XWPFRun ansR = ansP.createRun();
                ansR.setFontSize(10); ansR.setColor("52c41a");
                ansR.setText("正确答案：" + (q.getAnswer() != null ? q.getAnswer() : ""));

                // 解析
                if (q.getAnalysis() != null && !q.getAnalysis().isBlank()) {
                    XWPFParagraph anaP = doc.createParagraph();
                    XWPFRun anaR = anaP.createRun();
                    anaR.setFontSize(9); anaR.setColor("888888");
                    anaR.setText("解析：" + q.getAnalysis());
                }

                // 知识点
                XWPFParagraph kpP = doc.createParagraph();
                XWPFRun kpR = kpP.createRun();
                kpR.setFontSize(9); kpR.setColor("999999");
                kpR.setText("知识点：" + (q.getKnowledgePoints() != null ? q.getKnowledgePoints() : ""));
            }

            String fileName = "错题集-" + LocalDateTime.now().format(DF) + ".docx";
            try { exportLogService.saveLog("Word", 2, fileName, "./exports/" + fileName, null); } catch (Exception ignored) {}

            response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            response.setHeader("Content-Disposition",
                    "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
            doc.write(response.getOutputStream());
        } catch (Exception e) {
            log.error("错题Word导出失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Word导出失败");
        }
    }

    // ==================== PDF 导出（C1修复） ====================

    public void exportPdf(HttpServletResponse response) throws IOException {
        Long userId = StpUtil.getLoginIdAsLong();
        String html = previewHtml(userId);

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            org.xhtmlrenderer.pdf.ITextRenderer renderer = new org.xhtmlrenderer.pdf.ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(bos);
            byte[] pdfBytes = bos.toByteArray();

            String fileName = "错题集-" + LocalDateTime.now().format(DF) + ".pdf";
            try { exportLogService.saveLog("PDF", 2, fileName, "./exports/" + fileName, null); } catch (Exception ignored) {}

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition",
                    "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
            response.setContentLength(pdfBytes.length);
            response.getOutputStream().write(pdfBytes);
        } catch (Exception e) {
            log.error("错题PDF导出失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "PDF导出失败");
        }
    }

    // ==================== 批量导出（C5修复） ====================

    public void batchExport(HttpServletResponse response, String exportType) throws IOException {
        String ext = "Word".equalsIgnoreCase(exportType) ? "docx" : "pdf";
        String fileName = "错题集批量导出-" + LocalDateTime.now().format(DF) + ".zip";

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            Long userId = StpUtil.getLoginIdAsLong();
            List<ErrorBook> errors = errorBookService.list(QueryWrapper.create().eq("userId", userId));
            if (errors.isEmpty()) {
                zos.putNextEntry(new ZipEntry("empty.txt"));
                zos.write("没有错题记录".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
                return;
            }

            // 按知识点分组批量导出
            Map<String, List<ErrorBook>> byKp = new LinkedHashMap<>();
            List<Long> allQids = errors.stream().map(ErrorBook::getQuestionId).toList();
            List<Question> allQuestions = questionService.listByIds(allQids);
            var qMap = allQuestions.stream().collect(Collectors.toMap(Question::getId, q -> q));

            for (ErrorBook eb : errors) {
                Question q = qMap.get(eb.getQuestionId());
                String kpKey = q != null && q.getKnowledgePoints() != null
                        ? q.getKnowledgePoints().split(",")[0].trim() : "未分类";
                byKp.computeIfAbsent(kpKey, k -> new ArrayList<>()).add(eb);
            }

            for (Map.Entry<String, List<ErrorBook>> entry : byKp.entrySet()) {
                String safeName = entry.getKey().replaceAll("[\\\\/:*?\"<>|]", "_");
                ZipEntry ze = new ZipEntry(safeName + "." + ext);
                zos.putNextEntry(ze);
                // 写入简单CSV作为分组内容
                StringBuilder sb = new StringBuilder();
                sb.append("知识点分组: ").append(entry.getKey()).append("\n\n");
                for (ErrorBook eb : entry.getValue()) {
                    Question q = qMap.get(eb.getQuestionId());
                    if (q == null) continue;
                    sb.append("[").append(getTypeName(q.getType())).append("] ")
                      .append(q.getContent()).append("\n");
                    sb.append("答案: ").append(q.getAnswer()).append("\n");
                    sb.append("错误次数: ").append(eb.getErrorCount())
                      .append(" | ").append(getErrorTypeName(eb.getErrorType())).append("\n\n");
                }
                zos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        try { exportLogService.saveLog(exportType, 2, fileName, "./exports/" + fileName, null); } catch (Exception ignored) {}
    }

    // ==================== HTML 预览 ====================

    public String previewHtml(Long userId) {
        List<ErrorBook> errors = errorBookService.list(QueryWrapper.create().eq("userId", userId));
        List<Long> qids = errors.stream().map(ErrorBook::getQuestionId).toList();
        List<Question> questions = questionService.listByIds(qids);
        var qMap = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));

        StringBuilder html = new StringBuilder("<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta charset='utf-8'/><style>");
        html.append("body{font-family:'Microsoft YaHei',sans-serif;max-width:800px;margin:0 auto;padding:20px;}");
        html.append(".q-item{border:1px solid #ddd;padding:12px;margin-bottom:8px;border-radius:4px;}");
        html.append(".q-title{font-weight:bold;margin-bottom:4px;}");
        html.append(".q-content{margin:8px 0;line-height:1.8;}");
        html.append(".q-answer{color:#52c41a;margin-top:4px;}");
        html.append(".q-meta{color:#999;font-size:12px;margin-top:4px;}");
        html.append("</style></head><body><h2>错题集</h2>");

        for (ErrorBook eb : errors) {
            Question q = qMap.get(eb.getQuestionId());
            if (q == null) continue;
            html.append("<div class='q-item'>");
            html.append("<div class='q-title'>").append(getTypeName(q.getType()))
                .append(" | 错误").append(eb.getErrorCount()).append("次</div>");
            html.append("<div class='q-content'>").append(q.getContent() != null ? q.getContent() : "").append("</div>");
            html.append("<div class='q-answer'>正确答案: ").append(q.getAnswer() != null ? q.getAnswer() : "").append("</div>");
            html.append("<div class='q-meta'>知识点: ").append(q.getKnowledgePoints()).append(" | ")
                .append(getErrorTypeName(eb.getErrorType())).append(" | ")
                .append(eb.getLastErrorTime() != null ? eb.getLastErrorTime().toString() : "").append("</div>");
            html.append("</div>");
        }
        html.append("</body></html>");
        return html.toString();
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private String getTypeName(Integer t) {
        if (t == null) return "未知";
        if (t == 1) return "单选题";
        if (t == 2) return "多选题";
        if (t == 3) return "填空题";
        if (t == 4) return "简答题";
        return "未知";
    }

    private String getErrorTypeName(Integer t) {
        if (t == null) return "未知";
        if (t == 1) return "概念错误";
        if (t == 2) return "计算错误";
        if (t == 3) return "思路错误";
        if (t == 4) return "审题错误";
        return "未知";
    }
}
