package com.kanade.backend.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class MatchCountDTO {
    private String subject;
    private String chapter;
    private Integer difficulty;
    private List<Integer> types;
    private String knowledgePoints;
}
