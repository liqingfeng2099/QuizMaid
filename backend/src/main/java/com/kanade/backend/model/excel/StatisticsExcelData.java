package com.kanade.backend.model.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class StatisticsExcelData {
    @ExcelProperty("试卷名称")
    private String paperName;

    @ExcelProperty("学科")
    private String subject;

    @ExcelProperty("总分")
    private Integer totalScore;

    @ExcelProperty("最高分")
    private BigDecimal maxScore;

    @ExcelProperty("最低分")
    private BigDecimal minScore;

    @ExcelProperty("平均分")
    private BigDecimal avgScore;

    @ExcelProperty("中位数")
    private BigDecimal medianScore;

    @ExcelProperty("参考人数")
    private Integer totalExaminees;

    @ExcelProperty("高分率(>=90%)")
    private String highScoreRate;

    @ExcelProperty("及格率(>=60%)")
    private String passRate;

    @ExcelProperty("题型统计")
    private String typeStats;

    @ExcelProperty("难度统计")
    private String difficultyStats;

    @ExcelProperty("知识点统计")
    private String knowledgeStats;

    @ExcelProperty("统计时间")
    private String calculationTime;
}
