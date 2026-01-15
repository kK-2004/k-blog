package com.kk.kblog.dto.site;

import jakarta.validation.constraints.NotBlank;

public record QuickActionDto(
        @NotBlank String id,
        @NotBlank String title,
        String description,
        @NotBlank String icon,
        @NotBlank String targetType,
        @NotBlank String target,
        boolean visible,
        int order
) {
}

