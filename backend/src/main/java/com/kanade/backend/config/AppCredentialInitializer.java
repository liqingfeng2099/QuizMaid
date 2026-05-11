package com.kanade.backend.config;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app")
@Data
@Slf4j
public class AppCredentialInitializer implements ApplicationRunner {

    private Map<String, CredentialConfig> credentials;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (credentials == null || credentials.isEmpty()) {
            log.info("No app credentials configured, skipping initialization");
            return;
        }
        for (Map.Entry<String, CredentialConfig> entry : credentials.entrySet()) {
            String appId = entry.getKey();
            CredentialConfig config = entry.getValue();
            var credMap = new java.util.HashMap<String, Object>();
            credMap.put("secretKey", config.getSecretKey());
            credMap.put("appName", config.getAppName());
            credMap.put("status", config.getStatus());
            credMap.put("createTime", LocalDateTime.now().toString());
            stringRedisTemplate.opsForHash().put("app:credentials", appId, JSON.toJSONString(credMap));
            log.info("Initialized app credential: appId={}, appName={}", appId, config.getAppName());
        }
    }

    @Data
    public static class CredentialConfig {
        private String secretKey;
        private String appName;
        private String status;
    }
}
