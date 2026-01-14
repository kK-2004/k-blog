package com.kk.kblog.service.post;

import com.kk.kblog.dto.post.PostCommentDto;
import com.kk.kblog.dto.post.PostCommentRequests.CreateCommentRequest;
import com.kk.kblog.dto.post.PostCommentRequests.CreateReplyRequest;
import com.kk.kblog.dto.post.PostCommentRequests.LikeResponse;
import com.kk.kblog.entity.post.PostCommentEntity;
import com.kk.kblog.entity.post.PostHotCommentEntity;
import com.kk.kblog.repository.post.PostCommentRepository;
import com.kk.kblog.repository.post.PostCommentStatsJdbcRepository;
import com.kk.kblog.repository.post.PostRepository;
import com.kk.kblog.repository.post.PostStatsJdbcRepository;
import com.kk.kblog.repository.post.PostHotCommentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class PostCommentService {

    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostCommentStatsJdbcRepository postCommentStatsJdbcRepository;
    private final PostStatsJdbcRepository postStatsJdbcRepository;
    private final PostHotCommentRepository postHotCommentRepository;
    private final long hotCacheTtlSeconds;

    public PostCommentService(
            PostRepository postRepository,
            PostCommentRepository postCommentRepository,
            PostCommentStatsJdbcRepository postCommentStatsJdbcRepository,
            PostStatsJdbcRepository postStatsJdbcRepository,
            PostHotCommentRepository postHotCommentRepository,
            @Value("${app.comments.hot-cache-ttl-seconds:300}") long hotCacheTtlSeconds
    ) {
        this.postRepository = postRepository;
        this.postCommentRepository = postCommentRepository;
        this.postCommentStatsJdbcRepository = postCommentStatsJdbcRepository;
        this.postStatsJdbcRepository = postStatsJdbcRepository;
        this.postHotCommentRepository = postHotCommentRepository;
        this.hotCacheTtlSeconds = hotCacheTtlSeconds;
    }

    @Transactional
    public List<PostCommentDto> list(long postId) {
        if (!postRepository.existsById(postId)) {
            throw new EntityNotFoundException("post not found: " + postId);
        }

        var roots = postCommentRepository.findByPostIdAndParentIdIsNullOrderByIdDesc(postId);
        var rootIds = roots.stream().map(PostCommentEntity::getId).filter(id -> id != null).toList();
        var replies = rootIds.isEmpty()
                ? List.<PostCommentEntity>of()
                : postCommentRepository.findByPostIdAndParentIdInOrderByCreatedAtAscIdAsc(postId, rootIds);

        Map<Long, List<PostCommentDto.PostCommentReplyDto>> repliesByRootId = new HashMap<>();
        for (var reply : replies) {
            var parentId = reply.getParentId();
            if (parentId == null) continue;
            repliesByRootId.computeIfAbsent(parentId, ignored -> new java.util.ArrayList<>()).add(toReplyDto(reply));
        }

        // Reply ranking per root:
        // 1) likes > averageLikes first
        // 2) then by time ascending (older -> newer), tie by id
        for (var entry : repliesByRootId.entrySet()) {
            var list = entry.getValue();
            if (list.isEmpty()) continue;

            double avgLikes = list.stream().mapToInt(PostCommentDto.PostCommentReplyDto::likes).average().orElse(0.0);
            list.sort((a, b) -> {
                boolean aAbove = a.likes() > avgLikes;
                boolean bAbove = b.likes() > avgLikes;
                if (aAbove != bAbove) {
                    return aAbove ? -1 : 1;
                }
                int cmpTime = Long.compare(a.createdAt(), b.createdAt());
                if (cmpTime != 0) return cmpTime;
                return Long.compare(a.id(), b.id());
            });
        }

        long hotId = resolveHotId(postId, roots);

        long finalHotId = hotId;
        return roots.stream()
                .map(root -> new PostCommentDto(
                        root.getId(),
                        root.getUser(),
                        root.getText(),
                        root.getLikes(),
                        root.getCreatedAt() == null ? 0 : root.getCreatedAt().toEpochMilli(),
                        finalHotId > 0 && root.getId() == finalHotId,
                        repliesByRootId.getOrDefault(root.getId(), List.of())
                ))
                .toList();
    }

    @Transactional
    public PostCommentDto createComment(long postId, CreateCommentRequest request) {
        var post = postRepository.findById(postId).orElseThrow(() -> new EntityNotFoundException("post not found: " + postId));
        var user = normalizeUser(request.user());
        var text = normalizeText(request.text());

        var comment = new PostCommentEntity(post, user, text, null, null);
        var saved = postCommentRepository.save(comment);

        postStatsJdbcRepository.incrementComments(postId);
        log.info("post_comment_created postId={} commentId={} user={}", postId, saved.getId(), user);

        return new PostCommentDto(
                saved.getId(),
                saved.getUser(),
                saved.getText(),
                saved.getLikes(),
                saved.getCreatedAt() == null ? 0 : saved.getCreatedAt().toEpochMilli(),
                false,
                List.of()
        );
    }

    @Transactional
    public PostCommentDto.PostCommentReplyDto createReply(long postId, long rootCommentId, CreateReplyRequest request) {
        var root = postCommentRepository.findByIdAndPostId(rootCommentId, postId)
                .orElseThrow(() -> new EntityNotFoundException("comment not found: " + rootCommentId));
        if (root.getParentId() != null) {
            throw new IllegalArgumentException("cannot reply to non-root comment: " + rootCommentId);
        }

        var post = root.getPost();
        var user = normalizeUser(request.user());
        var text = normalizeText(request.text());
        var toUser = normalizeOptionalUser(request.toUser());

        var reply = new PostCommentEntity(post, user, text, rootCommentId, toUser);
        var saved = postCommentRepository.save(reply);

        postStatsJdbcRepository.incrementComments(postId);
        log.info("post_comment_reply_created postId={} rootId={} replyId={} user={}", postId, rootCommentId, saved.getId(), user);

        return toReplyDto(saved);
    }

    @Transactional
    public LikeResponse like(long postId, long commentId) {
        var updated = postCommentStatsJdbcRepository.incrementLikes(postId, commentId);
        if (updated == 0) {
            throw new EntityNotFoundException("comment not found: " + commentId);
        }
        var likes = postCommentStatsJdbcRepository.getLikes(postId, commentId);
        if (likes == null) {
            throw new EntityNotFoundException("comment not found: " + commentId);
        }
        return new LikeResponse(likes);
    }

    private PostCommentDto.PostCommentReplyDto toReplyDto(PostCommentEntity entity) {
        return new PostCommentDto.PostCommentReplyDto(
                entity.getId(),
                entity.getUser(),
                entity.getText(),
                entity.getLikes(),
                entity.getCreatedAt() == null ? 0 : entity.getCreatedAt().toEpochMilli(),
                entity.getToUser()
        );
    }

    private long resolveHotId(long postId, List<PostCommentEntity> roots) {
        var now = Instant.now();
        var cached = postHotCommentRepository.findById(postId).orElse(null);
        if (cached != null && cached.getExpiresAt() != null && cached.getExpiresAt().isAfter(now)) {
            var cachedId = cached.getCommentId();
            if (cachedId == null) {
                return -1L;
            }
            return cachedId;
        }

        var best = postCommentRepository
                .findTopByPostIdAndParentIdIsNullAndLikesGreaterThanOrderByLikesDescCreatedAtDescIdDesc(postId, 0)
                .orElse(null);

        var expiresAt = now.plusSeconds(Math.max(30, hotCacheTtlSeconds));
        var hotId = best == null ? null : best.getId();
        if (cached == null) {
            postHotCommentRepository.save(new PostHotCommentEntity(postId, hotId, expiresAt));
        } else {
            cached.setCommentId(hotId);
            cached.setExpiresAt(expiresAt);
            postHotCommentRepository.save(cached);
        }

        return hotId == null ? -1L : hotId;
    }

    private static String normalizeUser(String user) {
        if (user == null || user.isBlank()) {
            return "Guest";
        }
        return user.trim();
    }

    private static String normalizeOptionalUser(String user) {
        if (user == null || user.isBlank()) {
            return null;
        }
        return user.trim();
    }

    private static String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.trim();
    }
}
