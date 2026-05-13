package com.kanade.backend.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LargeAssemblyMessageDTO implements Serializable {
    private Long taskId;
    private Long userId;
    private Long strategyId;
    private String subject;
    private String paperName;
    private Integer paperStatus;
    private String algorithmType; // GREEDY / GENETIC
}
