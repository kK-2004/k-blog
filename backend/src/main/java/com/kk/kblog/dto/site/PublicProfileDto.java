package com.kk.kblog.dto.site;

public record PublicProfileDto(
        String username,
        String avatarUrl,
        String gender,
        Integer age,
        String email,
        String phone,
        String qq,
        String wechat,
        String github,
        String gitee
) {
}

