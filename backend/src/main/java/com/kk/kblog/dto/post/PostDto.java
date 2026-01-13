package com.kk.kblog.dto.post;

import java.util.List;

public record PostDto(
        Long id,
        String author,
        String title,
        String time,
        String content,
        HotCommentDto hotComment,
        int views,
        int likes,
        int comments,
        boolean pinned
) {

    public record HotCommentDto(
            Long id,
            String user,
            String text,
            int likes,
            List<HotCommentReplyDto> replies
    ) {
    }

    public record HotCommentReplyDto(
            Long id,
            String user,
            String text,
            int likes
    ) {
    }
}
