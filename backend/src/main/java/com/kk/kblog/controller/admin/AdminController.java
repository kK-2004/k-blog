package com.kk.kblog.controller.admin;

import com.kk.kblog.dto.site.AdminDtos.AdminMeDto;
import com.kk.kblog.dto.site.AdminDtos.UpdatePasswordRequest;
import com.kk.kblog.dto.site.AdminDtos.UpdateProfileRequest;
import com.kk.kblog.service.admin.AdminAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminAuthService adminAuthService;

    public AdminController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @GetMapping("/me")
    public AdminMeDto me() {
        return adminAuthService.me();
    }

    @PutMapping("/profile")
    public AdminMeDto updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return adminAuthService.updateProfile(request);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        adminAuthService.updatePassword(request);
        return ResponseEntity.noContent().build();
    }
}
