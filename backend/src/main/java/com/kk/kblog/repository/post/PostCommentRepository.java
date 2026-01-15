package com.kk.kblog.repository.post;

import com.kk.kblog.entity.post.PostCommentEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostCommentRepository extends JpaRepository<PostCommentEntity, Long> {

    List<PostCommentEntity> findByPostIdAndParentIdIsNullOrderByIdDesc(long postId);

    List<PostCommentEntity> findByPostIdAndParentIdInOrderByCreatedAtAscIdAsc(long postId, List<Long> parentIds);

    List<PostCommentEntity> findByPostIdAndParentIdOrderByCreatedAtAscIdAsc(long postId, long parentId);

    Optional<PostCommentEntity> findByIdAndPostId(long id, long postId);

    Optional<PostCommentEntity> findTopByPostIdAndParentIdIsNullAndLikesGreaterThanOrderByLikesDescCreatedAtDescIdDesc(long postId, int likes);
}
