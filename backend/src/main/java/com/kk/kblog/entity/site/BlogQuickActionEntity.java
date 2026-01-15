package com.kk.kblog.entity.site;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "blog_quick_actions")
public class BlogQuickActionEntity {

    @Id
    @Column(nullable = false, length = 50)
    private String id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 200)
    private String description;

    @Column(nullable = false, length = 50)
    private String icon;

    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType;

    @Column(nullable = false, length = 500)
    private String target;

    @Column(nullable = false)
    private boolean visible;

    @Column(name = "display_order", nullable = false)
    private int order;

    protected BlogQuickActionEntity() {
    }

    public BlogQuickActionEntity(String id, String title, String description, String icon, String targetType, String target, boolean visible, int order) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.targetType = targetType;
        this.target = target;
        this.visible = visible;
        this.order = order;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}

