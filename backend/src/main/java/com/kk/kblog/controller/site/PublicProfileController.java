package com.kk.kblog.controller.site;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kk.kblog.dto.site.AdminDtos;
import com.kk.kblog.dto.site.PublicProfileDto;
import com.kk.kblog.repository.site.AdminUserRepository;
import com.kk.kblog.service.RedisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/site")
public class PublicProfileController {
    private static final long SINGLE_ADMIN_ID = 1L;
    private static final AdminDtos.ProfileVisibility DEFAULT_VISIBILITY = new AdminDtos.ProfileVisibility(
            "public",
            "private",
            "private",
            "private",
            "private",
            "private",
            "public",
            "public"
    );

    private final AdminUserRepository adminUserRepository;
    private final ObjectMapper objectMapper;
    private final RedisService redisService;

    private static final String CACHE_KEY = "publicProfile:profile";

    public PublicProfileController(AdminUserRepository adminUserRepository, ObjectMapper objectMapper, RedisService redisService) {
        this.adminUserRepository = adminUserRepository;
        this.objectMapper = objectMapper;
        this.redisService = redisService;
    }

    @GetMapping("/profile")
    public PublicProfileDto profile() {
        // 先从缓存获取
        PublicProfileDto cached = redisService.get(CACHE_KEY, PublicProfileDto.class);
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，查询数据库
        var admin = adminUserRepository.findById(SINGLE_ADMIN_ID).orElse(null);
        PublicProfileDto result;
        if (admin == null) {
            result = new PublicProfileDto("Admin", null, null, null, null, null, null, null, null, null);
        } else {
            var visibility = readVisibility(admin.getProfileVisibilityJson());
            result = new PublicProfileDto(
                    admin.getUsername(),
                    admin.getAvatarUrl(),
                    "public".equals(visibility.gender()) ? admin.getGender() : null,
                    "public".equals(visibility.age()) ? admin.getAge() : null,
                    "public".equals(visibility.email()) ? admin.getEmail() : null,
                    "public".equals(visibility.phone()) ? admin.getPhone() : null,
                    "public".equals(visibility.qq()) ? admin.getQq() : null,
                    "public".equals(visibility.wechat()) ? admin.getWechat() : null,
                    "public".equals(visibility.github()) ? admin.getGithub() : null,
                    "public".equals(visibility.gitee()) ? admin.getGitee() : null
            );
        }

        // 写入缓存，5分钟过期
        redisService.set(CACHE_KEY, result, 5, TimeUnit.MINUTES);
        return result;
    }

    private AdminDtos.ProfileVisibility readVisibility(String json) {
        if (json == null || json.isBlank()) return DEFAULT_VISIBILITY;
        try {
            var v = objectMapper.readValue(json, AdminDtos.ProfileVisibility.class);
            return normalizeVisibility(v);
        } catch (Exception e) {
            return DEFAULT_VISIBILITY;
        }
    }

    private AdminDtos.ProfileVisibility normalizeVisibility(AdminDtos.ProfileVisibility v) {
        if (v == null) return DEFAULT_VISIBILITY;
        return new AdminDtos.ProfileVisibility(
                normalizeVisValue(v.gender(), DEFAULT_VISIBILITY.gender()),
                normalizeVisValue(v.age(), DEFAULT_VISIBILITY.age()),
                normalizeVisValue(v.email(), DEFAULT_VISIBILITY.email()),
                normalizeVisValue(v.phone(), DEFAULT_VISIBILITY.phone()),
                normalizeVisValue(v.qq(), DEFAULT_VISIBILITY.qq()),
                normalizeVisValue(v.wechat(), DEFAULT_VISIBILITY.wechat()),
                normalizeVisValue(v.github(), DEFAULT_VISIBILITY.github()),
                normalizeVisValue(v.gitee(), DEFAULT_VISIBILITY.gitee())
        );
    }

    private String normalizeVisValue(String value, String fallback) {
        if (value == null) return fallback;
        var v = value.trim();
        if ("public".equals(v) || "private".equals(v)) return v;
        return fallback;
    }

}

