package com.kk.kblog.entity.post;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hot_comments")
public class HotCommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false, unique = true)
    private PostEntity post;

    @Column(name = "user_name", nullable = false, length = 100)
    private String user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false)
    private int likes;

    @OneToMany(mappedBy = "hotComment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<HotCommentReplyEntity> replies = new ArrayList<>();

    protected HotCommentEntity() {
    }

    public HotCommentEntity(PostEntity post, String user, String text, int likes) {
        this.post = post;
        this.user = user;
        this.text = text;
        this.likes = likes;
    }

    public Long getId() {
        return id;
    }

    public PostEntity getPost() {
        return post;
    }

    public void setPost(PostEntity post) {
        this.post = post;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public List<HotCommentReplyEntity> getReplies() {
        return replies;
    }

    public void replaceReplies(List<HotCommentReplyEntity> newReplies) {
        this.replies.clear();
        if (newReplies == null) {
            return;
        }
        for (var reply : newReplies) {
            reply.setHotComment(this);
            this.replies.add(reply);
        }
    }
}
