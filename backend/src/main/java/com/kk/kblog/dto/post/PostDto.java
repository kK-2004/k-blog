package com.kk.kblog.dto.post;

public record PostDto(
        Long id,
        String author,
        String title,
        Long createdAt,
        Long updatedAt,
        String content,
        int views,
        int likes,
        int comments,
        boolean pinned
) {
}
