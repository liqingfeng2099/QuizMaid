package com.kanade.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.mapper.ExamPaperMapper;
import com.kanade.backend.mapper.PaperquestionMapper;
import com.kanade.backend.mapper.QuestionMapper;
import com.kanade.backend.model.dto.ExportConfigDTO;
import com.kanade.backend.model.entity.ExamPaper;
import com.kanade.backend.model.entity.ExportTemplate;
import com.kanade.backend.model.entity.Paperquestion;
import com.kanade.backend.model.entity.Question;
import com.kanade.backend.model.vo.ExportFileVO;
import com.kanade.backend.service.ExamPaperService;
import com.kanade.backend.service.ExportTemplateService;
import com.kanade.backend.service.PaperExportService;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
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
    private final ExportTemplateService exportTemplateService;

    // ==================== 模板配置合并 ====================

    private ExportConfigDTO mergeWithTemplate(ExportConfigDTO config) {
        if (config.getTemplateId() == null) {
            // 尝试加载默认模板
            String exportType = config.getExportType() != null ? config.getExportType() : "Word";
            Long userId = StpUtil.getLoginIdAsLong();
            ExportTemplate defaultTemplate = exportTemplateService.getDefaultTemplate(userId, exportType);
            if (defaultTemplate != null) {
                config.setTemplateId(defaultTemplate.getId());
            }
        }

        if (config.getTemplateId() != null) {
            ExportTemplate template = exportTemplateService.getById(config.getTemplateId());
            if (template != null && template.getConfig() != null) {
                try {
                    JSONObject tpl = JSONUtil.parseObj(template.getConfig());
                    // 模板配置作为默认值，请求参数可覆盖
                    if (config.getTitleAlign() == null && tpl.containsKey("titleAlign"))
                        config.setTitleAlign(tpl.getStr("titleAlign"));
                    if (config.getShowAnswer() == null && tpl.containsKey("showAnswer"))
                        config.setShowAnswer(tpl.getBool("showAnswer"));
                    if (config.getShowAnalysis() == null && tpl.containsKey("showAnalysis"))
                        config.setShowAnalysis(tpl.getBool("showAnalysis"));
                    if (config.getScorePosition() == null && tpl.containsKey("scorePosition"))
                        config.setScorePosition(tpl.getStr("scorePosition"));
                    if (config.getEditable() == null && tpl.containsKey("editable"))
                        config.setEditable(tpl.getBool("editable"));
                } catch (Exception e) {
                    log.warn("模板配置解析失败，使用请求参数: templateId={}", config.getTemplateId(), e);
                }
            }
        }
        return config;
    }

    // ==================== Word 导出 ====================

    @Override
    public byte[] exportWord(Long paperId, ExportConfigDTO config) {
        config = mergeWithTemplate(config);
        ExamPaper paper = examPaperMapper.selectOneById(paperId);
        if (paper == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "试卷不存在");

        List<Paperquestion> pqs = paperquestionMapper.selectListByQuery(
                QueryWrapper.create().eq("paperId", paperId).orderBy("sort", true));
        List<Long> qids = pqs.stream().map(Paperquestion::getQuestionId).collect(Collectors.toList());
        List<Question> questions = qids.isEmpty() ? Collections.emptyList() : questionMapper.selectListByIds(qids);

        try (XWPFDocument doc = new XWPFDocument()) {
            // 标题
            XWPFParagraph titlePara = doc.createParagraph();
            String align = config.getTitleAlign() != null ? config.getTitleAlign() : "center";
            if ("left".equals(align)) titlePara.setAlignment(ParagraphAlignment.LEFT);
            else if ("right".equals(align)) titlePara.setAlignment(ParagraphAlignment.RIGHT);
            else titlePara.setAlignment(ParagraphAlignment.CENTER);

            XWPFRun titleRun = titlePara.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setText(paper.getPaperName());

            // 信息行
            XWPFParagraph infoPara = doc.createParagraph();
            infoPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun infoRun = infoPara.createRun();
            infoRun.setFontSize(10);
            infoRun.setColor("666666");
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
                XWPFParagraph typePara = doc.createParagraph();
                XWPFRun typeRun = typePara.createRun();
                typeRun.setBold(true);
                typeRun.setFontSize(14);
                typeRun.setText(String.format("一、%s", getTypeName(type)));

                for (Paperquestion pq : entry.getValue()) {
                    Question q = questions.stream().filter(x -> x.getId().equals(pq.getQuestionId())).findFirst().orElse(null);
                    if (q == null) continue;

                    XWPFParagraph qPara = doc.createParagraph();
                    XWPFRun qRun = qPara.createRun();
                    qRun.setFontSize(11);
                    String scoreStr = "inline".equals(config.getScorePosition())
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
                                for (Map.Entry<String, String> oe : o.entrySet()) {
                                    optSb.append(oe.getKey()).append(". ").append(oe.getValue()).append("    ");
                                }
                            }
                            optRun.setText(optSb.toString());
                        } catch (Exception ignored) {
                        }
                    }

                    // 分值显示在右侧
                    if ("right".equals(config.getScorePosition())) {
                        answerMap.put(q.getId(), q.getAnswer());
                    }
                    if (Boolean.TRUE.equals(config.getShowAnswer())) {
                        answerMap.put(q.getId(), q.getAnswer());
                    }
                }
            }

            // 答案区
            if (!answerMap.isEmpty()) {
                XWPFParagraph sep = doc.createParagraph();
                sep.createRun().addBreak();
                XWPFParagraph ansTitle = doc.createParagraph();
                XWPFRun ansTitleRun = ansTitle.createRun();
                ansTitleRun.setBold(true);
                ansTitleRun.setFontSize(14);
                ansTitleRun.setText("参考答案");
                int ai = 1;
                for (Map.Entry<Long, String> e : answerMap.entrySet()) {
                    XWPFParagraph aPara = doc.createParagraph();
                    XWPFRun aRun = aPara.createRun();
                    aRun.setFontSize(10);
                    aRun.setText(String.format("%d. %s", ai++, e.getValue()));
                    if (Boolean.TRUE.equals(config.getShowAnalysis())) {
                        Question q = questions.stream().filter(x -> x.getId().equals(e.getKey())).findFirst().orElse(null);
                        if (q != null && q.getAnalysis() != null && !q.getAnalysis().isBlank()) {
                            XWPFRun anaRun = aPara.createRun();
                            anaRun.setFontSize(9);
                            anaRun.setColor("888888");
                            anaRun.setText("  解析：" + q.getAnalysis());
                        }
                    }
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
        config = mergeWithTemplate(config);
        String html = previewHtml(paperId, config);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            org.xhtmlrenderer.pdf.ITextRenderer renderer = new org.xhtmlrenderer.pdf.ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(bos);
            byte[] pdfBytes = bos.toByteArray();

            // 可编辑模式：添加AcroForm填空域
            if (Boolean.TRUE.equals(config.getEditable())) {
                pdfBytes = addAcroFormFields(pdfBytes, paperId);
            }

            return pdfBytes;
        } catch (Exception e) {
            log.error("PDF导出失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "PDF导出失败");
        }
    }

    private byte[] addAcroFormFields(byte[] pdfBytes, Long paperId) {
        try {
            com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(pdfBytes);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            com.lowagie.text.pdf.PdfStamper stamper = new com.lowagie.text.pdf.PdfStamper(reader, bos);
            com.lowagie.text.pdf.AcroFields form = stamper.getAcroFields();

            List<Paperquestion> pqs = paperquestionMapper.selectListByQuery(
                    QueryWrapper.create().eq("paperId", paperId).orderBy("sort", true));

            float pageHeight = reader.getPageSize(1).getHeight();
            float y = pageHeight - 120; // 从标题下方开始
            int pageNum = 1;
            int fieldIdx = 0;

            for (Paperquestion pq : pqs) {
                if (y < 80) {
                    y = pageHeight - 60;
                    pageNum++;
                }
                if (pageNum > reader.getNumberOfPages()) break;

                // 为每题添加一个多行文本域（答案填写区）
                com.lowagie.text.Rectangle rect = new com.lowagie.text.Rectangle(50, y - 8, 500, y - 22);
                com.lowagie.text.pdf.TextField tf = new com.lowagie.text.pdf.TextField(
                        stamper.getWriter(), rect, "answer_" + fieldIdx);
                tf.setOptions(com.lowagie.text.pdf.TextField.MULTILINE);
                tf.setFontSize(10);
                stamper.addAnnotation(tf.getTextField(), pageNum);

                y -= 55;
                fieldIdx++;
            }

            stamper.setFormFlattening(false);
            stamper.close();
            reader.close();

            log.info("[PDF可编辑] 已添加{}个表单域: paperId={}", fieldIdx, paperId);
            return bos.toByteArray();
        } catch (Exception e) {
            log.warn("[PDF可编辑] 添加表单域失败，返回只读版本: paperId={}", paperId, e);
            return pdfBytes;
        }
    }

    // ==================== HTML 预览 ====================

    @Override
    public String previewHtml(Long paperId, ExportConfigDTO config) {
        config = mergeWithTemplate(config);
        ExamPaper paper = examPaperMapper.selectOneById(paperId);
        if (paper == null) return "<p>试卷不存在</p>";

        List<Paperquestion> pqs = paperquestionMapper.selectListByQuery(
                QueryWrapper.create().eq("paperId", paperId).orderBy("sort", true));
        List<Long> qids = pqs.stream().map(Paperquestion::getQuestionId).collect(Collectors.toList());
        List<Question> questions = qids.isEmpty() ? Collections.emptyList() : questionMapper.selectListByIds(qids);

        String titleAlign = config.getTitleAlign() != null ? config.getTitleAlign() : "center";
        boolean editable = Boolean.TRUE.equals(config.getEditable());

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta charset=\"UTF-8\"/><style>");
        sb.append("body{font-family:SimSun;padding:20px;}");
        sb.append(".title{text-align:").append(titleAlign).append(";font-size:18pt;font-weight:bold;margin-bottom:4px;}");
        sb.append(".info{text-align:center;color:#666;font-size:9pt;margin-bottom:16px;}");
        sb.append(".q{margin:8px 0;font-size:11pt;}");
        sb.append(".opts{margin-left:24px;font-size:10pt;color:#333;}");
        sb.append(".answer{color:#1890ff;font-size:9pt;margin-left:8px;}");
        sb.append(".score{color:#999;font-size:9pt;}");
        if (editable) {
            sb.append(".answer-area{border:1px dashed #ccc;height:40px;margin:4px 0 8px 0;background:#fafafa;}");
        }
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
                    } catch (Exception ignored) {
                    }
                }

                // 可编辑模式：显示答案填写区
                if (editable) {
                    sb.append("<div class='answer-area'></div>");
                }

                if (Boolean.TRUE.equals(config.getShowAnswer())) {
                    sb.append("<span class='answer'>答案: ").append(escape(q.getAnswer())).append("</span>");
                    if (Boolean.TRUE.equals(config.getShowAnalysis()) && q.getAnalysis() != null && !q.getAnalysis().isBlank()) {
                        sb.append("<span class='answer'> 解析: ").append(escape(q.getAnalysis())).append("</span>");
                    }
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
        config = mergeWithTemplate(config);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(bos)) {
                for (Long pid : paperIds) {
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
        if (type == 1) return "单选题";
        if (type == 2) return "多选题";
        if (type == 3) return "填空题";
        if (type == 4) return "简答题";
        return "未知题型";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> parseOptions(String options) {
        if (options == null || options.isBlank()) return Collections.emptyList();
        List<Map<String, String>> list;
        try {
            list = (List) JSONUtil.toList(options, Map.class);
        } catch (Exception e) {
            list = (List) JSONUtil.toList("[" + options + "]", Map.class);
        }
        return list;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
