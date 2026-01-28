package com.kk.kblog.service.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kk.kblog.dto.site.AdminDtos.AdminMeDto;
import com.kk.kblog.dto.site.AdminDtos.LastLoginInfoDto;
import com.kk.kblog.dto.site.AdminDtos.ProfileVisibility;
import com.kk.kblog.dto.site.AdminDtos.UpdatePasswordRequest;
import com.kk.kblog.dto.site.AdminDtos.UpdateProfileRequest;
import com.kk.kblog.entity.site.AdminUserEntity;
import com.kk.kblog.repository.site.AdminUserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AdminAuthService {

    private static final long SINGLE_ADMIN_ID = 1L;
    private static final ProfileVisibility DEFAULT_VISIBILITY = new ProfileVisibility(
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
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final AdminAvatarService adminAvatarService;

    public AdminAuthService(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper,
            AdminAvatarService adminAvatarService
    ) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.adminAvatarService = adminAvatarService;
    }

    @Transactional(readOnly = true)
    public AdminMeDto me() {
        return toMeDto(getAdmin());
    }

    @Transactional(readOnly = true)
    public LastLoginInfoDto getLastLoginInfo() {
        AdminUserEntity admin = getAdmin();
        return new LastLoginInfoDto(
                admin.getLastLoginAt(),
                admin.getLastLoginLocation()
        );
    }

    @Transactional
    public AdminMeDto updateProfile(UpdateProfileRequest request) {
        var admin = getAdmin();
        boolean changed = false;

        if (request.avatarUrl() != null || request.avatarKey() != null) {
            adminAvatarService.setAvatar(request.avatarUrl(), request.avatarKey());
            changed = true;
        }

        if (request.username() != null) {
            var username = request.username().trim();
            if (username.isBlank()) throw new IllegalArgumentException("username cannot be blank");
            admin.setUsername(username);
            changed = true;
        }
        if (request.gender() != null) {
            admin.setGender(request.gender().isBlank() ? null : request.gender().trim());
            changed = true;
        }
        if (request.age() != null) {
            var age = request.age();
            if (age < 0 || age > 150) throw new IllegalArgumentException("invalid age");
            admin.setAge(age);
            changed = true;
        }
        if (request.email() != null) {
            admin.setEmail(request.email().isBlank() ? null : request.email().trim());
            changed = true;
        }
        if (request.phone() != null) {
            admin.setPhone(request.phone().isBlank() ? null : request.phone().trim());
            changed = true;
        }
        if (request.qq() != null) {
            admin.setQq(request.qq().isBlank() ? null : request.qq().trim());
            changed = true;
        }
        if (request.wechat() != null) {
            admin.setWechat(request.wechat().isBlank() ? null : request.wechat().trim());
            changed = true;
        }
        if (request.github() != null) {
            admin.setGithub(request.github().isBlank() ? null : request.github().trim());
            changed = true;
        }
        if (request.gitee() != null) {
            admin.setGitee(request.gitee().isBlank() ? null : request.gitee().trim());
            changed = true;
        }
        if (request.visibility() != null) {
            admin.setProfileVisibilityJson(writeVisibilityJson(request.visibility()));
            changed = true;
        }

        if (changed) {
            admin.setUpdatedAt(Instant.now());
            log.info("admin_profile_updated username={}", admin.getUsername());
        }
        return toMeDto(admin);
    }

    @Transactional
    public void updatePassword(UpdatePasswordRequest request) {
        var admin = getAdmin();
        if (!passwordEncoder.matches(request.oldPassword(), admin.getPasswordHash())) {
            throw new IllegalArgumentException("旧密码错误");
        }
        admin.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        admin.setUpdatedAt(Instant.now());
        log.info("admin_password_updated");
    }

    private AdminUserEntity getAdmin() {
        return adminUserRepository.findById(SINGLE_ADMIN_ID)
                .orElseThrow(() -> new EntityNotFoundException("admin user not initialized"));
    }

    private AdminMeDto toMeDto(AdminUserEntity admin) {
        var visibility = readVisibility(admin.getProfileVisibilityJson());
        return new AdminMeDto(
                admin.getId(),
                admin.getUsername(),
                admin.getAvatarUrl(),
                admin.getGender(),
                admin.getAge(),
                admin.getEmail(),
                admin.getPhone(),
                admin.getQq(),
                admin.getWechat(),
                admin.getGithub(),
                admin.getGitee(),
                visibility,
                admin.getCreatedAt(),
                admin.getUpdatedAt(),
                admin.getLastLoginAt(),
                admin.getLastLoginIp(),
                admin.getLastLoginLocation()
        );
    }

    private ProfileVisibility readVisibility(String json) {
        if (json == null || json.isBlank()) return DEFAULT_VISIBILITY;
        try {
            var v = objectMapper.readValue(json, ProfileVisibility.class);
            return normalizeVisibility(v);
        } catch (Exception e) {
            return DEFAULT_VISIBILITY;
        }
    }

    private ProfileVisibility normalizeVisibility(ProfileVisibility v) {
        if (v == null) return DEFAULT_VISIBILITY;
        return new ProfileVisibility(
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

    private String writeVisibilityJson(ProfileVisibility visibility) {
        try {
            return objectMapper.writeValueAsString(normalizeVisibility(visibility));
        } catch (Exception e) {
            return null;
        }
    }

}
