package com.kanade.backend.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Table("errorBook")
public class ErrorBook {
    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("userId")
    private Long userId;
    @Column("questionId")
    private Long questionId;
    @Column("errorType")
    private Integer errorType;
    @Column("reviewStatus")
    private Integer reviewStatus;
    @Column("errorCount")
    private Integer errorCount;
    @Column("firstErrorTime")
    private LocalDateTime firstErrorTime;
    @Column("lastErrorTime")
    private LocalDateTime lastErrorTime;
    @Column("isArchived")
    private Integer isArchived;
    @Column("createTime")
    private LocalDateTime createTime;
    @Column("updateTime")
    private LocalDateTime updateTime;
    @Column(value = "isDeleted", isLogicDelete = true)
    private Integer isDeleted;
}
