package com.kk.kblog.controller.site;

import com.kk.kblog.dto.site.MenuItemDto;
import com.kk.kblog.dto.site.QuickActionDto;
import com.kk.kblog.service.RedisService;
import com.kk.kblog.service.site.BlogQuickActionService;
import com.kk.kblog.service.site.QuickActionService;
import com.kk.kblog.service.site.SidebarMenuItemService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/site")
public class SiteConfigController {

    private final SidebarMenuItemService sidebarMenuItemService;
    private final QuickActionService quickActionService;
    private final BlogQuickActionService blogQuickActionService;
    private final RedisService redisService;

    private static final String CACHE_KEY_VISIBLE_ACTIONS = "blogQuickActions:visible";

    public SiteConfigController(
            SidebarMenuItemService sidebarMenuItemService,
            QuickActionService quickActionService,
            BlogQuickActionService blogQuickActionService,
            RedisService redisService
    ) {
        this.sidebarMenuItemService = sidebarMenuItemService;
        this.quickActionService = quickActionService;
        this.blogQuickActionService = blogQuickActionService;
        this.redisService = redisService;
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
        // 先从缓存获取
        List<QuickActionDto> cached = redisService.get(CACHE_KEY_VISIBLE_ACTIONS,
            new com.fasterxml.jackson.core.type.TypeReference<List<QuickActionDto>>() {});
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，查询数据库
        List<QuickActionDto> result = blogQuickActionService.listVisible();

        // 写入缓存，5分钟过期
        redisService.set(CACHE_KEY_VISIBLE_ACTIONS, result, 5, TimeUnit.MINUTES);
        return result;
    }

    @PutMapping("/blog-quick-actions")
    public List<QuickActionDto> replaceBlogQuickActions(@RequestBody List<@Valid QuickActionDto> items) {
        List<QuickActionDto> result = blogQuickActionService.replaceAll(items);

        // 清除缓存
        redisService.delete(CACHE_KEY_VISIBLE_ACTIONS);

        return result;
    }
}
