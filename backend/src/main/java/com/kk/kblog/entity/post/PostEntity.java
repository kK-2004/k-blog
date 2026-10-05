package com.kk.kblog.entity.post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "posts")
@Data
// 只更新变化的列：AI 摘要由后台线程单独更新，避免普通保存把它覆盖回旧值
@DynamicUpdate
public class PostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author", nullable = false, columnDefinition = "bigint")
    private Long authorId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "created_at", columnDefinition = "datetime(3) not null", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", columnDefinition = "datetime(3) not null")
    private Instant updatedAt;

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

    /** 内容版本号：标题/正文每次变更 +1，AI 摘要按版本号条件写入，防止旧摘要覆盖 */
    @Column(name = "content_version", columnDefinition = "bigint not null default 0")
    private long contentVersion;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_summary_status", length = 16)
    private AiSummaryStatus aiSummaryStatus;

    @Column(name = "ai_summary_updated_at", columnDefinition = "datetime(3)")
    private Instant aiSummaryUpdatedAt;

    protected PostEntity() {
    }

    public PostEntity(Long authorId, String title, String content, int views, int likes, int comments, boolean pinned) {
        this.authorId = authorId;
        this.title = title;
        this.content = content;
        this.views = views;
        this.likes = likes;
        this.comments = comments;
        this.pinned = pinned;
    }

    public Long getId() {
        return id;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @PrePersist
    protected void onCreate() {
        var now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    /**
     * 内容变更：版本号 +1，AI 摘要进入生成中（新建或标题/正文变更时调用）
     */
    public void bumpContentVersion() {
        this.contentVersion++;
        this.aiSummaryStatus = AiSummaryStatus.GENERATING;
        this.aiSummaryUpdatedAt = Instant.now();
    }

    public void touchUpdatedAt() {
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
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
}
