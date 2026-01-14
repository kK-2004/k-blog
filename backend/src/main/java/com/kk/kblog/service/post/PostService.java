package com.kk.kblog.service.post;

import com.kk.kblog.dto.post.PostDto;
import com.kk.kblog.dto.post.PostRequests.CreatePostRequest;
import com.kk.kblog.dto.post.PostRequests.PatchPostRequest;
import com.kk.kblog.dto.post.PostRequests.UpdatePostRequest;
import com.kk.kblog.entity.post.PostEntity;
import com.kk.kblog.repository.post.PostRepository;
import com.kk.kblog.repository.post.PostStatsJdbcRepository;
import com.kk.kblog.repository.site.AdminUserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PostService {

    private static final long SINGLE_ADMIN_ID = 1L;

    private final PostRepository postRepository;
    private final PostStatsJdbcRepository postStatsJdbcRepository;
    private final AdminUserRepository adminUserRepository;

    public PostService(PostRepository postRepository, PostStatsJdbcRepository postStatsJdbcRepository, AdminUserRepository adminUserRepository) {
        this.postRepository = postRepository;
        this.postStatsJdbcRepository = postStatsJdbcRepository;
        this.adminUserRepository = adminUserRepository;
    }

    @Transactional(readOnly = true)
    public List<PostDto> listPosts() {
        var posts = postRepository.findAll(Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("id")));
        var authorNameById = preloadAuthorNames(posts);
        return posts.stream().map(p -> toDto(p, authorNameById)).toList();
    }

    @Transactional(readOnly = true)
    public PostDto getPost(long id) {
        var post = postRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("post not found: " + id));
        return toDto(post, preloadAuthorNames(List.of(post)));
    }

    @Transactional
    public PostDto createPost(CreatePostRequest request) {
        var post = new PostEntity(
                SINGLE_ADMIN_ID,
                request.title(),
                request.content(),
                request.views() == null ? 0 : request.views(),
                request.likes() == null ? 0 : request.likes(),
                request.comments() == null ? 0 : request.comments(),
                request.pinned() != null && request.pinned()
        );
        var saved = postRepository.save(post);
        log.info("post_created id={} authorId={} title={} pinned={}", saved.getId(), saved.getAuthorId(), saved.getTitle(), saved.isPinned());
        return toDto(saved, preloadAuthorNames(List.of(saved)));
    }

    @Transactional
    public PostDto updatePost(long id, UpdatePostRequest request) {
        var post = postRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("post not found: " + id));
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setViews(request.views());
        post.setLikes(request.likes());
        post.setComments(request.comments());
        post.setPinned(request.pinned());
        return toDto(post, preloadAuthorNames(List.of(post)));
    }

    @Transactional
    public PostDto patchPost(long id, PatchPostRequest request) {
        var post = postRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("post not found: " + id));
        if (request.title() != null) post.setTitle(request.title());
        if (request.content() != null) post.setContent(request.content());
        if (request.views() != null) post.setViews(request.views());
        if (request.likes() != null) post.setLikes(request.likes());
        if (request.comments() != null) post.setComments(request.comments());
        if (request.pinned() != null) post.setPinned(request.pinned());
        return toDto(post, preloadAuthorNames(List.of(post)));
    }

    @Transactional
    public void deletePost(long id) {
        if (!postRepository.existsById(id)) {
            throw new EntityNotFoundException("post not found: " + id);
        }
        postRepository.deleteById(id);
    }

    @Transactional
    public PostDto incrementViews(long id) {
        var updated = postStatsJdbcRepository.incrementViews(id);
        if (updated == 0) {
            throw new EntityNotFoundException("post not found: " + id);
        }
        return getPost(id);
    }

    @Transactional
    public PostDto incrementLikes(long id) {
        var updated = postStatsJdbcRepository.incrementLikes(id);
        if (updated == 0) {
            throw new EntityNotFoundException("post not found: " + id);
        }
        log.info("post_like_incremented id={}", id);
        return getPost(id);
    }

    @Transactional
    public PostDto incrementComments(long id) {
        var updated = postStatsJdbcRepository.incrementComments(id);
        if (updated == 0) {
            throw new EntityNotFoundException("post not found: " + id);
        }
        log.info("post_comment_incremented id={}", id);
        return getPost(id);
    }

    private PostDto toDto(PostEntity post, Map<Long, String> authorNameById) {
        var authorName = authorNameById.getOrDefault(post.getAuthorId(), "Admin");
        return new PostDto(
                post.getId(),
                authorName,
                post.getTitle(),
                post.getCreatedAt() == null ? null : post.getCreatedAt().toEpochMilli(),
                post.getUpdatedAt() == null ? null : post.getUpdatedAt().toEpochMilli(),
                post.getContent(),
                post.getViews(),
                post.getLikes(),
                post.getComments(),
                post.isPinned()
        );
    }

    private Map<Long, String> preloadAuthorNames(List<PostEntity> posts) {
        var authorIds = posts.stream()
                .map(PostEntity::getAuthorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (authorIds.isEmpty()) return Map.of();

        var map = new HashMap<Long, String>(authorIds.size());
        adminUserRepository.findAllById(authorIds).forEach(u -> map.put(u.getId(), u.getUsername()));
        return map;
    }
}
