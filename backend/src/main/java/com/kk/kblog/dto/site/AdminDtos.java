package com.kk.kblog.dto.site;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record LoginRequest(@NotBlank String password) {
    }

    public record AdminMeDto(
            Long id,
            String username,
            Instant createdAt,
            Instant updatedAt,
            Instant lastLoginAt,
            String lastLoginIp,
            String lastLoginLocation
    ) {
    }

    public record UpdateProfileRequest(@NotBlank String username) {
    }

    public record UpdatePasswordRequest(@NotBlank String oldPassword, @NotBlank String newPassword) {
    }
}

