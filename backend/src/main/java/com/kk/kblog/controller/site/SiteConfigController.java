package com.kk.kblog.controller.site;

import com.kk.kblog.dto.site.MenuItemDto;
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

    public SiteConfigController(SidebarMenuItemService sidebarMenuItemService) {
        this.sidebarMenuItemService = sidebarMenuItemService;
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
}
