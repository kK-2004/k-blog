package com.kk.kblog.controller.admin;

import com.kk.kblog.service.admin.AdminAvatarService;
import com.kk.kblog.service.oss.AliOssPresignService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/avatar")
public class AdminAvatarController {

    private final AliOssPresignService aliOssPresignService;
    private final AdminAvatarService adminAvatarService;

    public AdminAvatarController(AliOssPresignService aliOssPresignService, AdminAvatarService adminAvatarService) {
        this.aliOssPresignService = aliOssPresignService;
        this.adminAvatarService = adminAvatarService;
    }

    public record PresignAvatarRequest(
            @NotBlank String filename,
            @NotBlank String contentType
    ) {
    }

    public record PresignAvatarResponse(
            String uploadUrl,
            String url,
            String key,
            long expireSeconds
    ) {
    }

    @PostMapping("/presign")
    public PresignAvatarResponse presign(@Valid @RequestBody PresignAvatarRequest request) {
        var contentType = request.contentType().trim();
        if (!contentType.startsWith("image/")) {
            throw new IllegalArgumentException("only image content-type is allowed");
        }

        var ext = safeExt(request.filename());
        var datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        var key = "avatars/" + datePath + "/" + UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        key = aliOssPresignService.normalizeKey(key);

        long expireSeconds = 600;
        var uploadUrl = aliOssPresignService.presignPut(key, contentType, expireSeconds).toString();
        var url = aliOssPresignService.toPublicUrl(key);
        return new PresignAvatarResponse(uploadUrl, url, key, expireSeconds);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete() {
        adminAvatarService.deleteAvatar();
        return ResponseEntity.noContent().build();
    }

    private String safeExt(String filename) {
        if (filename == null) return "";
        var fn = filename.trim();
        int dot = fn.lastIndexOf('.');
        if (dot < 0 || dot == fn.length() - 1) return "";
        var ext = fn.substring(dot + 1).toLowerCase();
        if (ext.length() > 10) return "";
        return ext.replaceAll("[^a-z0-9]", "");
    }
}
