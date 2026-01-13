package com.kk.kblog.entity.post;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "posts")
public class PostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String author;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "time_text", nullable = false, length = 50)
    private String time;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false)
    private int views;

    @Column(nullable = false)
    private int likes;

    @Column(nullable = false)
    private int comments;

    @Column(nullable = false)
    private boolean pinned;

    @OneToOne(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private HotCommentEntity hotComment;

    protected PostEntity() {
    }

    public PostEntity(String author, String title, String time, String content, int views, int likes, int comments, boolean pinned) {
        this.author = author;
        this.title = title;
        this.time = time;
        this.content = content;
        this.views = views;
        this.likes = likes;
        this.comments = comments;
        this.pinned = pinned;
    }

    public Long getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getComments() {
        return comments;
    }

    public void setComments(int comments) {
        this.comments = comments;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public HotCommentEntity getHotComment() {
        return hotComment;
    }

    public void setHotComment(HotCommentEntity hotComment) {
        if (hotComment != null) {
            hotComment.setPost(this);
        }
        this.hotComment = hotComment;
    }
}
