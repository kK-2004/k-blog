package com.kk.kblog.controller.admin;

import com.kk.kblog.dto.site.AdminDtos.AdminMeDto;
import com.kk.kblog.dto.site.AdminDtos.UpdatePasswordRequest;
import com.kk.kblog.dto.site.AdminDtos.UpdateProfileRequest;
import com.kk.kblog.service.RedisService;
import com.kk.kblog.service.admin.AdminAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminAuthService adminAuthService;
    private final RedisService redisService;

    private static final String CACHE_KEY_ME = "adminMe:me";
    private static final String CACHE_KEY_PROFILE = "publicProfile:profile";

    public AdminController(AdminAuthService adminAuthService, RedisService redisService) {
        this.adminAuthService = adminAuthService;
        this.redisService = redisService;
    }

    @GetMapping("/me")
    public AdminMeDto me() {
        // 先从缓存获取
        AdminMeDto cached = redisService.get(CACHE_KEY_ME, AdminMeDto.class);
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，查询数据库
        AdminMeDto result = adminAuthService.me();

        // 写入缓存，1分钟过期
        redisService.set(CACHE_KEY_ME, result, 1, TimeUnit.MINUTES);
        return result;
    }

    @PutMapping("/profile")
    public AdminMeDto updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        AdminMeDto result = adminAuthService.updateProfile(request);

        // 清除相关缓存
        redisService.delete(CACHE_KEY_ME, CACHE_KEY_PROFILE);

        return result;
    }

    @PutMapping("/password")
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        adminAuthService.updatePassword(request);
        return ResponseEntity.noContent().build();
    }
}
