package com.kk.kblog.repository.site;

import com.kk.kblog.entity.site.BlogQuickActionEntity;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogQuickActionRepository extends JpaRepository<BlogQuickActionEntity, String> {
    default List<BlogQuickActionEntity> findAllSorted() {
        return findAll().stream().sorted(Comparator.comparingInt(BlogQuickActionEntity::getOrder)).toList();
    }
}

