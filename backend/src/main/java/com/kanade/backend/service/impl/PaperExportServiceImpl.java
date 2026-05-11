package com.kanade.backend.service.impl;

import cn.hutool.json.JSONUtil;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.mapper.ExamPaperMapper;
import com.kanade.backend.mapper.PaperquestionMapper;
import com.kanade.backend.mapper.QuestionMapper;
import com.kanade.backend.model.dto.ExportConfigDTO;
import com.kanade.backend.model.entity.ExamPaper;
import com.kanade.backend.model.entity.Paperquestion;
import com.kanade.backend.model.entity.Question;
import com.kanade.backend.model.vo.ExportFileVO;
import com.kanade.backend.service.ExamPaperService;
import com.kanade.backend.service.PaperExportService;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaperExportServiceImpl implements PaperExportService {

    private static final String EXPORT_DIR = "./exports/";
    private final ExamPaperMapper examPaperMapper;
    private final QuestionMapper questionMapper;
    private final PaperquestionMapper paperquestionMapper;
    private final ExamPaperService examPaperService;

    // ==================== Word 导出 ====================

    @Override
    public byte[] exportWord(Long paperId, ExportConfigDTO config) {
        ExamPaper paper = examPaperMapper.selectOneById(paperId);
        if (paper == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "试卷不存在");

        List<Paperquestion> pqs = paperquestionMapper.selectListByQuery(
                QueryWrapper.create().eq("paperId", paperId).orderBy("sort", true));
        List<Long> qids = pqs.stream().map(Paperquestion::getQuestionId).collect(Collectors.toList());
        List<Question> questions = qids.isEmpty() ? Collections.emptyList() : questionMapper.selectListByIds(qids);

        try (XWPFDocument doc = new XWPFDocument()) {
            // 标题
            XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setBold(true); titleRun.setFontSize(18);
            titleRun.setText(paper.getPaperName());

            // 信息行
            XWPFParagraph infoPara = doc.createParagraph();
            infoPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun infoRun = infoPara.createRun();
            infoRun.setFontSize(10); infoRun.setColor("666666");
            infoRun.setText(String.format("学科：%s | 总分：%d | 时间：%d分钟",
                    paper.getSubject(), paper.getTotalScore(),
                    paper.getDuration() != null ? paper.getDuration() : 90));

            // 按题型分组
            Map<Integer, List<Paperquestion>> byType = pqs.stream()
                    .collect(Collectors.groupingBy(pq -> {
                        Question q = questions.stream().filter(x -> x.getId().equals(pq.getQuestionId())).findFirst().orElse(null);
                        return q != null ? q.getType() : 1;
                    }, LinkedHashMap::new, Collectors.toList()));

            int qNum = 1;
            Map<Long, String> answerMap = new LinkedHashMap<>();

            for (Map.Entry<Integer, List<Paperquestion>> entry : byType.entrySet()) {
                int type = entry.getKey();
                String typeName = getTypeName(type);
                XWPFParagraph typePara = doc.createParagraph();
                XWPFRun typeRun = typePara.createRun();
                typeRun.setBold(true); typeRun.setFontSize(14);
                typeRun.setText(String.format("一、%s", typeName));

                for (Paperquestion pq : entry.getValue()) {
                    Question q = questions.stream().filter(x -> x.getId().equals(pq.getQuestionId())).findFirst().orElse(null);
                    if (q == null) continue;

                    XWPFParagraph qPara = doc.createParagraph();
                    XWPFRun qRun = qPara.createRun();
                    qRun.setFontSize(11);
                    String scoreStr = config.getScorePosition() != null && "inline".equals(config.getScorePosition())
                            ? String.format("（%d分）", pq.getQuestionScore()) : "";
                    qRun.setText(String.format("%d. %s%s", qNum++, q.getContent(), scoreStr));

                    // 选项
                    if ((type == 1 || type == 2) && q.getOptions() != null && !q.getOptions().isBlank()) {
                        try {
                            List<Map<String, String>> opts = parseOptions(q.getOptions());
                            XWPFParagraph optPara = doc.createParagraph();
                            XWPFRun optRun = optPara.createRun();
                            optRun.setFontSize(10);
                            StringBuilder optSb = new StringBuilder();
                            for (Map<String, String> o : opts) {
                                optSb.append(o.get("A") != null ? "A. " + o.get("A") : "")
                                      .append(o.get("B") != null ? "  B. " + o.get("B") : "")
                                      .append(o.get("C") != null ? "  C. " + o.get("C") : "")
                                      .append(o.get("D") != null ? "  D. " + o.get("D") : "").append("    ");
                            }
                            optRun.setText(optSb.toString());
                        } catch (Exception ignored) {}
                    }

                    // 答案
                    if (Boolean.TRUE.equals(config.getShowAnswer())) {
                        answerMap.put(q.getId(), q.getAnswer());
                    }
                }
            }

            // 答案区
            if (Boolean.TRUE.equals(config.getShowAnswer()) && !answerMap.isEmpty()) {
                XWPFParagraph sep = doc.createParagraph();
                XWPFRun sepRun = sep.createRun(); sepRun.setText(""); sepRun.addBreak();
                XWPFParagraph ansTitle = doc.createParagraph();
                XWPFRun ansTitleRun = ansTitle.createRun();
                ansTitleRun.setBold(true); ansTitleRun.setFontSize(14);
                ansTitleRun.setText("参考答案");
                int ai = 1;
                for (Map.Entry<Long, String> e : answerMap.entrySet()) {
                    XWPFParagraph aPara = doc.createParagraph();
                    XWPFRun aRun = aPara.createRun();
                    aRun.setFontSize(10);
                    aRun.setText(String.format("%d. %s", ai++, e.getValue()));
                }
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            doc.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("Word导出失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Word导出失败");
        }
    }

    // ==================== PDF 导出 ====================

    @Override
    public byte[] exportPdf(Long paperId, ExportConfigDTO config) {
        String html = previewHtml(paperId, config);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            org.xhtmlrenderer.pdf.ITextRenderer renderer = new org.xhtmlrenderer.pdf.ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("PDF导出失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "PDF导出失败");
        }
    }

    // ==================== HTML 预览 ====================

    @Override
    public String previewHtml(Long paperId, ExportConfigDTO config) {
        ExamPaper paper = examPaperMapper.selectOneById(paperId);
        if (paper == null) return "<p>试卷不存在</p>";

        List<Paperquestion> pqs = paperquestionMapper.selectListByQuery(
                QueryWrapper.create().eq("paperId", paperId).orderBy("sort", true));
        List<Long> qids = pqs.stream().map(Paperquestion::getQuestionId).collect(Collectors.toList());
        List<Question> questions = qids.isEmpty() ? Collections.emptyList() : questionMapper.selectListByIds(qids);

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta charset=\"UTF-8\"/><style>");
        sb.append("body{font-family:SimSun;padding:20px;}");
        sb.append(".title{text-align:center;font-size:18pt;font-weight:bold;margin-bottom:4px;}");
        sb.append(".info{text-align:center;color:#666;font-size:9pt;margin-bottom:16px;}");
        sb.append(".q{margin:6px 0;font-size:11pt;}");
        sb.append(".opts{margin-left:24px;font-size:10pt;color:#333;}");
        sb.append(".answer{color:#1890ff;font-size:9pt;margin-left:8px;}");
        sb.append(".score{color:#999;font-size:9pt;}");
        sb.append("</style></head><body>");

        sb.append("<div class='title'>").append(escape(paper.getPaperName())).append("</div>");
        sb.append("<div class='info'>学科：").append(escape(paper.getSubject()))
          .append(" | 总分：").append(paper.getTotalScore())
          .append(" | 时长：").append(paper.getDuration() != null ? paper.getDuration() : 90).append("分钟</div>");

        Map<Integer, List<Paperquestion>> byType = pqs.stream()
                .collect(Collectors.groupingBy(pq -> {
                    Question q = questions.stream().filter(x -> x.getId().equals(pq.getQuestionId())).findFirst().orElse(null);
                    return q != null ? q.getType() : 1;
                }, LinkedHashMap::new, Collectors.toList()));

        int qNum = 1;
        for (Map.Entry<Integer, List<Paperquestion>> entry : byType.entrySet()) {
            int type = entry.getKey();
            sb.append("<h3>一、").append(getTypeName(type)).append("</h3>");
            for (Paperquestion pq : entry.getValue()) {
                Question q = questions.stream().filter(x -> x.getId().equals(pq.getQuestionId())).findFirst().orElse(null);
                if (q == null) continue;
                sb.append("<div class='q'>").append(qNum++).append(". ").append(escape(q.getContent()));
                sb.append("<span class='score'>（").append(pq.getQuestionScore()).append("分）</span></div>");

                if ((type == 1 || type == 2) && q.getOptions() != null && !q.getOptions().isBlank()) {
                    try {
                        List<Map<String, String>> opts = parseOptions(q.getOptions());
                        sb.append("<div class='opts'>");
                        for (Map<String, String> o : opts) {
                            for (Map.Entry<String, String> oe : o.entrySet()) {
                                sb.append(oe.getKey()).append(". ").append(escape(oe.getValue())).append("&nbsp;&nbsp;");
                            }
                        }
                        sb.append("</div>");
                    } catch (Exception ignored) {}
                }
                if (Boolean.TRUE.equals(config.getShowAnswer())) {
                    sb.append("<span class='answer'>答案: ").append(escape(q.getAnswer())).append("</span>");
                }
            }
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    // ==================== 文件管理 ====================

    @Override
    public ExportFileVO saveAndRecord(Long paperId, String exportType, String fileName) {
        try {
            Files.createDirectories(Path.of(EXPORT_DIR));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "无法创建导出目录");
        }
        return ExportFileVO.builder()
                .paperId(paperId)
                .fileName(fileName)
                .filePath(EXPORT_DIR + fileName)
                .exportType(exportType)
                .exportStatus(1)
                .createTime(LocalDateTime.now())
                .build();
    }

    @Override
    public List<ExportFileVO> listExportFiles(Long userId) {
        List<ExportFileVO> files = new ArrayList<>();
        try {
            Path dir = Path.of(EXPORT_DIR);
            if (!Files.exists(dir)) return files;
            Files.list(dir).filter(Files::isRegularFile).forEach(f -> {
                String name = f.getFileName().toString();
                if (name.endsWith(".docx") || name.endsWith(".pdf")) {
                    files.add(ExportFileVO.builder()
                            .fileName(name)
                            .filePath(f.toString())
                            .exportType(name.endsWith(".pdf") ? "PDF" : "Word")
                            .createTime(LocalDateTime.now())
                            .build());
                }
            });
        } catch (IOException e) {
            log.error("列出文件失败", e);
        }
        return files;
    }

    @Override
    public boolean deleteExportFile(Long fileId) {
        List<ExportFileVO> files = listExportFiles(null);
        if (fileId < 0 || fileId >= files.size()) return false;
        try {
            Files.deleteIfExists(Path.of(files.get(fileId.intValue()).getFilePath()));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public byte[] batchExport(List<Long> paperIds, ExportConfigDTO config) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(bos)) {
                for (Long pid : paperIds) {
                    config.setPaperId(pid);
                    byte[] word = exportWord(pid, config);
                    ExamPaper paper = examPaperMapper.selectOneById(pid);
                    String name = (paper != null ? paper.getPaperName() : "paper-" + pid) + ".docx";
                    ZipEntry entry = new ZipEntry(name);
                    zos.putNextEntry(entry);
                    zos.write(word);
                    zos.closeEntry();
                }
            }
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("批量导出失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "批量导出失败");
        }
    }

    // ==================== 辅助 ====================

    private String getTypeName(int type) {
        return switch (type) {
            case 1 -> "单选题";
            case 2 -> "多选题";
            case 3 -> "填空题";
            case 4 -> "简答题";
            default -> "未知题型";
        };
    }

    private List<Map<String, String>> parseOptions(String options) {
        if (options == null || options.isBlank()) return Collections.emptyList();
        List<Map<String, String>> list;
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> tmp = (List) JSONUtil.toList(options, Map.class);
            list = tmp;
        } catch (Exception e) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> tmp = (List) JSONUtil.toList("[" + options + "]", Map.class);
            list = tmp;
        }
        return list;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
