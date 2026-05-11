package com.kanade.backend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIProfileVO {

    /** 总答题数 */
    private Long answerNum;

    /** 总做对题数 */
    private Long correctNum;

    /** 整体正确率 */
    private BigDecimal accuracy;

    /** 薄弱知识点列表（正确率<60%） */
    private List<WeakPoint> weakPoints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeakPoint {
        private String knowledgePoint;
        private BigDecimal accuracy;
        private Long totalCount;
    }
}
