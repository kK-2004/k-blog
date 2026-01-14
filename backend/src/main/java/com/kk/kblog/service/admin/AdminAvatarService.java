package com.kk.kblog.service.admin;

import com.kk.kblog.repository.site.AdminUserRepository;
import com.kk.kblog.service.oss.AliOssPresignService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class AdminAvatarService {
    private static final long SINGLE_ADMIN_ID = 1L;

    private final AdminUserRepository adminUserRepository;
    private final AliOssPresignService oss;

    public AdminAvatarService(AdminUserRepository adminUserRepository, AliOssPresignService oss) {
        this.adminUserRepository = adminUserRepository;
        this.oss = oss;
    }

    @Transactional
    public void setAvatar(String avatarUrl, String avatarKey) {
        var admin = adminUserRepository.findById(SINGLE_ADMIN_ID)
                .orElseThrow(() -> new EntityNotFoundException("admin user not initialized"));

        var oldKey = admin.getAvatarKey();

        admin.setAvatarUrl(StringUtils.hasText(avatarUrl) ? avatarUrl.trim() : null);
        admin.setAvatarKey(StringUtils.hasText(avatarKey) ? avatarKey.trim() : null);

        if (StringUtils.hasText(oldKey) && !oldKey.equals(admin.getAvatarKey())) {
            deleteQuietly(oldKey);
        }

        log.info("admin_avatar_updated keyPresent={}", StringUtils.hasText(admin.getAvatarKey()));
    }

    @Transactional
    public void deleteAvatar() {
        var admin = adminUserRepository.findById(SINGLE_ADMIN_ID)
                .orElseThrow(() -> new EntityNotFoundException("admin user not initialized"));

        var oldKey = admin.getAvatarKey();
        admin.setAvatarUrl(null);
        admin.setAvatarKey(null);

        deleteQuietly(oldKey);
        log.info("admin_avatar_deleted");
    }

    private void deleteQuietly(String key) {
        if (!StringUtils.hasText(key)) return;
        try {
            if (oss.isReady()) {
                oss.deleteByKey(key);
            }
        } catch (Exception e) {
            log.warn("failed_to_delete_oss_object key={} msg={}", key, e.getMessage());
        }
    }
}

