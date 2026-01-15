package com.kk.kblog.service.site;

import com.kk.kblog.dto.site.QuickActionDto;
import com.kk.kblog.entity.site.BlogQuickActionEntity;
import com.kk.kblog.repository.site.BlogQuickActionRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlogQuickActionService {

    private final BlogQuickActionRepository repository;

    public BlogQuickActionService(BlogQuickActionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<QuickActionDto> listAll() {
        return repository.findAllSorted().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<QuickActionDto> listVisible() {
        return repository.findAllSorted().stream().filter(BlogQuickActionEntity::isVisible).map(this::toDto).toList();
    }

    @Transactional
    public List<QuickActionDto> replaceAll(@Valid List<QuickActionDto> items) {
        validateItems(items);
        repository.deleteAllInBatch();
        repository.saveAll(items.stream().map(this::toEntity).toList());
        return listAll();
    }

    private void validateItems(List<QuickActionDto> items) {
        for (var item : items) {
            var type = item.targetType() == null ? "" : item.targetType().trim();
            var target = item.target() == null ? "" : item.target().trim();
            if (!type.equals("internal") && !type.equals("external")) {
                throw new IllegalArgumentException("targetType 仅支持 internal 或 external");
            }
            if (type.equals("internal") && !target.startsWith("#/")) {
                throw new IllegalArgumentException("internal 目标必须以 #/ 开头");
            }
            if (type.equals("external") && !(target.startsWith("http://") || target.startsWith("https://"))) {
                throw new IllegalArgumentException("external 目标必须以 http:// 或 https:// 开头");
            }
        }
    }

    private QuickActionDto toDto(BlogQuickActionEntity entity) {
        return new QuickActionDto(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getIcon(),
                entity.getTargetType(),
                entity.getTarget(),
                entity.isVisible(),
                entity.getOrder()
        );
    }

    private BlogQuickActionEntity toEntity(QuickActionDto dto) {
        return new BlogQuickActionEntity(
                dto.id(),
                dto.title(),
                dto.description(),
                dto.icon(),
                dto.targetType(),
                dto.target(),
                dto.visible(),
                dto.order()
        );
    }
}

