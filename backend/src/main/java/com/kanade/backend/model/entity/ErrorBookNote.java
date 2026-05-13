package com.kanade.backend.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Table("errorBookNote")
public class ErrorBookNote {
    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("errorBookId")
    private Long errorBookId;
    @Column("userId")
    private Long userId;
    @Column("noteType")
    private Integer noteType;
    @Column("content")
    private String content;
    @Column("imageUrl")
    private String imageUrl;
    @Column("createTime")
    private LocalDateTime createTime;
    @Column("updateTime")
    private LocalDateTime updateTime;
    @Column(value = "isDeleted", isLogicDelete = true)
    private Integer isDeleted;
}
