package com.kk.kblog.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PostCommentRequests {

    public record CreateCommentRequest(
            @Size(max = 50) String user,
            @NotBlank @Size(max = 2000) String text
    ) {
    }

    public record CreateReplyRequest(
            @Size(max = 50) String user,
            @NotBlank @Size(max = 2000) String text,
            @Size(max = 50) String toUser
    ) {
    }

    public record LikeResponse(int likes) {
    }
}

