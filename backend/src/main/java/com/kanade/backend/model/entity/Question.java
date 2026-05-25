package com.kanade.backend.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.io.Serial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 试题主表 实体类。
 *
 * @author kanade
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("question")
public class Question implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 题干MD5指纹，用于查重
     */
    @Column("questionMd5")
    private String questionMd5;

    /**
     * 复合MD5(题干+题型+选项+答案)，运行时计算，不持久化到数据库
     */
    @Column(ignore = true)
    private String compositeMd5;

    /**
     * 题型 1-单选 2-多选 3-填空 4-简答
     */
    private Integer type;

    /**
     * 学科
     */
    private String subject;

    /**
     * 章节
     */
    private String chapter;

    /**
     * 难度 1-易 2-中 3-难
     */
    private Integer difficulty;

    /**
     * 知识点，逗号分隔
     */
    @Column("knowledgePoints")
    private String knowledgePoints;

    /**
     * 题目标签（字符串数组）
     */
    private String tags;

    /**
     * 题干内容
     */
    private String content;

    /**
     * 选项JSON
     */
    private String options;

    /**
     * 标准答案
     */
    private String answer;

    /**
     * 解析
     */
    private String analysis;

    /**
     * 创建人ID
     */
    @Column("creatorId")
    private Long creatorId;

    /**
     * 状态 1-草稿 2-已发布 3-停用
     */
    private Integer status;

    /**
     * 做对次数
     */
    @Column("correctCount")
    private Long correctCount;

    /**
     * 做题总次数
     */
    @Column("totalCount")
    private Long totalCount;

    /**
     * 正确率
     */
    private BigDecimal accuracy;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 区分度 1-5
     */
    private Integer discrimination;

    /**
     * 计算量等级 1-3
     */
    @Column("calcLevel")
    private Integer calcLevel;

    /**
     * 考频 0-100
     */
    @Column("examFrequency")
    private Integer examFrequency;

    /**
     * 学段ID
     */
    @Column("gradeStageId")
    private Long gradeStageId;

    @Column(value = "isDeleted", isLogicDelete = true)
    private Integer isDeleted;

}
