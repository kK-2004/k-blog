package com.kk.kblog.controller.post;

import com.kk.kblog.dto.post.PostCommentDto;
import com.kk.kblog.dto.post.PostCommentRequests.CreateCommentRequest;
import com.kk.kblog.dto.post.PostCommentRequests.CreateReplyRequest;
import com.kk.kblog.dto.post.PostCommentRequests.LikeResponse;
import com.kk.kblog.service.post.PostCommentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class PostCommentController {

    private final PostCommentService postCommentService;

    public PostCommentController(PostCommentService postCommentService) {
        this.postCommentService = postCommentService;
    }

    @GetMapping
    public List<PostCommentDto> list(@PathVariable long postId) {
        return postCommentService.list(postId);
    }

    @PostMapping
    public ResponseEntity<PostCommentDto> create(@PathVariable long postId, @Valid @RequestBody CreateCommentRequest request) {
        var created = postCommentService.createComment(postId, request);
        return ResponseEntity.created(URI.create("/api/posts/" + postId + "/comments/" + created.id())).body(created);
    }

    @PostMapping("/{commentId}/replies")
    public ResponseEntity<PostCommentDto.PostCommentReplyDto> createReply(
            @PathVariable long postId,
            @PathVariable long commentId,
            @Valid @RequestBody CreateReplyRequest request
    ) {
        var created = postCommentService.createReply(postId, commentId, request);
        return ResponseEntity.created(URI.create("/api/posts/" + postId + "/comments/" + commentId + "/replies/" + created.id())).body(created);
    }

    @PostMapping("/{commentId}/likes")
    public LikeResponse like(@PathVariable long postId, @PathVariable long commentId) {
        return postCommentService.like(postId, commentId);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable long postId, @PathVariable long commentId) {
        postCommentService.delete(postId, commentId);
        return ResponseEntity.noContent().build();
    }
}
