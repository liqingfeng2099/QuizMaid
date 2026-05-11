package com.kanade.backend.mq;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.fastjson.JSON;
import com.kanade.backend.config.RabbitMQConfig;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.model.dto.QuestionImportMessageDTO;
import com.kanade.backend.model.entity.Question;
import com.kanade.backend.model.excel.QuestionExcelData;
import com.kanade.backend.service.ImportTaskRedisService;
import com.kanade.backend.service.QuestionService;
import com.kanade.backend.utils.NormalizationUtils;
import com.mybatisflex.core.logicdelete.LogicDeleteManager;
import com.mybatisflex.core.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class QuestionImportConsumer {

    @Resource
    private QuestionService questionService;

    @Resource
    private ImportTaskRedisService importTaskRedisService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final int BATCH_SIZE = 100;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_IMPORT, ackMode = "MANUAL", concurrency = "1")
    public void handleImport(QuestionImportMessageDTO message, Channel channel, Message amqpMessage) throws IOException {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        String taskId = message.getTaskId();
        String filePath = message.getFilePath();
        Long creatorId = message.getCreatorId();

        try {
            if (!Files.exists(Paths.get(filePath))) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件不存在");
            }

            // Layer 1: File MD5 dedup
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            String fileMd5 = DigestUtil.md5Hex(fileBytes);
            Object existingRecord = stringRedisTemplate.opsForHash().get("import:file:records", fileMd5);
            if (existingRecord != null) {
                var record = JSON.parseObject(existingRecord.toString());
                if ("COMPLETED".equals(record.getString("status"))) {
                    log.info("File already processed, skipping: md5={}, fileName={}", fileMd5, filePath);
                    importTaskRedisService.finishTask(taskId, true);
                    channel.basicAck(deliveryTag, false);
                    return;
                }
            }

            // Layer 2: User-level task lock
            RLock lock = redissonClient.getLock("lock:import:" + creatorId);
            boolean locked = lock.tryLock(0, 600, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("User has import task in progress: userId={}", creatorId);
                importTaskRedisService.updateProgress(taskId, 0, 0, 0,
                        List.of("当前有导入任务正在进行，请稍后再试"));
                importTaskRedisService.finishTask(taskId, false);
                channel.basicAck(deliveryTag, false);
                return;
            }

            try {
                // Read all Excel rows
                List<Question> allQuestions = new ArrayList<>();
                List<String> errorList = new ArrayList<>();
                int[] total = {0};

                EasyExcel.read(filePath, QuestionExcelData.class, new AnalysisEventListener<QuestionExcelData>() {
                    @Override
                    public void invoke(QuestionExcelData data, AnalysisContext context) {
                        total[0]++;
                        try {
                            validateExcelData(data);
                            Question question = convertToQuestion(data, creatorId);
                            question.setCompositeMd5(NormalizationUtils.generateCompositeMd5(question));
                            allQuestions.add(question);
                        } catch (Exception e) {
                            log.error("第{}行数据校验失败: {}", total[0], e.getMessage());
                            errorList.add("第" + total[0] + "行: " + e.getMessage());
                        }
                    }

                    @Override
                    public void doAfterAllAnalysed(AnalysisContext context) {
                        log.info("Excel解析完成，共{}行，有效{}题", total[0], allQuestions.size());
                    }
                }).sheet().doRead();

                // Update initial progress
                importTaskRedisService.updateProgress(taskId, total[0], 0, 0, errorList);

                // Layer 3: Batch preload dedup
                List<Long> resultIds = batchAddWithDedup(allQuestions, creatorId, taskId);

                // Layer 4 done (inside batchAddWithDedup)

                // Store file MD5 record
                var recordMap = new HashMap<String, String>();
                recordMap.put("userId", creatorId.toString());
                recordMap.put("fileName", Paths.get(filePath).getFileName().toString());
                recordMap.put("totalQuestions", String.valueOf(total[0]));
                recordMap.put("status", "COMPLETED");
                stringRedisTemplate.opsForHash().putAll("import:file:records:" + fileMd5, recordMap);
                stringRedisTemplate.expire("import:file:records:" + fileMd5, Duration.ofDays(7));

                boolean allSuccess = errorList.isEmpty();
                importTaskRedisService.finishTask(taskId, allSuccess);
                channel.basicAck(deliveryTag, false);

            } finally {
                lock.unlock();
            }

        } catch (Exception e) {
            log.error("导入处理失败", e);
            importTaskRedisService.finishTask(taskId, false);
            importTaskRedisService.updateProgress(taskId, 0, 0, 0, List.of("系统错误: " + e.getMessage()));
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private List<Long> batchAddWithDedup(List<Question> allQuestions, Long creatorId, String taskId) {
        if (CollUtil.isEmpty(allQuestions)) {
            return List.of();
        }

        // Layer 3: Compute MD5s and batch preload dedup
        for (Question q : allQuestions) {
            q.setQuestionMd5(cn.hutool.crypto.digest.DigestUtil.md5Hex(q.getContent()));
            q.setCompositeMd5(NormalizationUtils.generateCompositeMd5(q));
            q.setCreatorId(creatorId);
            q.setStatus(q.getStatus() == null ? 1 : q.getStatus());
        }

        List<String> questionMd5List = allQuestions.stream()
                .map(Question::getQuestionMd5)
                .distinct()
                .toList();

        // Single DB query for all existing questions by questionMd5
        List<Question> existingQuestions = LogicDeleteManager.execWithoutLogicDelete(() -> {
            QueryWrapper wrapper = QueryWrapper.create()
                    .in(Question::getQuestionMd5, questionMd5List);
            return questionService.list(wrapper);
        });

        // Build HashMap for O(1) lookup by questionMd5
        Map<String, Question> existMap = new HashMap<>();
        if (CollUtil.isNotEmpty(existingQuestions)) {
            for (Question eq : existingQuestions) {
                if (eq.getQuestionMd5() != null) {
                    existMap.put(eq.getQuestionMd5(), eq);
                }
            }
        }

        // Classify: questionMd5 for DB dedup, compositeMd5 for in-batch dedup
        List<Question> toInsert = new ArrayList<>();
        List<Long> resultIds = new ArrayList<>();
        int duplicateCount = 0;
        int restoreCount = 0;
        Set<String> batchCompositeSeen = new HashSet<>();

        for (Question q : allQuestions) {
            String qMd5 = q.getQuestionMd5();
            String cMd5 = q.getCompositeMd5();
            Question existing = existMap.get(qMd5);

            if (existing != null) {
                if (existing.getIsDeleted() == 0) {
                    duplicateCount++;
                } else {
                    LogicDeleteManager.execWithoutLogicDelete(() -> {
                        existing.setIsDeleted(0);
                        existing.setUpdateTime(java.time.LocalDateTime.now());
                        questionService.updateById(existing);
                    });
                    resultIds.add(existing.getId());
                    restoreCount++;
                }
            } else if (batchCompositeSeen.contains(cMd5)) {
                duplicateCount++;
            } else {
                toInsert.add(q);
                batchCompositeSeen.add(cMd5);
            }
        }

        // Layer 4: Batch insert with transactions
        int successCount = 0;
        List<String> batchErrors = new ArrayList<>();

        for (int i = 0; i < toInsert.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, toInsert.size());
            List<Question> batch = toInsert.subList(i, end);
            try {
                doInsertBatch(batch);
                for (Question q : batch) {
                    resultIds.add(q.getId());
                }
                successCount += batch.size();
            } catch (Exception e) {
                log.error("批次插入失败: rows {}-{}", i, end, e);
                for (Question q : batch) {
                    try {
                        doInsertSingle(q);
                        resultIds.add(q.getId());
                        successCount++;
                    } catch (Exception ex) {
                        batchErrors.add("第" + (i + batch.indexOf(q) + 1) + "题: " + ex.getMessage());
                    }
                }
            }

            // Update progress after each batch
            importTaskRedisService.updateProgress(taskId, batch.size(),
                    successCount, batch.size() - successCount + duplicateCount, batchErrors);
            batchErrors.clear();
        }

        log.info("Import complete: total={}, inserted={}, restored={}, duplicates={}",
                allQuestions.size(), successCount, restoreCount, duplicateCount);
        return resultIds;
    }

    @Transactional(rollbackFor = Exception.class)
    public void doInsertBatch(List<Question> batch) {
        questionService.saveBatch(batch);
    }

    @Transactional(rollbackFor = Exception.class)
    public void doInsertSingle(Question question) {
        questionService.save(question);
    }

    private void validateExcelData(QuestionExcelData data) {
        if (data.getType() == null || data.getType() < 1 || data.getType() > 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题型必须是1-4之间的数字");
        }
        if (StrUtil.isBlank(data.getSubject())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学科不能为空");
        }
        if (data.getDifficulty() == null || data.getDifficulty() < 1 || data.getDifficulty() > 3) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "难度必须是1-3之间的数字");
        }
        if (StrUtil.isBlank(data.getContent())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题干不能为空");
        }
        if (StrUtil.isBlank(data.getAnswer())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答案不能为空");
        }
    }

    private Question convertToQuestion(QuestionExcelData data, Long creatorId) {
        Question question = new Question();
        BeanUtils.copyProperties(data, question);
        question.setCreatorId(creatorId);
        if (question.getStatus() == null) {
            question.setStatus(1);
        }
        return question;
    }
}
