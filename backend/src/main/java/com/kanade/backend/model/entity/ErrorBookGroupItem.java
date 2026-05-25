package com.kanade.backend.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Table("errorBookGroupItem")
public class ErrorBookGroupItem {
    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("groupId")
    private Long groupId;
    @Column("errorBookId")
    private Long errorBookId;
    @Column("createTime")
    private LocalDateTime createTime;
    @Column(value = "isDeleted", isLogicDelete = true)
    private Integer isDeleted;
}
