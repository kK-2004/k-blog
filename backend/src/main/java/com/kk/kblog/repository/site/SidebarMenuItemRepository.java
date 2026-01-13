package com.kk.kblog.repository.site;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import com.kk.kblog.entity.site.SidebarMenuItemEntity;

public interface SidebarMenuItemRepository extends JpaRepository<SidebarMenuItemEntity, String> {

    default List<SidebarMenuItemEntity> findAllSorted() {
        return findAll(Sort.by(Sort.Order.asc("order")));
    }
}
