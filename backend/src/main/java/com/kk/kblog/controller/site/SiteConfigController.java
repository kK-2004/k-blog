package com.kk.kblog.controller.site;

import com.kk.kblog.dto.site.MenuItemDto;
import com.kk.kblog.dto.site.QuickActionDto;
import com.kk.kblog.service.site.BlogQuickActionService;
import com.kk.kblog.service.site.QuickActionService;
import com.kk.kblog.service.site.SidebarMenuItemService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/site")
public class SiteConfigController {

    private final SidebarMenuItemService sidebarMenuItemService;
    private final QuickActionService quickActionService;
    private final BlogQuickActionService blogQuickActionService;

    public SiteConfigController(
            SidebarMenuItemService sidebarMenuItemService,
            QuickActionService quickActionService,
            BlogQuickActionService blogQuickActionService
    ) {
        this.sidebarMenuItemService = sidebarMenuItemService;
        this.quickActionService = quickActionService;
        this.blogQuickActionService = blogQuickActionService;
    }

    @GetMapping("/sidebar-menu-items")
    public List<MenuItemDto> listMenuItems() {
        return sidebarMenuItemService.listAll();
    }

    @GetMapping("/sidebar-menu-items/visible")
    public List<MenuItemDto> listVisibleMenuItems() {
        return sidebarMenuItemService.listVisible();
    }

    @PutMapping("/sidebar-menu-items")
    public List<MenuItemDto> replaceMenuItems(@RequestBody List<@Valid MenuItemDto> items) {
        return sidebarMenuItemService.replaceAll(items);
    }

    @GetMapping("/quick-actions")
    public List<QuickActionDto> listQuickActions() {
        return quickActionService.listAll();
    }

    @GetMapping("/quick-actions/visible")
    public List<QuickActionDto> listVisibleQuickActions() {
        return quickActionService.listVisible();
    }

    @PutMapping("/quick-actions")
    public List<QuickActionDto> replaceQuickActions(@RequestBody List<@Valid QuickActionDto> items) {
        return quickActionService.replaceAll(items);
    }

    @GetMapping("/blog-quick-actions")
    public List<QuickActionDto> listBlogQuickActions() {
        return blogQuickActionService.listAll();
    }

    @GetMapping("/blog-quick-actions/visible")
    public List<QuickActionDto> listVisibleBlogQuickActions() {
        return blogQuickActionService.listVisible();
    }

    @PutMapping("/blog-quick-actions")
    public List<QuickActionDto> replaceBlogQuickActions(@RequestBody List<@Valid QuickActionDto> items) {
        return blogQuickActionService.replaceAll(items);
    }
}
