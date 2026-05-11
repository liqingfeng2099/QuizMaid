package com.kanade.backend.config;

import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    /**
     * JSON-serialized RedisTemplate named "jsonRedisTemplate" to avoid conflict
     * with Spring Boot's default "redisTemplate" and Sa-Token's Redis integration.
     */
    @Bean
    public RedisTemplate<String, Object> jsonRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        RedisSerializer<String> stringSerializer = RedisSerializer.string();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        FastJsonRedisSerializer<Object> jsonSerializer = new FastJsonRedisSerializer<>(Object.class);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
