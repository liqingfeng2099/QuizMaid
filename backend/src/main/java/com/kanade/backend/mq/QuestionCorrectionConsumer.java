package com.kanade.backend.mq;

import cn.hutool.json.JSONUtil;
import com.kanade.backend.ai.AiService;
import com.kanade.backend.ai.AiServiceFactory;
import com.kanade.backend.ai.model.JudgeResult;
import com.kanade.backend.config.RabbitMQConfig;
import com.kanade.backend.model.dto.QuestionCorrectionMessageDTO;
import com.kanade.backend.model.entity.Question;
import com.kanade.backend.model.entity.Useranswerdetail;
import com.kanade.backend.model.entity.Userexamrecord;
import com.kanade.backend.model.enums.TaskEnum;
import com.kanade.backend.service.QuestionService;
import com.kanade.backend.service.UseranswerdetailService;
import com.kanade.backend.service.UserexamrecordService;
import com.mybatisflex.core.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Component
@Slf4j
public class QuestionCorrectionConsumer {

    @Resource
    private UseranswerdetailService useranswerdetailService;

    @Resource
    private UserexamrecordService userexamrecordService;

    @Resource
    private QuestionService questionService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_CORRECTION, ackMode = "MANUAL", concurrency = "1")
    public void handleCorrection(QuestionCorrectionMessageDTO message, Channel channel, Message amqpMessage) throws IOException {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        Long detailId = message.getDetailId();

        try {
            Useranswerdetail detail = useranswerdetailService.getById(detailId);
            if (detail == null) {
                log.error("Answer detail not found: detailId={}", detailId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            Question question = questionService.getById(detail.getQuestionId());
            if (question == null) {
                log.error("Question not found: questionId={}", detail.getQuestionId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            String userMessage = buildJudgePrompt(question, detail);

            AiService aiService = AiServiceFactory.getAiService(TaskEnum.JUDGE);
            JudgeResult result = aiService.generateQuestionJudge(userMessage);

            detail.setActualScore(result.getTotalScore() != null ? result.getTotalScore() : 0);
            if (result.getTotalScore() != null && detail.getQuestionScore() != null
                    && result.getTotalScore() >= detail.getQuestionScore()) {
                detail.setCorrectStatus(1);
            } else {
                detail.setCorrectStatus(2);
            }
            detail.setAiReviewMsg(JSONUtil.toJsonStr(result));
            useranswerdetailService.updateById(detail);

            stringRedisTemplate.opsForValue().set(
                    "correction:" + detailId,
                    JSONUtil.toJsonStr(result),
                    Duration.ofDays(30));

            tryFinishExamRecord(detail.getRecordId());

            channel.basicAck(deliveryTag, false);
            log.info("Correction done: detailId={}, score={}/{}", detailId, result.getTotalScore(), detail.getQuestionScore());

        } catch (Exception e) {
            log.error("Correction failed: detailId={}", detailId, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private String buildJudgePrompt(Question question, Useranswerdetail detail) {
        return String.format("""
                题目内容: %s
                题目总分: %d
                标准答案: %s
                学生答案: %s
                """,
                question.getContent(),
                detail.getQuestionScore() != null ? detail.getQuestionScore() : 0,
                question.getAnswer(),
                detail.getUserAnswer());
    }

    private void tryFinishExamRecord(Long recordId) {
        List<Useranswerdetail> details = useranswerdetailService.list(
                QueryWrapper.create().eq("recordId", recordId));

        boolean allGraded = details.stream()
                .noneMatch(d -> d.getCorrectStatus() == null || d.getCorrectStatus() == 0);

        if (!allGraded) {
            return;
        }

        int totalScore = details.stream()
                .mapToInt(d -> d.getActualScore() != null ? d.getActualScore() : 0)
                .sum();

        Userexamrecord record = userexamrecordService.getById(recordId);
        if (record != null) {
            record.setUserScore(totalScore);
            record.setStatus(2);
            userexamrecordService.updateById(record);
            log.info("Exam record all graded: recordId={}, totalScore={}", recordId, totalScore);
        }
    }
}
