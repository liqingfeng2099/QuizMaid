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
@Table("aiPaperChat")
public class AiPaperChat implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("userId")
    private Long userId;

    @Column("paperId")
    private Long paperId;

    @Column("strategyId")
    private Long strategyId;

    @Column("sessionRound")
    private Integer sessionRound;

    @Column("chatContent")
    private String chatContent;

    @Column("aiResponse")
    private String aiResponse;

    private Integer status;

    @Column("retryCount")
    private Integer retryCount;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDeleted", isLogicDelete = true)
    private Integer isDeleted;
}
