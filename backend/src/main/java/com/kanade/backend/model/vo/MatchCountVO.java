package com.kanade.backend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchCountVO {
    private Integer totalCount;
    private Map<Integer, Integer> byType;
    private Map<Integer, Integer> byDifficulty;
}
