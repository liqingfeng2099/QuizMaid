package com.kanade.backend.mq;

import com.kanade.backend.assembly.AssemblyOrchestrator;
import com.kanade.backend.mapper.ExamPaperMapper;
import com.kanade.backend.mapper.PaperquestionMapper;
import com.kanade.backend.mapper.StrategyWeightMapper;
import com.kanade.backend.model.dto.AssemblyRequestDTO;
import com.kanade.backend.model.entity.ExamPaper;
import com.kanade.backend.model.entity.Paperquestion;
import com.kanade.backend.model.entity.PaperStrategy;
import com.kanade.backend.model.entity.StrategyWeight;
import com.kanade.backend.model.vo.AssemblyResultVO;
import com.kanade.backend.service.NotificationService;
import com.kanade.backend.service.PaperStrategyService;
import com.mybatisflex.core.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LargeAssemblyConsumer {

    private final NotificationService notificationService;
    private final StringRedisTemplate stringRedisTemplate;
    private final AssemblyOrchestrator assemblyOrchestrator;
    private final PaperStrategyService paperStrategyService;
    private final StrategyWeightMapper strategyWeightMapper;
    private final ExamPaperMapper examPaperMapper;
    private final PaperquestionMapper paperquestionMapper;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = LargeAssemblyProducer.QUEUE_ASSEMBLY, durable = "true"),
            exchange = @Exchange(value = LargeAssemblyProducer.EXCHANGE_ASSEMBLY, type = "direct"),
            key = LargeAssemblyProducer.ROUTING_KEY_ASSEMBLY
    ), ackMode = "MANUAL", concurrency = "2")
    @Transactional
    public void onMessage(LargeAssemblyMessageDTO message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        String progressKey = "assembly:progress:" + message.getTaskId();
        log.info("[异步组卷] 收到任务: taskId={} userId={} subject={} algorithm={}",
                message.getTaskId(), message.getUserId(), message.getSubject(), message.getAlgorithmType());

        try {
            updateProgress(progressKey, 10, "正在加载组卷策略...");

            // 1. 加载策略和权重
            PaperStrategy strategy = paperStrategyService.getById(message.getStrategyId());
            if (strategy == null) {
                throw new RuntimeException("组卷策略不存在: " + message.getStrategyId());
            }
            List<StrategyWeight> weights = strategyWeightMapper.selectListByQuery(
                    QueryWrapper.create().eq("strategyId", message.getStrategyId()));

            updateProgress(progressKey, 25, "正在检索候选题库...");

            // 2. 构建请求
            AssemblyRequestDTO request = new AssemblyRequestDTO();
            request.setStrategyId(message.getStrategyId());
            request.setSubject(message.getSubject());
            request.setPaperName(message.getPaperName());
            request.setPaperStatus(message.getPaperStatus() != null ? message.getPaperStatus() : 0);

            updateProgress(progressKey, 40, "正在执行" +
                    ("GENETIC".equalsIgnoreCase(message.getAlgorithmType()) ? "遗传" : "贪心") + "组卷算法...");

            // 3. 执行组卷算法
            AssemblyResultVO result;
            if ("GENETIC".equalsIgnoreCase(message.getAlgorithmType())) {
                result = assemblyOrchestrator.geneticAssemble(request, strategy, weights, message.getUserId());
            } else {
                result = assemblyOrchestrator.greedyAssemble(request, strategy, weights, message.getUserId());
            }

            if (result.getTotalQuestions() == 0) {
                throw new RuntimeException("组卷结果为空：" +
                        (result.getDimensionResults() != null ? result.getDimensionResults().get("error") : "候选题目不足"));
            }

            updateProgress(progressKey, 70, "正在保存试卷...");

            // 4. 创建试卷
            ExamPaper paper = ExamPaper.builder()
                    .paperName(message.getPaperName() != null ? message.getPaperName() : strategy.getStrategyName() + "-组卷")
                    .subject(message.getSubject() != null ? message.getSubject() : "综合")
                    .totalScore(result.getActualTotalScore())
                    .creatorId(message.getUserId())
                    .status(message.getPaperStatus() != null ? message.getPaperStatus() : 0)
                    .strategyId(strategy.getId())
                    .paperType(2) // 自动组卷
                    .difficultyRate(BigDecimal.valueOf(strategy.getDifficultyAvg() != null ? strategy.getDifficultyAvg() : 3))
                    .duration(strategy.getDuration())
                    .exportStatus(0)
                    .build();
            examPaperMapper.insert(paper);

            // 5. 关联题目
            int sort = 1;
            int totalScore = 0;
            if (result.getQuestions() != null) {
                for (AssemblyResultVO.QuestionScoreVO qs : result.getQuestions()) {
                    Paperquestion pq = new Paperquestion();
                    pq.setPaperId(paper.getId());
                    pq.setQuestionId(qs.getQuestionId());
                    pq.setQuestionScore(qs.getScore() != null ? qs.getScore() : 10);
                    pq.setSort(sort++);
                    pq.setIsAutoAdd(1);
                    pq.setCreateTime(java.time.LocalDateTime.now());
                    paperquestionMapper.insert(pq);
                    totalScore += (qs.getScore() != null ? qs.getScore() : 10);
                }
            }

            // 6. 更新总分
            paper.setTotalScore(totalScore);
            examPaperMapper.update(paper);

            updateProgress(progressKey, 100, "组卷完成");

            String degradeInfo = "";
            if (result.getDegradeHints() != null && !result.getDegradeHints().isEmpty()) {
                degradeInfo = "（含" + result.getDegradeHints().size() + "项降级适配）";
            }

            notificationService.sendNotification(message.getUserId(),
                    "组卷完成",
                    "试卷《" + paper.getPaperName() + "》异步组卷完成，共" +
                            result.getTotalQuestions() + "题，总分" + totalScore + "分" + degradeInfo,
                    1, "/paper");

            channel.basicAck(tag, false);
            log.info("[异步组卷] 任务完成: taskId={} paperId={} questions={} score={}",
                    message.getTaskId(), paper.getId(), result.getTotalQuestions(), totalScore);
        } catch (Exception e) {
            log.error("[异步组卷] 任务失败: taskId={}", message.getTaskId(), e);
            updateProgress(progressKey, -1, "组卷失败：" + e.getMessage());
            notificationService.sendNotification(message.getUserId(),
                    "组卷失败",
                    "试卷组卷失败：" + e.getMessage() + "，请重试或切换手动组卷",
                    1, "/paper/assembly");
            try {
                channel.basicNack(tag, false, false);
            } catch (IOException ioException) {
                log.error("[异步组卷] NACK失败", ioException);
            }
        }
    }

    private void updateProgress(String key, int percent, String status) {
        stringRedisTemplate.opsForHash().put(key, "percent", String.valueOf(percent));
        stringRedisTemplate.opsForHash().put(key, "status", status);
        stringRedisTemplate.expire(key, Duration.ofHours(1));
    }
}
