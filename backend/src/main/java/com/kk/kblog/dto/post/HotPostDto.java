package com.kk.kblog.dto.post;

/**
 * 热门文章 DTO
 */
public record HotPostDto(
        Long id,
        String title,
        long lastViewTime
) {
}
