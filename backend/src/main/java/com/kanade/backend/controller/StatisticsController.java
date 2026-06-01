package com.kanade.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.excel.EasyExcel;
import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.mapper.StatisticsMapper;
import com.kanade.backend.model.dto.StatisticsQueryDTO;
import com.kanade.backend.model.dto.TrendQueryDTO;
import com.kanade.backend.model.excel.HighFreqWrongExcelData;
import com.kanade.backend.model.excel.StatisticsExcelData;
import com.kanade.backend.model.vo.HighFreqWrongQuestionVO;
import com.kanade.backend.model.vo.PaperStatisticsVO;
import com.kanade.backend.model.vo.QuestionTypeStatVO;
import com.kanade.backend.model.vo.TrendDataPointVO;
import com.kanade.backend.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "成绩统计与可视化")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final StatisticsMapper statisticsMapper;

    @PostMapping("/paper")
    @SaCheckLogin
    @Operation(summary = "获取试卷整体统计")
    public BaseResponse<PaperStatisticsVO> getPaperStatistics(@RequestBody StatisticsQueryDTO queryDTO) {
        if (queryDTO.getPaperId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "试卷ID不能为空");
        }
        PaperStatisticsVO vo = statisticsService.getPaperStatistics(queryDTO);
        return ResultUtils.success(vo);
    }

    @PostMapping("/trend")
    @SaCheckLogin
    @Operation(summary = "获取用户成绩趋势")
    public BaseResponse<List<TrendDataPointVO>> getTrend(@RequestBody TrendQueryDTO queryDTO) {
        List<TrendDataPointVO> list = statisticsService.getScoreTrend(queryDTO);
        return ResultUtils.success(list);
    }

    @GetMapping("/comparison/{paperId}")
    @SaCheckLogin
    @Operation(summary = "获取当前用户在该试卷的个人对比")
    public BaseResponse<PaperStatisticsVO> getComparison(@PathVariable Long paperId) {
        PaperStatisticsVO vo = statisticsService.getComparison(paperId);
        return ResultUtils.success(vo);
    }

    // 获取全部题型正确率统计接口 GET /statistics/type-accuracy .hml
    @GetMapping("/type-accuracy")
    @SaCheckLogin
    @Operation(summary = "获取全部题型正确率统计")
    public BaseResponse<List<QuestionTypeStatVO>> getTypeAccuracy() {
        List<QuestionTypeStatVO> list = statisticsService.getTypeAccuracy();
        return ResultUtils.success(list);
    }

    // 获取指定试卷的题型正确率统计接口 GET /statistics/paper/{paperId}/type-accuracy .hml
    @GetMapping("/paper/{paperId}/type-accuracy")
    @SaCheckLogin
    @Operation(summary = "获取指定试卷的题型正确率统计")
    public BaseResponse<List<QuestionTypeStatVO>> getPaperTypeAccuracy(@PathVariable Long paperId) {
        List<QuestionTypeStatVO> list = statisticsService.getPaperTypeAccuracy(paperId);
        return ResultUtils.success(list);
    }

    @PostMapping("/papers-available")
    @SaCheckLogin
    @Operation(summary = "获取可统计的试卷列表")
    public BaseResponse<List<PaperStatisticsVO>> getAvailablePapers() {
        List<PaperStatisticsVO> papers = statisticsService.getAvailablePapers();
        return ResultUtils.success(papers);
    }

    @PostMapping("/export/excel")
    @SaCheckLogin
    @Operation(summary = "导出统计数据为Excel")
    public void exportExcel(@RequestBody StatisticsQueryDTO queryDTO,
                            HttpServletResponse response) throws IOException {
        PaperStatisticsVO stats = statisticsService.getPaperStatistics(queryDTO);

        StatisticsExcelData data = new StatisticsExcelData();
        data.setPaperName(stats.getPaperName());
        data.setSubject(stats.getSubject());
        data.setTotalScore(stats.getTotalScore());
        data.setMaxScore(stats.getMaxScore());
        data.setMinScore(stats.getMinScore());
        data.setAvgScore(stats.getAvgScore());
        data.setMedianScore(stats.getMedianScore());
        data.setTotalExaminees(stats.getTotalExaminees());
        data.setHighScoreRate(stats.getHighScoreRate() != null ? stats.getHighScoreRate() + "%" : "0%");
        data.setPassRate(stats.getPassRate() != null ? stats.getPassRate() + "%" : "0%");
        data.setTypeStats(formatTypeStats(stats));
        data.setDifficultyStats(formatDifficultyStats(stats));
        data.setKnowledgeStats(formatKnowledgeStats(stats));
        data.setCalculationTime(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));

        String fileName = (stats.getSubject() != null ? stats.getSubject() : "统计") + "-成绩统计-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20"));

        EasyExcel.write(response.getOutputStream(), StatisticsExcelData.class)
                .sheet("成绩统计").doWrite(List.of(data));
    }

    @GetMapping("/export/wrong-excel/{paperId}")
    @SaCheckLogin
    @Operation(summary = "导出高频错题为Excel")
    public void exportWrongExcel(@PathVariable Long paperId,
                                 HttpServletResponse response) throws IOException {
        List<HighFreqWrongQuestionVO> wrongList =
                statisticsMapper.selectHighFreqWrongQuestions(paperId, 200);

        for (HighFreqWrongQuestionVO wq : wrongList) {
            wq.setQuestionTypeName(getTypeName(wq.getQuestionType()));
            wq.setDifficultyName(getDiffName(wq.getDifficulty()));
        }

        List<HighFreqWrongExcelData> excelData = wrongList.stream().map(wq -> {
            HighFreqWrongExcelData d = new HighFreqWrongExcelData();
            d.setQuestionId(wq.getQuestionId());
            d.setQuestionContent(wq.getQuestionContent());
            d.setQuestionTypeName(wq.getQuestionTypeName());
            d.setDifficultyName(wq.getDifficultyName());
            d.setKnowledgePoints(wq.getKnowledgePoints());
            d.setWrongCount(wq.getWrongCount());
            d.setTotalScoreLost(wq.getTotalScoreLost());
            return d;
        }).collect(Collectors.toList());

        String fileName = "高频错题-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20"));

        EasyExcel.write(response.getOutputStream(), HighFreqWrongExcelData.class)
                .sheet("高频错题").doWrite(excelData);
    }

    private String getTypeName(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "单选题";
            case 2: return "多选题";
            case 3: return "填空题";
            case 4: return "简答题";
            default: return "未知";
        }
    }

    private String getDiffName(Integer diff) {
        if (diff == null) return "未知";
        switch (diff) {
            case 1: return "简单";
            case 2: return "中等";
            case 3: return "困难";
            default: return "未知";
        }
    }

    private String formatTypeStats(PaperStatisticsVO stats) {
        if (stats.getQuestionTypeStats() == null || stats.getQuestionTypeStats().isEmpty()) return "";
        return stats.getQuestionTypeStats().stream()
                .map(t -> t.getQuestionTypeName() + ": " + t.getTotalCount() + "题, 正确率" +
                        (t.getCorrectRate() != null ? t.getCorrectRate() : "0") + "%")
                .collect(Collectors.joining("; "));
    }

    private String formatDifficultyStats(PaperStatisticsVO stats) {
        if (stats.getDifficultyStats() == null || stats.getDifficultyStats().isEmpty()) return "";
        return stats.getDifficultyStats().stream()
                .map(d -> d.getDifficultyName() + ": " + d.getTotalCount() + "题, 正确率" +
                        (d.getCorrectRate() != null ? d.getCorrectRate() : "0") + "%")
                .collect(Collectors.joining("; "));
    }

    private String formatKnowledgeStats(PaperStatisticsVO stats) {
        if (stats.getKnowledgePointStats() == null || stats.getKnowledgePointStats().isEmpty()) return "";
        return stats.getKnowledgePointStats().stream()
                .limit(10)
                .map(k -> k.getKnowledgePoint() + ": 正确率" +
                        (k.getCorrectRate() != null ? k.getCorrectRate() : "0") + "%")
                .collect(Collectors.joining("; "));
    }
}
