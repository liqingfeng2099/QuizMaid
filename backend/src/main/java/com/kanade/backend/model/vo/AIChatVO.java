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
public class AIChatVO {

    private Long id;
    private Long userId;
    private Long paperId;
    private Long strategyId;
    private Integer sessionRound;
    private String chatContent;
    private String aiResponse;
    private Integer status;
    private Integer retryCount;
    private LocalDateTime createTime;
}
