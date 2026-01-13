package com.kk.kblog.service.post;

import com.kk.kblog.dto.post.PostDto;
import com.kk.kblog.dto.post.PostRequests.CreatePostRequest;
import com.kk.kblog.dto.post.PostRequests.HotCommentReplyRequest;
import com.kk.kblog.dto.post.PostRequests.HotCommentRequest;
import com.kk.kblog.dto.post.PostRequests.PatchPostRequest;
import com.kk.kblog.dto.post.PostRequests.UpdatePostRequest;
import com.kk.kblog.entity.post.HotCommentEntity;
import com.kk.kblog.entity.post.HotCommentReplyEntity;
import com.kk.kblog.entity.post.PostEntity;
import com.kk.kblog.repository.post.PostRepository;
import com.kk.kblog.repository.post.PostStatsJdbcRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class PostService {

    private final PostRepository postRepository;
    private final PostStatsJdbcRepository postStatsJdbcRepository;

    public PostService(PostRepository postRepository, PostStatsJdbcRepository postStatsJdbcRepository) {
        this.postRepository = postRepository;
        this.postStatsJdbcRepository = postStatsJdbcRepository;
    }

    @Transactional(readOnly = true)
    public List<PostDto> listPosts() {
        var posts = postRepository.findAll(Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("id")));
        return posts.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PostDto getPost(long id) {
        var post = postRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("post not found: " + id));
        return toDto(post);
    }

    @Transactional
    public PostDto createPost(CreatePostRequest request) {
        var post = new PostEntity(
                request.author(),
                request.title(),
                request.time() == null ? "刚刚" : request.time(),
                request.content(),
                request.views() == null ? 0 : request.views(),
                request.likes() == null ? 0 : request.likes(),
                request.comments() == null ? 0 : request.comments(),
                request.pinned() != null && request.pinned()
        );
        applyHotComment(post, request.hotComment());
        var saved = postRepository.save(post);
        log.info("post_created id={} author={} title={} pinned={}", saved.getId(), saved.getAuthor(), saved.getTitle(), saved.isPinned());
        return toDto(saved);
    }

    @Transactional
    public PostDto updatePost(long id, UpdatePostRequest request) {
        var post = postRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("post not found: " + id));
        post.setAuthor(request.author());
        post.setTitle(request.title());
        post.setTime(request.time());
        post.setContent(request.content());
        post.setViews(request.views());
        post.setLikes(request.likes());
        post.setComments(request.comments());
        post.setPinned(request.pinned());
        applyHotComment(post, request.hotComment());
        return toDto(post);
    }

    @Transactional
    public PostDto patchPost(long id, PatchPostRequest request) {
        var post = postRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("post not found: " + id));
        if (request.author() != null) post.setAuthor(request.author());
        if (request.title() != null) post.setTitle(request.title());
        if (request.time() != null) post.setTime(request.time());
        if (request.content() != null) post.setContent(request.content());
        if (request.views() != null) post.setViews(request.views());
        if (request.likes() != null) post.setLikes(request.likes());
        if (request.comments() != null) post.setComments(request.comments());
        if (request.pinned() != null) post.setPinned(request.pinned());
        if (request.hotComment() != null) {
            applyHotComment(post, request.hotComment());
        }
        return toDto(post);
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

    private void applyHotComment(PostEntity post, HotCommentRequest request) {
        if (request == null) {
            post.setHotComment(null);
            return;
        }

        var hotComment = post.getHotComment();
        if (hotComment == null) {
            hotComment = new HotCommentEntity(post, request.user(), request.text(), request.likes() == null ? 0 : request.likes());
            post.setHotComment(hotComment);
        } else {
            hotComment.setUser(request.user());
            hotComment.setText(request.text());
            hotComment.setLikes(request.likes() == null ? 0 : request.likes());
        }

        hotComment.replaceReplies(mapReplies(request.replies()));
    }

    private List<HotCommentReplyEntity> mapReplies(List<HotCommentReplyRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream()
                .map(r -> new HotCommentReplyEntity(r.user(), r.text(), r.likes() == null ? 0 : r.likes()))
                .toList();
    }

    private PostDto toDto(PostEntity post) {
        var hc = post.getHotComment();
        PostDto.HotCommentDto hotCommentDto = null;
        if (hc != null) {
            var replies = hc.getReplies().stream()
                    .map(r -> new PostDto.HotCommentReplyDto(r.getId(), r.getUser(), r.getText(), r.getLikes()))
                    .toList();
            hotCommentDto = new PostDto.HotCommentDto(hc.getId(), hc.getUser(), hc.getText(), hc.getLikes(), replies);
        }

        return new PostDto(
                post.getId(),
                post.getAuthor(),
                post.getTitle(),
                post.getTime(),
                post.getContent(),
                hotCommentDto,
                post.getViews(),
                post.getLikes(),
                post.getComments(),
                post.isPinned()
        );
    }
}
