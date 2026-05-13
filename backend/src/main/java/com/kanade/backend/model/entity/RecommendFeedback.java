package com.kanade.backend.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Table("recommendfeedback")
public class RecommendFeedback {
    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("userId")
    private Long userId;
    @Column("questionId")
    private Long questionId;
    @Column("knowledgePoints")
    private String knowledgePoints;
    @Column("feedback")
    private Integer feedback;
    @Column("createTime")
    private LocalDateTime createTime;
    @Column(value = "isDeleted", isLogicDelete = true)
    private Integer isDeleted;
}
