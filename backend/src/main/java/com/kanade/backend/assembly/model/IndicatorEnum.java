package com.kanade.backend.assembly.model;

import lombok.Getter;

@Getter
public enum IndicatorEnum {

    DIFFICULTY("difficulty", "难度", 1, 5),
    ACCURACY("accuracy", "正确率", 0, 100),
    DISCRIMINATION("discrimination", "区分度", -1, 1),
    CALC_LEVEL("calcLevel", "计算量", 1, 3),
    EXAM_FREQUENCY("examFrequency", "考频", 0, 100),
    KNOWLEDGE_COUNT("knowledgeCount", "考点关联数", 1, 10);

    private final String code;
    private final String label;
    private final double minValue;
    private final double maxValue;

    IndicatorEnum(String code, String label, double minValue, double maxValue) {
        this.code = code;
        this.label = label;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * 归一化：将原始值映射到 [0, 1]
     */
    public double normalize(double rawValue) {
        if (rawValue < minValue) rawValue = minValue;
        if (rawValue > maxValue) rawValue = maxValue;
        return (rawValue - minValue) / (maxValue - minValue);
    }

    public static IndicatorEnum fromCode(String code) {
        for (IndicatorEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
