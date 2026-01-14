package com.kk.kblog.repository.post;

import com.kk.kblog.entity.post.PostHotCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostHotCommentRepository extends JpaRepository<PostHotCommentEntity, Long> {
}

