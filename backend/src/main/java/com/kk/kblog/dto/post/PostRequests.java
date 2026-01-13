package com.kk.kblog.dto.post;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class PostRequests {

    private PostRequests() {
    }

    public record CreatePostRequest(
            @NotBlank String author,
            @NotBlank String title,
            String time,
            @NotBlank String content,
            Integer views,
            Integer likes,
            Integer comments,
            Boolean pinned,
            @Valid HotCommentRequest hotComment
    ) {
    }

    public record UpdatePostRequest(
            @NotBlank String author,
            @NotBlank String title,
            @NotNull String time,
            @NotBlank String content,
            @NotNull Integer views,
            @NotNull Integer likes,
            @NotNull Integer comments,
            @NotNull Boolean pinned,
            @Valid HotCommentRequest hotComment
    ) {
    }

    public record PatchPostRequest(
            String author,
            String title,
            String time,
            String content,
            Integer views,
            Integer likes,
            Integer comments,
            Boolean pinned,
            @Valid HotCommentRequest hotComment
    ) {
    }

    public record HotCommentRequest(
            @NotBlank String user,
            @NotBlank String text,
            Integer likes,
            List<@Valid HotCommentReplyRequest> replies
    ) {
    }

    public record HotCommentReplyRequest(
            @NotBlank String user,
            @NotBlank String text,
            Integer likes
    ) {
    }
}
