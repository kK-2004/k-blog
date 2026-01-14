package com.kk.kblog.controller.site;

import com.kk.kblog.dto.site.PublicProfileDto;
import com.kk.kblog.repository.site.AdminUserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/site")
public class PublicProfileController {
    private static final long SINGLE_ADMIN_ID = 1L;

    private final AdminUserRepository adminUserRepository;

    public PublicProfileController(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @GetMapping("/profile")
    public PublicProfileDto profile() {
        var admin = adminUserRepository.findById(SINGLE_ADMIN_ID).orElse(null);
        if (admin == null) return new PublicProfileDto("Admin", null);
        return new PublicProfileDto(admin.getUsername(), admin.getAvatarUrl());
    }
}

