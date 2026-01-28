package com.kk.kblog.controller;

import com.kk.kblog.dto.site.AdminDtos.LastLoginInfoDto;
import com.kk.kblog.service.admin.AdminAuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final AdminAuthService adminAuthService;

    public PublicController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    /**
     * 公开端点：获取上一次登录信息（不需要认证）
     */
    @GetMapping("/last-login")
    public LastLoginInfoDto getLastLoginInfo() {
        return adminAuthService.getLastLoginInfo();
    }
}
