package com.kk.kblog.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class PostRequests {

    private PostRequests() {
    }

    public record CreatePostRequest(
            @NotBlank String title,
            @NotBlank String content,
            Integer views,
            Integer likes,
            Integer comments,
            Boolean pinned
    ) {
    }

    public record UpdatePostRequest(
            @NotBlank String title,
            @NotBlank String content,
            @NotNull Integer views,
            @NotNull Integer likes,
            @NotNull Integer comments,
            @NotNull Boolean pinned
    ) {
    }

    public record PatchPostRequest(
            String title,
            String content,
            Integer views,
            Integer likes,
            Integer comments,
            Boolean pinned
    ) {
    }
}
