package com.kanade.backend.model.enums;

import lombok.Getter;

@Getter
public enum RateLevel {
    L1_STRICT(5, 60),
    L2_MEDIUM(30, 60),
    L3_LOOSE(100, 60);

    private final int defaultMaxRequests;
    private final int defaultTimeWindow;

    RateLevel(int defaultMaxRequests, int defaultTimeWindow) {
        this.defaultMaxRequests = defaultMaxRequests;
        this.defaultTimeWindow = defaultTimeWindow;
    }
}
