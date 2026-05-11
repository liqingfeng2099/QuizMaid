package com.kanade.backend.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 试卷试题关联表 实体类。
 *
 * @author kanade
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("paperquestion")
public class Paperquestion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 试卷ID
     */
    @Column("paperId")
    private Long paperId;

    /**
     * 试题ID
     */
    @Column("questionId")
    private Long questionId;

    /**
     * 试题分值
     */
    @Column("questionScore")
    private Integer questionScore;

    /**
     * 排序
     */
    private Integer sort;

    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 是否自动组卷添加 0-否 1-是
     */
    @Column("isAutoAdd")
    private Integer isAutoAdd;

    @Column(value = "isDeleted", isLogicDelete = true)
    private Integer isDeleted;

}
