package com.kk.kblog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置
 * <p>
 * 使用 StringRedisSerializer 存储 JSON 字符串，避免类型头问题
 */
@Configuration
public class RedisConfig {

    /**
     * 配置 RedisTemplate
     * 使用 String 序列化，值以 JSON 字符串形式存储
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        // key 使用 String 序列化
        template.setKeySerializer(new StringRedisSerializer());
        // value 使用 String 序列化（存储 JSON 字符串）
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * ObjectMapper 用于 JSON 序列化/反序列化
     */
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .build()
                .registerModules(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
