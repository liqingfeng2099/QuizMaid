package com.kanade.backend.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("examPaper")
public class ExamPaper implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("paperName")
    private String paperName;

    private String subject;
    @Column("totalScore")
    private Integer totalScore;

    @Column("creatorId")
    private Long creatorId;

    private Integer status; // 0-草稿 1-发布 2-归档 3-停用

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 组卷策略ID
     */
    @Column("strategyId")
    private Long strategyId;

    /**
     * 试卷类型 1-手动组卷 2-自动组卷 3-AI组卷
     */
    @Column("paperType")
    private Integer paperType;

    /**
     * 整体难度系数
     */
    @Column("difficultyRate")
    private BigDecimal difficultyRate;

    /**
     * 答题时长（分钟）
     */
    private Integer duration;

    /**
     * 导出状态 0-未导出 1-已导出
     */
    @Column("exportStatus")
    private Integer exportStatus;

    @Column(value = "isDeleted", isLogicDelete = true)
    private Integer isDeleted;
}