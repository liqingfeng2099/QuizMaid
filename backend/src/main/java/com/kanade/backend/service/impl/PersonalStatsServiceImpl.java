package com.kanade.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.kanade.backend.mapper.PersonalStatsMapper;
import com.kanade.backend.model.vo.*;
import com.kanade.backend.service.PersonalStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonalStatsServiceImpl implements PersonalStatsService {

    private final PersonalStatsMapper personalStatsMapper;
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public PersonalStatsVO getPersonalStats(String subject, String startDate, String endDate) {
        Long userId = StpUtil.getLoginIdAsLong();
        PersonalStatsVO vo = new PersonalStatsVO();

        // 总数
        long total = personalStatsMapper.totalAnswersByUser(userId);
        long correct = personalStatsMapper.totalCorrectByUser(userId);
        vo.setTotalAnswers(total);
        vo.setTotalCorrect(correct);
        vo.setTotalAccuracy(total > 0 ? BigDecimal.valueOf(correct * 100.0 / total)
                .setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        // 按题型
        vo.setByType(enrichTypeStats(personalStatsMapper.statsByType(userId, subject, startDate, endDate)));

        // 按难度
        vo.setByDifficulty(enrichDiffStats(personalStatsMapper.statsByDifficulty(userId, subject, startDate, endDate)));

        // 按知识点（拆分逗号分隔）
        vo.setByKnowledge(computeKPStats(personalStatsMapper.statsByKnowledgeRaw(userId, subject, startDate, endDate)));

        // 趋势
        String trendStart = startDate != null ? startDate : LocalDate.now().minusDays(30).format(DF);
        vo.setTrend(personalStatsMapper.dailyTrend(userId, subject, trendStart));

        return vo;
    }

    @Override
    public PersonalStatsVO getPersonalStatsWithPeriod(String subject, String period) {
        String endDate = LocalDate.now().format(DF);
        String startDate;
        switch (period != null ? period : "30d") {
            case "7d": startDate = LocalDate.now().minusDays(7).format(DF); break;
            case "30d": startDate = LocalDate.now().minusDays(30).format(DF); break;
            case "90d": startDate = LocalDate.now().minusDays(90).format(DF); break;
            default: startDate = null;
        }
        return getPersonalStats(subject, startDate, endDate);
    }

    private List<PersonalDimensionVO> enrichTypeStats(List<PersonalDimensionVO> list) {
        Map<String, String> typeNames = Map.of("1","单选题","2","多选题","3","填空题","4","简答题");
        for (PersonalDimensionVO v : list) {
            v.setDimensionKey(typeNames.getOrDefault(v.getDimensionKey(), v.getDimensionKey()));
            v.setCorrectRate(calcRate(v.getCorrectCount(), v.getTotalCount()));
        }
        return list;
    }

    private List<PersonalDimensionVO> enrichDiffStats(List<PersonalDimensionVO> list) {
        Map<String, String> diffNames = Map.of("1","简单","2","中等","3","困难");
        for (PersonalDimensionVO v : list) {
            v.setDimensionKey(diffNames.getOrDefault(v.getDimensionKey(), v.getDimensionKey()));
            v.setCorrectRate(calcRate(v.getCorrectCount(), v.getTotalCount()));
        }
        return list;
    }

    private List<PersonalDimensionVO> computeKPStats(List<PersonalDimensionVO> raw) {
        if (raw.isEmpty()) return Collections.emptyList();
        Map<String, PersonalDimensionVO> agg = new LinkedHashMap<>();
        for (PersonalDimensionVO r : raw) {
            if (StrUtil.isBlank(r.getDimensionKey())) continue;
            String[] kps = r.getDimensionKey().split(",");
            int n = kps.length;
            for (String kp : kps) {
                String key = kp.trim();
                if (key.isEmpty()) continue;
                agg.compute(key, (k, v) -> {
                    if (v == null) { v = new PersonalDimensionVO(); v.setDimensionKey(key); v.setTotalCount(0L); v.setCorrectCount(0L); }
                    v.setTotalCount(v.getTotalCount() + r.getTotalCount() / n);
                    v.setCorrectCount(v.getCorrectCount() + r.getCorrectCount() / n);
                    return v;
                });
            }
        }
        List<PersonalDimensionVO> result = new ArrayList<>(agg.values());
        result.forEach(v -> v.setCorrectRate(calcRate(v.getCorrectCount(), v.getTotalCount())));
        result.sort((a, b) -> Long.compare(b.getTotalCount(), a.getTotalCount()));
        return result;
    }

    private BigDecimal calcRate(long num, long den) {
        if (den == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(num * 100.0 / den).setScale(2, RoundingMode.HALF_UP);
    }
}
