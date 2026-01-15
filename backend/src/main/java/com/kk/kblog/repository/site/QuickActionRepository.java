package com.kk.kblog.repository.site;

import com.kk.kblog.entity.site.QuickActionEntity;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuickActionRepository extends JpaRepository<QuickActionEntity, String> {
    default List<QuickActionEntity> findAllSorted() {
        return findAll().stream().sorted(Comparator.comparingInt(QuickActionEntity::getOrder)).toList();
    }
}

