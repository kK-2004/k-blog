package com.kk.kblog.config;

import com.kk.kblog.entity.site.AdminUserEntity;
import com.kk.kblog.entity.site.SidebarMenuItemEntity;
import com.kk.kblog.repository.site.AdminUserRepository;
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
