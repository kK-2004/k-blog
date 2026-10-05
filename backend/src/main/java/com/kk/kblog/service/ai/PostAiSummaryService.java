package com.kk.kblog.service.ai;

import com.kk.kblog.entity.post.AiSummaryStatus;
import com.kk.kblog.repository.post.PostRepository;
import com.kk.kblog.service.RedisService;
import com.kk.kblog.service.post.PostContentChangedEvent;
import com.kk.kblog.service.post.PostDeletedEvent;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 文章 AI 摘要：创建/编辑后提交后台任务调用 LLM 生成并落库，读取时 Redis → 数据库
 * <p>
 * 每个任务带上提交时的内容版本号，落库时按版本号条件更新，旧版本的结果直接丢弃；
 * 任务在单线程队列中串行执行，避免 LLM 接口并发限流
 */
@Slf4j
@Service
public class PostAiSummaryService {

    private static final String CACHE_KEY_PREFIX = "post:aiSummary:";
    private static final long CACHE_TTL_DAYS = 7;

    public record AiSummaryView(long postId, AiSummaryStatus status, String summary) {
    }

    private final PostRepository postRepository;
    private final AiService aiService;
    private final RedisService redisService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        var t = new Thread(r, "ai-summary");
        t.setDaemon(true);
        return t;
    });

    public PostAiSummaryService(PostRepository postRepository, AiService aiService, RedisService redisService) {
        this.postRepository = postRepository;
        this.aiService = aiService;
        this.redisService = redisService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostContentChanged(PostContentChangedEvent event) {
        evictCache(event.postId());
        submit(event.postId(), event.contentVersion());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostDeleted(PostDeletedEvent event) {
        evictCache(event.postId());
    }

    /**
     * 启动时为历史文章、失败或中断的文章补生成摘要
     */
    @EventListener(ApplicationReadyEvent.class)
    public void backfillOnStartup() {
        var posts = postRepository.findNeedingAiSummary();
        if (posts.isEmpty()) return;
        log.info("ai_summary_backfill count={}", posts.size());
        posts.forEach(p -> {
            postRepository.updateAiSummaryStatus(p.getId(), AiSummaryStatus.GENERATING, Instant.now());
            submit(p.getId(), p.getContentVersion());
        });
    }

    /**
     * 读取单篇摘要：Redis → 数据库；未生成完成时 summary 为空
     */
    public AiSummaryView getSummary(long postId) {
        var cached = readCache(postId);
        if (cached != null) {
            return new AiSummaryView(postId, AiSummaryStatus.READY, cached);
        }
        var post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("post not found: " + postId));
        var status = post.getAiSummaryStatus() == null ? AiSummaryStatus.GENERATING : post.getAiSummaryStatus();
        if (status == AiSummaryStatus.READY && post.getAiSummary() != null) {
            writeCache(postId, post.getAiSummary());
            return new AiSummaryView(postId, status, post.getAiSummary());
        }
        return new AiSummaryView(postId, status, null);
    }

    /**
     * 批量查询状态（前端轮询生成中的文章用，不返回摘要正文）
     */
    public List<AiSummaryView> getStatuses(Collection<Long> postIds) {
        var ids = postIds.stream().filter(Objects::nonNull).distinct().toList();
        return postRepository.findAllById(ids).stream()
                .map(p -> new AiSummaryView(p.getId(),
                        p.getAiSummaryStatus() == null ? AiSummaryStatus.GENERATING : p.getAiSummaryStatus(), null))
                .toList();
    }

    private void submit(long postId, long version) {
        executor.execute(() -> generate(postId, version));
    }

    private void generate(long postId, long version) {
        try {
            var post = postRepository.findById(postId).orElse(null);
            // 已删除或已有更新的版本（后面还有对应任务）→ 跳过，不浪费 LLM 调用
            if (post == null || post.getContentVersion() != version) return;

            var summary = aiService.summarize(post.getContent());
            if (postRepository.saveAiSummary(postId, version, summary, Instant.now()) > 0) {
                // 先写库再删缓存（不在这里写缓存），由读取时回填，避免旧任务把旧摘要写进缓存
                evictCache(postId);
                log.info("ai_summary_ready postId={} version={} length={}", postId, version, summary.length());
            } else {
                log.info("ai_summary_discarded postId={} version={}: content changed during generation", postId, version);
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("ai_summary_failed postId={} version={}", postId, version, e);
            postRepository.markAiSummaryFailed(postId, version, Instant.now());
        }
    }

    private String readCache(long postId) {
        try {
            return redisService.get(CACHE_KEY_PREFIX + postId, String.class);
        } catch (Exception e) {
            log.warn("ai_summary_cache_read_failed postId={}", postId, e);
            return null;
        }
    }

    private void writeCache(long postId, String summary) {
        try {
            redisService.set(CACHE_KEY_PREFIX + postId, summary, CACHE_TTL_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("ai_summary_cache_write_failed postId={}", postId, e);
        }
    }

    private void evictCache(long postId) {
        try {
            redisService.delete(CACHE_KEY_PREFIX + postId);
        } catch (Exception e) {
            log.warn("ai_summary_cache_evict_failed postId={}", postId, e);
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
