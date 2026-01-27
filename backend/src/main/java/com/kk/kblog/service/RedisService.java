package com.kk.kblog.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存服务
 * 封装 RedisTemplate 操作，所有 key 自动添加 k_blog: 前缀
 * 使用 StringRedisSerializer 存储 JSON 字符串，手动处理序列化
 */
@Service
public class RedisService {

    private static final String KEY_PREFIX = "k_blog:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 生成带前缀的 key
     */
    private String getKey(String key) {
        return KEY_PREFIX + key;
    }

    /**
     * 设置缓存
     */
    public void set(String key, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(getKey(key), json);
        } catch (Exception e) {
            throw new RuntimeException("Redis 序列化失败", e);
        }
    }

    /**
     * 设置缓存并指定过期时间（秒）
     */
    public void set(String key, Object value, long timeout) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(getKey(key), json, timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Redis 序列化失败", e);
        }
    }

    /**
     * 设置缓存并指定过期时间
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(getKey(key), json, timeout, unit);
        } catch (Exception e) {
            throw new RuntimeException("Redis 序列化失败", e);
        }
    }

    /**
     * 获取缓存
     */
    public <T> T get(String key, Class<T> clazz) {
        try {
            String json = (String) redisTemplate.opsForValue().get(getKey(key));
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Redis 反序列化失败", e);
        }
    }

    /**
     * 获取缓存（支持泛型，如 List<SomeDto>）
     */
    public <T> T get(String key, TypeReference<T> typeReference) {
        try {
            String json = (String) redisTemplate.opsForValue().get(getKey(key));
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            throw new RuntimeException("Redis 反序列化失败", e);
        }
    }

    /**
     * 删除缓存
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(getKey(key));
    }

    /**
     * 批量删除缓存
     */
    public Long delete(String... keys) {
        String[] prefixedKeys = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            prefixedKeys[i] = getKey(keys[i]);
        }
        return redisTemplate.delete(List.of(prefixedKeys));
    }

    /**
     * 批量删除缓存（集合）
     */
    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    /**
     * 删除匹配前缀的所有缓存
     */
    public void deleteByPrefix(String pattern) {
        Collection<String> keys = (Collection<String>) redisTemplate.keys(KEY_PREFIX + pattern + "*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 判断 key 是否存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(getKey(key));
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(getKey(key), timeout, unit);
    }

    /**
     * 设置过期时间（秒）
     */
    public Boolean expire(String key, long timeout) {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 获取过期时间（秒）
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(getKey(key));
    }

    /**
     * 检查 key 是否存在
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(getKey(key)));
    }

    // ============ ZSet 操作方法 ============

    /**
     * ZSet 添加/更新成员（带分数）
     * 如果成员已存在，则更新其分数
     */
    public Boolean zAdd(String key, String member, double score) {
        return redisTemplate.opsForZSet().add(getKey(key), member, score);
    }

    /**
     * ZSet 获取倒序排列的成员（带分数），指定范围
     */
    public Set<ZSetOperations.TypedTuple<Object>> zReverseRangeWithScores(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(getKey(key), start, end);
    }

    /**
     * ZSet 获取倒序排列的所有成员（带分数）
     */
    public Set<ZSetOperations.TypedTuple<Object>> zReverseRangeWithScores(String key) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(getKey(key), 0 , -1);
    }

    /**
     * ZSet 获取成员分数
     */
    public Double zScore(String key, String member) {
        return redisTemplate.opsForZSet().score(getKey(key), member);
    }

    /**
     * ZSet 移除成员
     */
    public Long zRemove(String key, Object... members) {
        return redisTemplate.opsForZSet().remove(getKey(key), members);
    }

    /**
     * ZSet 按分数范围删除
     */
    public Long zRemoveRangeByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().removeRangeByScore(getKey(key), min, max);
    }

    /**
     * ZSet 获取集合大小
     */
    public Long zSize(String key) {
        return redisTemplate.opsForZSet().size(getKey(key));
    }
}
