package com.kanade.backend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportFileVO {
    private Long id;
    private Long paperId;
    private String fileName;
    private String filePath;
    private String exportType;
    private Integer exportStatus;
    private LocalDateTime createTime;
}
