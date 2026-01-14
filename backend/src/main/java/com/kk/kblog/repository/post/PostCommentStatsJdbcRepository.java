package com.kk.kblog.repository.post;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostCommentStatsJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostCommentStatsJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int incrementLikes(long postId, long commentId) {
        return jdbcTemplate.update(
                "UPDATE post_comments SET likes = likes + 1 WHERE id = ? AND post_id = ?",
                commentId,
                postId
        );
    }

    public Integer getLikes(long postId, long commentId) {
        return jdbcTemplate.query(
                "SELECT likes FROM post_comments WHERE id = ? AND post_id = ?",
                rs -> rs.next() ? rs.getInt(1) : null,
                commentId,
                postId
        );
    }
}

