package com.kk.kblog.config;

import com.kk.kblog.entity.site.AdminUserEntity;
import com.kk.kblog.entity.site.BlogQuickActionEntity;
import com.kk.kblog.entity.site.QuickActionEntity;
import com.kk.kblog.entity.site.SidebarMenuItemEntity;
import com.kk.kblog.repository.site.AdminUserRepository;
import com.kk.kblog.repository.site.BlogQuickActionRepository;
import com.kk.kblog.repository.site.QuickActionRepository;
import com.kk.kblog.repository.site.SidebarMenuItemRepository;
import java.util.List;
import java.time.Instant;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class DefaultDataInitializer {

    @Bean
    ApplicationRunner initSidebarMenuItems(SidebarMenuItemRepository sidebarMenuItemRepository) {
        return args -> initIfEmpty(sidebarMenuItemRepository);
    }

    @Bean
    ApplicationRunner initQuickActions(QuickActionRepository quickActionRepository) {
        return args -> initQuickActionsIfEmpty(quickActionRepository);
    }

    @Bean
    ApplicationRunner initBlogQuickActions(BlogQuickActionRepository blogQuickActionRepository) {
        return args -> initBlogQuickActionsIfEmpty(blogQuickActionRepository);
    }

    @Bean
    ApplicationRunner initAdminUser(
            AdminUserRepository adminUserRepository,
            @Value("${app.admin.username:Admin}") String adminUsername,
            @Value("${app.admin.password:admin}") String adminPassword
    ) {
        return args -> initAdminIfMissing(adminUserRepository, adminUsername, adminPassword);
    }

    @Transactional
    void initIfEmpty(SidebarMenuItemRepository sidebarMenuItemRepository) {
        if (sidebarMenuItemRepository.count() > 0) {
            return;
        }

        sidebarMenuItemRepository.saveAll(List.of(
                new SidebarMenuItemEntity("blog", "Blog", "ph-house", false, true, 1),
                new SidebarMenuItemEntity("admin", "Dashboard", "ph-squares-four", true, true, 2),
                new SidebarMenuItemEntity("settings", "Settings", "ph-gear", true, true, 3)
        ));
    }

    @Transactional
    void initQuickActionsIfEmpty(QuickActionRepository quickActionRepository) {
        if (quickActionRepository.count() > 0) {
            return;
        }

        quickActionRepository.saveAll(List.of(
                new QuickActionEntity("home", "回到首页", "打开博客首页", "ph-house", "internal", "#/blog", true, 1),
                new QuickActionEntity("dashboard", "后台管理", "打开后台面板", "ph-squares-four", "internal", "#/admin", true, 2),
                new QuickActionEntity("settings", "系统设置", "配置侧边栏与快捷入口", "ph-gear", "internal", "#/settings", true, 3)
        ));
    }

    @Transactional
    void initBlogQuickActionsIfEmpty(BlogQuickActionRepository blogQuickActionRepository) {
        if (blogQuickActionRepository.count() > 0) {
            return;
        }

        blogQuickActionRepository.saveAll(List.of(
                new BlogQuickActionEntity("blog_home", "首页", "回到博客列表", "ph-house", "internal", "#/blog", true, 1),
                new BlogQuickActionEntity("blog_login", "后台登录", "进入管理后台", "ph-lock-key", "internal", "#/login", true, 2)
        ));
    }

    @Transactional
    void initAdminIfMissing(AdminUserRepository adminUserRepository, String adminUsername, String adminPassword) {
        if (adminUserRepository.existsById(1L)) {
            return;
        }
        var now = Instant.now();
        var encoder = new BCryptPasswordEncoder();
        var admin = new AdminUserEntity(1L, adminUsername, encoder.encode(adminPassword), now, now);
        adminUserRepository.save(admin);
    }
}
