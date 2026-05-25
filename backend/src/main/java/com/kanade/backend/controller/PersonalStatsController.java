package com.kanade.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.alibaba.excel.EasyExcel;
import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.model.vo.PersonalStatsVO;
import com.kanade.backend.service.PersonalStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/personal")
@RequiredArgsConstructor
@Tag(name = "个人多维度统计")
public class PersonalStatsController {

    private final PersonalStatsService personalStatsService;

    @GetMapping("/stats")
    @SaCheckLogin
    @Operation(summary = "获取个人多维度统计数据")
    public BaseResponse<PersonalStatsVO> getStats(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        PersonalStatsVO vo;
        if (period != null) {
            vo = personalStatsService.getPersonalStatsWithPeriod(subject, period);
        } else {
            vo = personalStatsService.getPersonalStats(subject, startDate, endDate);
        }
        return ResultUtils.success(vo);
    }

    @GetMapping("/stats/export")
    @SaCheckLogin
    @Operation(summary = "导出个人统计Excel")
    public void exportExcel(@RequestParam(required = false) String subject,
                            @RequestParam(defaultValue = "30d") String period,
                            HttpServletResponse response) throws IOException {
        PersonalStatsVO stats = personalStatsService.getPersonalStatsWithPeriod(subject, period);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = "个人学习统计-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20"));

        List<java.util.Map<String, String>> rows = new java.util.ArrayList<>();
        java.util.Map<String, String> header = new java.util.LinkedHashMap<>();
        header.put("total", "总答题数:" + stats.getTotalAnswers() + " 总正确率:" +
                (stats.getTotalAccuracy() != null ? stats.getTotalAccuracy() : "0") + "%");
        rows.add(header);

        for (var d : stats.getByType()) {
            java.util.Map<String, String> row = new java.util.LinkedHashMap<>();
            row.put("total", d.getDimensionKey() + ": " + d.getCorrectRate() + "% (" +
                    d.getCorrectCount() + "/" + d.getTotalCount() + ")");
            rows.add(row);
        }
        EasyExcel.write(response.getOutputStream()).head(head()).sheet("个人统计").doWrite(rows);
    }

    private List<List<String>> head() {
        return List.of(List.of("统计内容"));
    }
}
