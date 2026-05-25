package com.kanade.backend.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Table("systemNotification")
public class SystemNotification {
    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("userId")
    private Long userId;
    private String title;
    private String content;
    @Column("type")
    private Integer type;
    @Column("isRead")
    private Integer isRead;
    private String link;
    @Column("createTime")
    private LocalDateTime createTime;
    @Column(value = "isDeleted", isLogicDelete = true)
    private Integer isDeleted;
}
