package com.kk.kblog.controller.admin;

import com.kk.kblog.service.oss.AliOssPresignService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Markdown 编辑器图片上传接口
 */
@RestController
@RequestMapping("/api/admin/images")
public class AdminImageController {

    private final AliOssPresignService aliOssPresignService;

    public AdminImageController(AliOssPresignService aliOssPresignService) {
        this.aliOssPresignService = aliOssPresignService;
    }

    public record PresignImageRequest(
            @NotBlank String filename,
            @NotBlank String contentType
    ) {
    }

    public record PresignImageResponse(
            String uploadUrl,
            String url,
            String key,
            long expireSeconds
    ) {
    }

    /**
     * 获取图片上传预签名 URL
     * 用于 Markdown 编辑器直接上传到 OSS
     */
    @PostMapping("/presign")
    public PresignImageResponse presign(@Valid @RequestBody PresignImageRequest request) {
        var contentType = request.contentType().trim();
        if (!contentType.startsWith("image/")) {
            throw new IllegalArgumentException("只支持图片类型");
        }

        var ext = safeExt(request.filename());
        var datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        var key = "posts/" + datePath + "/" + UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        key = aliOssPresignService.normalizeKey(key);

        long expireSeconds = 600;
        var uploadUrl = aliOssPresignService.presignPut(key, contentType, expireSeconds).toString();
        var url = aliOssPresignService.toPublicUrl(key);
        return new PresignImageResponse(uploadUrl, url, key, expireSeconds);
    }

    private String safeExt(String filename) {
        if (filename == null) return "";
        var fn = filename.trim();
        int dot = fn.lastIndexOf('.');
        if (dot < 0 || dot == fn.length() - 1) return "";
        var ext = fn.substring(dot + 1).toLowerCase();
        if (ext.length() > 10) return "";
        // 只允许常见图片扩展名
        if (!ext.matches("jpg|jpeg|png|gif|webp|svg|bmp|ico")) {
            return "jpg";
        }
        return ext;
    }
}
