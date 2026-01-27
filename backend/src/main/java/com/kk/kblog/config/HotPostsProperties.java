package com.kk.kblog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 热门文章配置属性
 */
@Component
@ConfigurationProperties(prefix = "app.hot-posts")
public class HotPostsProperties {

    /**
     * 是否启用热门文章功能
     */
    private boolean enabled = true;

    /**
     * 时间窗口（分钟）
     * 在此时间窗口内访问过的文章会被记录
     */
    private int windowMinutes = 10;

    /**
     * 榜单显示数量
     */
    private int listSize = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getWindowMinutes() {
        return windowMinutes;
    }

    public void setWindowMinutes(int windowMinutes) {
        this.windowMinutes = windowMinutes;
    }

    public int getListSize() {
        return listSize;
    }

    public void setListSize(int listSize) {
        this.listSize = listSize;
    }
}
