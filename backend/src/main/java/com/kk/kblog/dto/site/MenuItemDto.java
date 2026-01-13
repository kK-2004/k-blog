package com.kk.kblog.dto.site;

import jakarta.validation.constraints.NotBlank;

public record MenuItemDto(
        @NotBlank String id,
        @NotBlank String label,
        @NotBlank String icon,
        boolean requiresAuth,
        boolean visible,
        int order
) {
}
