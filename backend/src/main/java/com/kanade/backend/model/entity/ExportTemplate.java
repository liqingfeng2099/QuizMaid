package com.kanade.backend.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Table("exporttemplate")
public class ExportTemplate {
    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("templateName")
    private String templateName;
    @Column("userId")
    private Long userId;
    @Column("exportType")
    private String exportType;      // PDF / Word
    @Column("config")
    private String config;          // JSON: 标题、排版、答案/解析开关、分值位置等
    @Column("isDefault")
    private Integer isDefault;
    @Column("createTime")
    private LocalDateTime createTime;
    @Column("updateTime")
    private LocalDateTime updateTime;
    @Column(value = "isDeleted", isLogicDelete = true)
    private Integer isDeleted;
}
