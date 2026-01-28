package com.kk.kblog.entity.post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
        name = "post_comments"
)
public class PostCommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;

    @Column(nullable = false, length = 50)
    private String user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false)
    private int likes;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "to_user", length = 50)
    private String toUser;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "datetime(3) not null", updatable = false)
    private Instant createdAt;

    protected PostCommentEntity() {
    }

    public PostCommentEntity(PostEntity post, String user, String text, Long parentId, String toUser) {
        this.post = post;
        this.user = user;
        this.text = text;
        this.parentId = parentId;
        this.toUser = toUser;
        this.likes = 0;
    }

    public Long getId() {
        return id;
    }

    public PostEntity getPost() {
        return post;
    }

    public String getUser() {
        return user;
    }

    public String getText() {
        return text;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getToUser() {
        return toUser;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
