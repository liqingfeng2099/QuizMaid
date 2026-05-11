package com.kanade.backend;

import cn.dev33.satoken.stp.StpUtil;
import com.kanade.backend.model.vo.UserHeatMapVO;
import com.kanade.backend.service.UserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;

@SpringBootTest
public class SignTest {

    @Autowired
    private WebApplicationContext context;

    @Resource
    UserService userService;

    private MockMvc mockMvc;

    private void setupMockMvc() {
        if (mockMvc == null) {
            mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        }
    }

    @Test
    void testGetContinuousSignInDays() throws Exception {
        setupMockMvc();

        // 执行请求并验证响应状态
        mockMvc.perform(MockMvcRequestBuilders.get("/users/continuous_sign_in_days"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(result -> {
                    String response = result.getResponse().getContentAsString();
                    System.out.println("连续签到天数响应：" + response);
                    assert response.contains("\"code\":0") || response.contains("\"data\"");
                });
    }

    @Test
    void testGetHeatMap() throws Exception {
        setupMockMvc();

        // 测试获取自己的热力图
        long userId = StpUtil.getLoginIdAsLong();

        mockMvc.perform(MockMvcRequestBuilders.get("/users/heatmap/" + userId))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(result -> {
                    String response = result.getResponse().getContentAsString();
                    System.out.println("热力图响应：" + response);
                    assert response.contains("\"code\":0") || response.contains("\"data\"");
                });
    }

    @Test
    void testGetHeatMapForOtherUser() throws Exception {
        setupMockMvc();

        // 测试查看其他用户的热力图（使用一个示例 ID）
        long otherUserId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.get("/users/heatmap/" + otherUserId))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(result -> {
                    String response = result.getResponse().getContentAsString();
                    System.out.println("其他用户热力图响应：" + response);
                    assert response.contains("\"code\":0") || response.contains("\"data\"");
                });
    }

    @Test
    void testUserServiceGetSignDays() {
        // 直接测试 Service 层方法
        long loginId = StpUtil.getLoginIdAsLong();
        Integer days = userService.getUserSignDays(loginId);

        System.out.println("连续签到天数：" + days);
        assert days >= 0 : "签到天数不能为负数";
    }

    @Test
    void testUserServiceGetHeatMap() {
        // 直接测试 Service 层方法
        long userId = StpUtil.getLoginIdAsLong();
        List<UserHeatMapVO> heatMap = userService.getUserHeatMap(userId);

        System.out.println("热力图数据大小：" + heatMap.size());
        assert heatMap != null : "热力图数据不能为空";
        assert heatMap.size() == 365 : "应该返回近 365 天的数据";

        // 验证数据结构
        UserHeatMapVO firstDay = heatMap.get(0);
        assert firstDay.getDate() != null : "日期不能为空";
        assert firstDay.getCount() >= 0 : "做题数量不能为负数";
        assert firstDay.getLevel() >= 0 && firstDay.getLevel() <= 4 : "活跃度等级应该在 0-4 之间";

        // 打印部分数据
        System.out.println("最近一天的热力图数据:");
        UserHeatMapVO lastDay = heatMap.get(heatMap.size() - 1);
        System.out.println("日期：" + lastDay.getDate());
        System.out.println("做题数：" + lastDay.getCount());
        System.out.println("活跃度等级：" + lastDay.getLevel());
    }

    @Test
    void testHeatMapDataStructure() {
        long userId = 1L; // 测试用户 ID
        List<UserHeatMapVO> heatMap = userService.getUserHeatMap(userId);

        // 验证返回的数据是连续的 365 天
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(364);

        assert heatMap.size() == 365 : "应该返回 365 天的数据";
        assert heatMap.get(0).getDate().equals(startDate) : "第一天应该是 " + startDate;
        assert heatMap.get(heatMap.size() - 1).getDate().equals(endDate) : "最后一天应该是 " + endDate;

        System.out.println("热力图数据验证通过：从 " + startDate + " 到 " + endDate);
    }
}
