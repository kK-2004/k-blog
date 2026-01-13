package com.kk.kblog.repository.post;

import org.springframework.data.jpa.repository.JpaRepository;
import com.kk.kblog.entity.post.PostEntity;

public interface PostRepository extends JpaRepository<PostEntity, Long> {
}
