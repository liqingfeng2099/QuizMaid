package com.kanade.backend.ai.strategy;

import cn.hutool.json.JSONUtil;
import com.kanade.backend.ai.AiService;
import com.kanade.backend.ai.AiServiceFactory;
import com.kanade.backend.model.dto.AIPaperAssemblyV2DTO;
import com.kanade.backend.model.entity.PaperStrategy;
import com.kanade.backend.model.entity.StrategyWeight;
import com.kanade.backend.model.vo.AIProfileVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 策略推断服务 —— 将自然语言组卷需求转换为结构化策略参数
 * <p>
 * 对应 A+C 混合组卷流程的阶段1：
 * 自然语言需求 → LLM 推断 → PaperStrategy + StrategyWeight
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StrategyInferenceService {

    private final AiServiceFactory aiServiceFactory;

    /**
     * 推断组卷策略（含 DB 持久化）
     *
     * @return 完整的策略+权重对，或 null（推断失败时返回默认策略）
     */
    public StrategyInferenceResult infer(AIPaperAssemblyV2DTO dto, AIProfileVO profile) {
        // 1. 构建推断提示词
        String prompt = buildInferencePrompt(dto, profile);

        // 2. 调用 LLM
        String aiResponse = null;
        try {
            AiService ai = aiServiceFactory.createAiCodeGeneratorService();
            aiResponse = ai.generateStrategyInference(prompt);
            log.info("[策略推断] AI 响应: {}", aiResponse);
        } catch (Exception e) {
            log.error("[策略推断] LLM 调用失败，将使用默认策略", e);
            return buildDefaultResult(dto);
        }

        // 3. 解析响应
        InferredParams params = parseResponse(aiResponse);
        if (params == null) {
            log.warn("[策略推断] 解析失败，将使用默认策略。原始响应: {}", aiResponse);
            return buildDefaultResult(dto);
        }

        // 4. 校验参数合法性
        params = validateAndFix(params, dto);

        // 5. 构建返回结果
        return StrategyInferenceResult.builder()
                .params(params)
                .aiResponse(aiResponse)
                .inferenceSuccess(true)
                .build();
    }

    // ==================== Prompt 构建 ====================

    private String buildInferencePrompt(AIPaperAssemblyV2DTO dto, AIProfileVO profile) {
        StringBuilder sb = new StringBuilder();

        // 个性化画像
        if (profile != null) {
            sb.append("【用户学习画像】\n");
            sb.append("- 总答题数: ").append(profile.getAnswerNum())
                    .append(", 正确率: ").append(profile.getAccuracy() != null ? profile.getAccuracy() : "N/A").append("%\n");
            if (profile.getWeakPoints() != null && !profile.getWeakPoints().isEmpty()) {
                sb.append("- 薄弱知识点: ");
                profile.getWeakPoints().stream().limit(5).forEach(wp ->
                        sb.append(wp.getKnowledgePoint()).append("(正确率").append(wp.getAccuracy()).append("%) "));
                sb.append("\n");
            }
            sb.append("\n");
        }

        // 表单约束
        sb.append("【组卷约束条件】\n");
        if (dto.getSubject() != null && !dto.getSubject().isBlank()) {
            sb.append("- 学科: ").append(dto.getSubject()).append("\n");
        }
        if (dto.getChapter() != null && !dto.getChapter().isBlank()) {
            sb.append("- 章节: ").append(dto.getChapter()).append("\n");
        }
        if (dto.getDifficulty() != null) {
            sb.append("- 用户指定难度: ").append(difficultyText(dto.getDifficulty())).append(" (级别").append(dto.getDifficulty()).append(")\n");
        }
        if (dto.getTotalScore() != null) {
            sb.append("- 目标总分: ").append(dto.getTotalScore()).append("\n");
        }
        sb.append("\n");

        // 自然语言需求
        if (dto.getUserRequirement() != null && !dto.getUserRequirement().isBlank()) {
            sb.append("【用户自然语言需求】\n");
            sb.append(dto.getUserRequirement()).append("\n\n");
        } else {
            sb.append("【用户自然语言需求】\n");
            sb.append("（用户未提供额外需求，请根据表单约束推断合适的策略）\n\n");
        }

        // 个性化指令
        if (Boolean.TRUE.equals(dto.getIncludeWeakAreas()) && profile != null
                && profile.getWeakPoints() != null && !profile.getWeakPoints().isEmpty()) {
            sb.append("【特殊指令】\n");
            sb.append("用户要求聚焦薄弱知识点，请将薄弱知识点加入 knowledgePointScope，");
            sb.append("并提高 knowledgeCount 权重至 25-35。\n");
        }

        return sb.toString();
    }

    // ==================== 响应解析 ====================

    @SuppressWarnings("unchecked")
    private InferredParams parseResponse(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) return null;

        try {
            String json = aiResponse.trim();
            // 去除可能的 markdown 代码块标记
            if (json.startsWith("```")) {
                json = json.substring(json.indexOf("\n") + 1);
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.lastIndexOf("```")).trim();
            }
            if (json.startsWith("json")) {
                json = json.substring(4).trim();
            }

            Map<String, Object> map = JSONUtil.toBean(json, Map.class);
            InferredParams params = new InferredParams();

            // difficultyAvg
            if (map.get("difficultyAvg") instanceof Number) {
                params.setDifficultyAvg(((Number) map.get("difficultyAvg")).intValue());
            }

            // duration
            if (map.get("duration") instanceof Number) {
                params.setDuration(((Number) map.get("duration")).intValue());
            }

            // difficultyConfig
            if (map.get("difficultyConfig") instanceof List) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) map.get("difficultyConfig");
                params.setDifficultyConfig(list.stream()
                        .map(m -> new DiffConfigItem(
                                ((Number) m.get("level")).intValue(),
                                ((Number) m.get("ratio")).doubleValue()))
                        .collect(Collectors.toList()));
            }

            // questionTypeConfig
            if (map.get("questionTypeConfig") instanceof List) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) map.get("questionTypeConfig");
                params.setQuestionTypeConfig(list.stream()
                        .map(m -> new TypeConfigItem(
                                ((Number) m.get("type")).intValue(),
                                ((Number) m.get("count")).intValue(),
                                ((Number) m.get("score")).intValue()))
                        .collect(Collectors.toList()));
            }

            // weights
            if (map.get("weights") instanceof List) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) map.get("weights");
                params.setWeights(list.stream()
                        .map(m -> new WeightItem(
                                String.valueOf(m.get("weightType")),
                                ((Number) m.get("weightValue")).intValue()))
                        .collect(Collectors.toList()));
            }

            // knowledgePointScope
            if (map.get("knowledgePointScope") instanceof List) {
                params.setKnowledgePointScope(
                        ((List<?>) map.get("knowledgePointScope")).stream()
                                .map(Object::toString)
                                .collect(Collectors.toList()));
            }

            // strategyDescription
            if (map.get("strategyDescription") instanceof String) {
                params.setStrategyDescription((String) map.get("strategyDescription"));
            }

            return params;
        } catch (Exception e) {
            log.error("[策略推断] JSON 解析异常", e);
            return null;
        }
    }

    // ==================== 参数校验与修复 ====================

    private InferredParams validateAndFix(InferredParams params, AIPaperAssemblyV2DTO dto) {
        // 难度均值修正
        if (params.getDifficultyAvg() == null || params.getDifficultyAvg() < 1 || params.getDifficultyAvg() > 5) {
            params.setDifficultyAvg(dto.getDifficulty() != null ? dto.getDifficulty() : 3);
        }

        // 时长修正
        if (params.getDuration() == null || params.getDuration() < 10) {
            params.setDuration(90);
        }

        // 题型配置修正
        if (params.getQuestionTypeConfig() == null || params.getQuestionTypeConfig().isEmpty()) {
            params.setQuestionTypeConfig(defaultTypeConfig());
        }

        // 难度分布修正：ratio 之和必须等于 1.0
        if (params.getDifficultyConfig() == null || params.getDifficultyConfig().isEmpty()) {
            params.setDifficultyConfig(defaultDifficultyConfig(params.getDifficultyAvg()));
        } else {
            double sum = params.getDifficultyConfig().stream().mapToDouble(DiffConfigItem::getRatio).sum();
            if (Math.abs(sum - 1.0) > 0.01) {
                // 归一化
                for (DiffConfigItem item : params.getDifficultyConfig()) {
                    item.setRatio(item.getRatio() / sum);
                }
            }
        }

        // 权重修正：总和必须等于 100
        if (params.getWeights() == null || params.getWeights().isEmpty()) {
            params.setWeights(defaultWeights());
        } else {
            int sum = params.getWeights().stream().mapToInt(WeightItem::getWeightValue).sum();
            if (sum != 100 && sum > 0) {
                // 按比例缩放到 100
                int remaining = 100;
                for (int i = 0; i < params.getWeights().size() - 1; i++) {
                    int scaled = (int) Math.round(params.getWeights().get(i).getWeightValue() * 100.0 / sum);
                    params.getWeights().get(i).setWeightValue(scaled);
                    remaining -= scaled;
                }
                // 最后一个用剩余值（确保总和为 100）
                params.getWeights().get(params.getWeights().size() - 1).setWeightValue(Math.max(0, remaining));
            } else if (sum == 0) {
                params.setWeights(defaultWeights());
            }
        }

        // 知识点范围修正
        if (params.getKnowledgePointScope() == null) {
            params.setKnowledgePointScope(Collections.emptyList());
        }

        return params;
    }

    // ==================== 默认策略 ====================

    private StrategyInferenceResult buildDefaultResult(AIPaperAssemblyV2DTO dto) {
        InferredParams params = new InferredParams();
        params.setDifficultyAvg(dto.getDifficulty() != null ? dto.getDifficulty() : 3);
        params.setDuration(90);
        params.setQuestionTypeConfig(defaultTypeConfig());
        params.setDifficultyConfig(defaultDifficultyConfig(params.getDifficultyAvg()));
        params.setWeights(defaultWeights());
        params.setKnowledgePointScope(Collections.emptyList());
        params.setStrategyDescription("默认均衡策略（AI 推断未成功，使用系统默认值）");

        return StrategyInferenceResult.builder()
                .params(params)
                .aiResponse(null)
                .inferenceSuccess(false)
                .build();
    }

    private List<TypeConfigItem> defaultTypeConfig() {
        return Arrays.asList(
                new TypeConfigItem(1, 10, 5),   // 单选 10题 × 5分
                new TypeConfigItem(2, 5, 6),    // 多选 5题 × 6分
                new TypeConfigItem(3, 5, 8),    // 填空 5题 × 8分
                new TypeConfigItem(4, 3, 10)    // 简答 3题 × 10分
        );
    }

    private List<DiffConfigItem> defaultDifficultyConfig(int avgDifficulty) {
        if (avgDifficulty <= 2) {
            return Arrays.asList(
                    new DiffConfigItem(1, 0.5),
                    new DiffConfigItem(2, 0.3),
                    new DiffConfigItem(3, 0.2)
            );
        } else if (avgDifficulty >= 4) {
            return Arrays.asList(
                    new DiffConfigItem(1, 0.15),
                    new DiffConfigItem(2, 0.25),
                    new DiffConfigItem(3, 0.6)
            );
        } else {
            return Arrays.asList(
                    new DiffConfigItem(1, 0.2),
                    new DiffConfigItem(2, 0.5),
                    new DiffConfigItem(3, 0.3)
            );
        }
    }

    private List<WeightItem> defaultWeights() {
        return Arrays.asList(
                new WeightItem("difficulty", 30),
                new WeightItem("accuracy", 15),
                new WeightItem("discrimination", 20),
                new WeightItem("calcLevel", 10),
                new WeightItem("examFrequency", 10),
                new WeightItem("knowledgeCount", 15)
        );
    }

    private String difficultyText(Integer d) {
        if (d == null) return "未知";
        switch (d) {
            case 1: return "简单";
            case 2: return "中等";
            case 3: return "困难";
            default: return "未知";
        }
    }

    // ==================== 数据类 ====================

    @Data
    public static class InferredParams {
        private Integer difficultyAvg;
        private Integer duration;
        private List<TypeConfigItem> questionTypeConfig;
        private List<DiffConfigItem> difficultyConfig;
        private List<WeightItem> weights;
        private List<String> knowledgePointScope;
        private String strategyDescription;
    }

    @Data
    @Builder
    public static class StrategyInferenceResult {
        private InferredParams params;
        private String aiResponse;
        private boolean inferenceSuccess;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TypeConfigItem {
        private int type;
        private int count;
        private int score;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", type);
            map.put("count", count);
            map.put("score", score);
            return map;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DiffConfigItem {
        private int level;
        private double ratio;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("level", level);
            map.put("ratio", ratio);
            return map;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WeightItem {
        private String weightType;
        private int weightValue;
    }
}
