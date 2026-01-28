package com.kk.kblog.repository.post;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostStatsJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostStatsJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int incrementViews(long postId) {
        return jdbcTemplate.update("UPDATE posts SET views = views + 1, updated_at = UTC_TIMESTAMP(3) WHERE id = ?", postId);
    }

    public int incrementLikes(long postId) {
        return jdbcTemplate.update("UPDATE posts SET likes = likes + 1, updated_at = UTC_TIMESTAMP(3) WHERE id = ?", postId);
    }

    public int incrementComments(long postId) {
        return jdbcTemplate.update("UPDATE posts SET comments = comments + 1, updated_at = UTC_TIMESTAMP(3) WHERE id = ?", postId);
    }

    public int decrementComments(long postId, int delta) {
        if (delta <= 0) return 0;
        return jdbcTemplate.update("UPDATE posts SET comments = GREATEST(0, comments - ?), updated_at = UTC_TIMESTAMP(3) WHERE id = ?", delta, postId);
    }
}
