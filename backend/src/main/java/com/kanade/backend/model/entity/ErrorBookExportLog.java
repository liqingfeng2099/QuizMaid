package com.kanade.backend.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Table("errorbookexportlog")
public class ErrorBookExportLog {
    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("userId")
    private Long userId;
    @Column("exportType")
    private String exportType;
    @Column("exportStatus")
    private Integer exportStatus;
    @Column("fileName")
    private String fileName;
    @Column("exportPath")
    private String exportPath;
    @Column("exportConfig")
    private String exportConfig;
    @Column("createTime")
    private LocalDateTime createTime;
    @Column("updateTime")
    private LocalDateTime updateTime;
    @Column(value = "isDeleted", isLogicDelete = true)
    private Integer isDeleted;
}
