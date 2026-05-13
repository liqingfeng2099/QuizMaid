package com.kanade.backend.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Table("paperShare")
public class PaperShare {
    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("paperId")
    private Long paperId;
    @Column("ownerId")
    private Long ownerId;
    @Column("targetUserId")
    private Long targetUserId;
    @Column("targetGroupId")
    private Long targetGroupId;
    @Column("createTime")
    private LocalDateTime createTime;
    @Column("updateTime")
    private LocalDateTime updateTime;
    @Column(value = "isDeleted", isLogicDelete = true)
    private Integer isDeleted;
}
