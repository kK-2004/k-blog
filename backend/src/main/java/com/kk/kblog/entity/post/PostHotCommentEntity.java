package com.kk.kblog.entity.post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "post_hot_comments")
public class PostHotCommentEntity {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "expires_at", columnDefinition = "datetime(3) not null")
    private Instant expiresAt;

    protected PostHotCommentEntity() {
    }

    public PostHotCommentEntity(Long postId, Long commentId, Instant expiresAt) {
        this.postId = postId;
        this.commentId = commentId;
        this.expiresAt = expiresAt;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}

