package com.kk.kblog.service;

import com.kk.kblog.config.HotPostsProperties;
import com.kk.kblog.dto.post.HotPostDto;
import com.kk.kblog.entity.post.PostEntity;
import com.kk.kblog.repository.post.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 热门文章服务
 * 使用 Redis ZSet 记录最近访问过的文章
 * Score = 过期时间戳（当前时间 + 时间窗口）
 */
@Slf4j
@Service
public class HotPostsService {

    private static final String HOT_POSTS_KEY = "hot_posts";

    private final RedisService redisService;
    private final PostRepository postRepository;
    private final HotPostsProperties properties;

    public HotPostsService(RedisService redisService,
                          PostRepository postRepository,
                          HotPostsProperties properties) {
        this.redisService = redisService;
        this.postRepository = postRepository;
        this.properties = properties;
    }

    /**
     * 记录文章访问
     * 将该文章的 score 设置为：当前时间 + 时间窗口
     *
     * @param postId 文章ID
     */
    public void recordPostView(Long postId) {
        if (!properties.isEnabled() || postId == null) {
            return;
        }

        String member = postId.toString();
        // Score = 过期时间戳（当前时间 + 时间窗口）
        long expireTime = System.currentTimeMillis() + properties.getWindowMinutes() * 60 * 1000;
        double score = (double) expireTime;

        redisService.zAdd(HOT_POSTS_KEY, member, score);

        log.debug("Recorded view for post {}, expire at {}", postId, expireTime);
    }

    /**
     * 获取热门文章榜单
     * 返回最近时间窗口内访问过的文章，按最近访问时间排序
     *
     * @return 热门文章列表
     */
    public List<HotPostDto> getHotPosts() {
        if (!properties.isEnabled()) {
            return List.of();
        }

        // 获取所有成员（带分数）
        Set<ZSetOperations.TypedTuple<Object>> tuples = redisService.zReverseRangeWithScores(HOT_POSTS_KEY, 0, -1);
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }

        long currentTime = System.currentTimeMillis();
        List<HotPostDto> result = new ArrayList<>();
        int maxSize = properties.getListSize();

        // 遍历并过滤过期记录
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            Double score = tuple.getScore();
            if (score == null || score < currentTime) {
                // 已过期的记录
                continue;
            }

            Long postId = Long.parseLong((String) tuple.getValue());
            // 从数据库获取文章标题
            postRepository.findById(postId).ifPresent(post -> {
                result.add(new HotPostDto(postId, post.getTitle(), score.longValue()));
            });

            // 只返回前 N 条
            if (result.size() >= maxSize) {
                break;
            }
        }

        return result;
    }

    /**
     * 清理过期的记录（可选，由定时任务调用）
     */
    public void cleanExpiredPosts() {
        long currentTime = System.currentTimeMillis();
        // 删除 score < 当前时间的记录
        redisService.zRemoveRangeByScore(HOT_POSTS_KEY, 0, currentTime);
        log.debug("Cleaned expired hot posts");
    }

    /**
     * 定时清理过期记录
     * 每天 0点30分 执行
     */
    @Scheduled(cron = "0 30 0 * * ?")
    public void scheduledCleanExpiredPosts() {
        if (!properties.isEnabled()) {
            return;
        }
        cleanExpiredPosts();
        log.info("Scheduled cleanup of expired hot posts completed");
    }
}
