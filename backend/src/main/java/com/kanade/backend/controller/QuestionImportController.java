package com.kanade.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.kanade.backend.annotation.RateLimit;
import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.config.RabbitMQConfig;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.model.dto.QuestionImportMessageDTO;
import com.kanade.backend.model.enums.RateLevel;
import com.kanade.backend.service.ImportTaskRedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/question/import")
@Slf4j
public class QuestionImportController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ImportTaskRedisService importTaskRedisService;

    @Value("${import.upload-dir:./uploads}")
    private String uploadDir;

    @PostMapping("/upload")
    @RateLimit(level = RateLevel.L1_STRICT, timeWindow = 300, maxRequests = 3)
    public BaseResponse<String> uploadExcel(@RequestParam("file") MultipartFile file) {
        // 1. 校验文件
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (!originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请上传Excel文件");
        }

        try {
            String uploadDir = System.getProperty("user.dir") + "/uploads";
            // 2. 保存文件到本地临时目录
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String fileName = System.currentTimeMillis() + "_" + originalFilename;
            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            // todo 为什么保存到本地
            // 3. 生成任务ID
            String taskId = UUID.randomUUID().toString().replace("-", "");

            // 4. 在Redis中创建任务记录
            Long creatorId = StpUtil.getLoginIdAsLong();
            importTaskRedisService.createTask(taskId, creatorId);

            // 5. 发送消息
            QuestionImportMessageDTO message = new QuestionImportMessageDTO();
            message.setTaskId(taskId);
            message.setFilePath(filePath.toAbsolutePath().toString());
            message.setCreatorId(creatorId);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_IMPORT,
                    RabbitMQConfig.ROUTING_KEY_IMPORT,
                    message
            );

            return ResultUtils.success(taskId);
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }
    }

    @GetMapping("/status/{taskId}")
    public BaseResponse<Map<Object, Object>> getImportStatus(@PathVariable String taskId) {
        Map<Object, Object> status = importTaskRedisService.getTaskStatus(taskId);
        if (status.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "任务不存在或已过期");
        }
        return ResultUtils.success(status);
    }
}