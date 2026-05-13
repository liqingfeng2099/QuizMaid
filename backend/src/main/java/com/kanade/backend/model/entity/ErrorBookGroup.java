package com.kanade.backend.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Table("errorBookGroup")
public class ErrorBookGroup {
    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("userId")
    private Long userId;
    @Column("groupName")
    private String groupName;
    @Column("description")
    private String description;
    @Column("sort")
    private Integer sort;
    @Column("createTime")
    private LocalDateTime createTime;
    @Column("updateTime")
    private LocalDateTime updateTime;
    @Column(value = "isDeleted", isLogicDelete = true)
    private Integer isDeleted;
}
