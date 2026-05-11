package com.kanade.backend.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("paperStrategy")
public class PaperStrategy implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("strategyName")
    private String strategyName;

    @Column("userId")
    private Long userId;

    @Column("totalScore")
    private Integer totalScore;

    @Column("difficultyAvg")
    private Integer difficultyAvg;

    private Integer duration;

    @Column("questionTypeConfig")
    private String questionTypeConfig;

    @Column("difficultyConfig")
    private String difficultyConfig;

    @Column("knowledgePointScope")
    private String knowledgePointScope;

    @Column("isDefault")
    private Integer isDefault;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDeleted", isLogicDelete = true)
    private Integer isDeleted;
}
