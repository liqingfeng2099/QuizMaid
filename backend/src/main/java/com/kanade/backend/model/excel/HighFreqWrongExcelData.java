package com.kanade.backend.model.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class HighFreqWrongExcelData {
    @ExcelProperty("题号")
    private Long questionId;

    @ExcelProperty("题干")
    private String questionContent;

    @ExcelProperty("题型")
    private String questionTypeName;

    @ExcelProperty("难度")
    private String difficultyName;

    @ExcelProperty("知识点")
    private String knowledgePoints;

    @ExcelProperty("错误次数")
    private Long wrongCount;

    @ExcelProperty("失分合计")
    private Integer totalScoreLost;
}
