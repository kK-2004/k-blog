package com.kk.kblog.service.site;

import com.kk.kblog.dto.site.MenuItemDto;
import com.kk.kblog.entity.site.SidebarMenuItemEntity;
import com.kk.kblog.repository.site.SidebarMenuItemRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SidebarMenuItemService {

    private final SidebarMenuItemRepository repository;

    public SidebarMenuItemService(SidebarMenuItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<MenuItemDto> listAll() {
        return repository.findAllSorted().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<MenuItemDto> listVisible() {
        return repository.findAllSorted().stream().filter(SidebarMenuItemEntity::isVisible).map(this::toDto).toList();
    }

    @Transactional
    public List<MenuItemDto> replaceAll(@Valid List<MenuItemDto> items) {
        repository.deleteAllInBatch();
        repository.saveAll(items.stream().map(this::toEntity).toList());
        return listAll();
    }

    private MenuItemDto toDto(SidebarMenuItemEntity entity) {
        return new MenuItemDto(entity.getId(), entity.getLabel(), entity.getIcon(), entity.isRequiresAuth(), entity.isVisible(), entity.getOrder());
    }

    private SidebarMenuItemEntity toEntity(MenuItemDto dto) {
        return new SidebarMenuItemEntity(dto.id(), dto.label(), dto.icon(), dto.requiresAuth(), dto.visible(), dto.order());
    }
}
