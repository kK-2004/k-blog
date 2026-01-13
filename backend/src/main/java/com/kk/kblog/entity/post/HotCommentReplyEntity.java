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

@Entity
@Table(name = "hot_comment_replies")
public class HotCommentReplyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hot_comment_id", nullable = false)
    private HotCommentEntity hotComment;

    @Column(name = "user_name", nullable = false, length = 100)
    private String user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false)
    private int likes;

    protected HotCommentReplyEntity() {
    }

    public HotCommentReplyEntity(String user, String text, int likes) {
        this.user = user;
        this.text = text;
        this.likes = likes;
    }

    public Long getId() {
        return id;
    }

    public HotCommentEntity getHotComment() {
        return hotComment;
    }

    public void setHotComment(HotCommentEntity hotComment) {
        this.hotComment = hotComment;
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
}
