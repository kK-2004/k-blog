package com.kk.kblog.dto.post;

import java.util.List;

public record PostCommentDto(
        long id,
        String user,
        String text,
        int likes,
        long createdAt,
        boolean hot,
        List<PostCommentReplyDto> replies
) {
    public record PostCommentReplyDto(
            long id,
            String user,
            String text,
            int likes,
            long createdAt,
            String toUser
    ) {
    }
}

