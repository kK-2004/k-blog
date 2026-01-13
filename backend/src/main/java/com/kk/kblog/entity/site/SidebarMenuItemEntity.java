package com.kk.kblog.entity.site;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sidebar_menu_items")
public class SidebarMenuItemEntity {

    @Id
    @Column(nullable = false, length = 50)
    private String id;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false, length = 50)
    private String icon;

    @Column(name = "requires_auth", nullable = false)
    private boolean requiresAuth;

    @Column(nullable = false)
    private boolean visible;

    @Column(name = "display_order", nullable = false)
    private int order;

    protected SidebarMenuItemEntity() {
    }

    public SidebarMenuItemEntity(String id, String label, String icon, boolean requiresAuth, boolean visible, int order) {
        this.id = id;
        this.label = label;
        this.icon = icon;
        this.requiresAuth = requiresAuth;
        this.visible = visible;
        this.order = order;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public boolean isRequiresAuth() {
        return requiresAuth;
    }

    public void setRequiresAuth(boolean requiresAuth) {
        this.requiresAuth = requiresAuth;
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
