package com.kanade.backend.model.dto;

import com.kanade.backend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PaperStrategyQueryDTO extends PageRequest {
    private String strategyName;
    private Integer difficultyAvg;
    private Integer isDefault;
}
