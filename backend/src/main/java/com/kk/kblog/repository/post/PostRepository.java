package com.kk.kblog.repository.post;

import com.kk.kblog.entity.post.AiSummaryStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.kk.kblog.entity.post.PostEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PostRepository extends JpaRepository<PostEntity, Long> {

    interface PostVersion {
        Long getId();

        long getContentVersion();
    }

    @Transactional
    @Modifying
    @Query("update PostEntity p set p.aiSummaryStatus = :status, p.aiSummaryUpdatedAt = :now where p.id = :id")
    int updateAiSummaryStatus(@Param("id") long id, @Param("status") AiSummaryStatus status, @Param("now") Instant now);

    /**
     * 写入摘要；仅当内容版本号仍是生成时的版本才生效，旧摘要不会覆盖新内容
     */
    @Transactional
    @Modifying
    @Query("update PostEntity p set p.aiSummary = :summary, p.aiSummaryStatus = com.kk.kblog.entity.post.AiSummaryStatus.READY, "
            + "p.aiSummaryUpdatedAt = :now where p.id = :id and p.contentVersion = :version")
    int saveAiSummary(@Param("id") long id, @Param("version") long version,
                      @Param("summary") String summary, @Param("now") Instant now);

    @Transactional
    @Modifying
    @Query("update PostEntity p set p.aiSummaryStatus = com.kk.kblog.entity.post.AiSummaryStatus.FAILED, "
            + "p.aiSummaryUpdatedAt = :now where p.id = :id and p.contentVersion = :version")
    int markAiSummaryFailed(@Param("id") long id, @Param("version") long version, @Param("now") Instant now);

    @Query("select p.id as id, p.contentVersion as contentVersion from PostEntity p where p.aiSummaryStatus is null "
            + "or p.aiSummaryStatus <> com.kk.kblog.entity.post.AiSummaryStatus.READY order by p.id desc")
    List<PostVersion> findNeedingAiSummary();
}
