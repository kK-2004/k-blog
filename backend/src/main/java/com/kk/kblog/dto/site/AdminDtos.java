package com.kk.kblog.dto.site;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record LoginRequest(@NotBlank String password) {
    }

    public record ProfileVisibility(
            String gender,
            String age,
            String email,
            String phone,
            String qq,
            String wechat,
            String github,
            String gitee
    ) {
    }

    public record AdminMeDto(
            Long id,
            String username,
            String avatarUrl,
            String gender,
            Integer age,
            String email,
            String phone,
            String qq,
            String wechat,
            String github,
            String gitee,
            ProfileVisibility visibility,
            Instant createdAt,
            Instant updatedAt,
            Instant lastLoginAt,
            String lastLoginIp,
            String lastLoginLocation
    ) {
    }

    public record UpdateProfileRequest(
            String username,
            String avatarUrl,
            String avatarKey,
            String gender,
            Integer age,
            String email,
            String phone,
            String qq,
            String wechat,
            String github,
            String gitee,
            ProfileVisibility visibility
    ) {
    }

    public record UpdatePasswordRequest(@NotBlank String oldPassword, @NotBlank String newPassword) {
    }
}
