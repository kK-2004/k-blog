package com.kk.kblog.service.admin;

import com.kk.kblog.dto.site.AdminDtos.AdminMeDto;
import com.kk.kblog.dto.site.AdminDtos.UpdatePasswordRequest;
import com.kk.kblog.dto.site.AdminDtos.UpdateProfileRequest;
import com.kk.kblog.entity.site.AdminUserEntity;
import com.kk.kblog.repository.site.AdminUserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AdminAuthService {

    private static final long SINGLE_ADMIN_ID = 1L;

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAuthService(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AdminMeDto me() {
        return toMeDto(getAdmin());
    }

    @Transactional
    public AdminMeDto updateProfile(UpdateProfileRequest request) {
        var admin = getAdmin();
        admin.setUsername(request.username());
        admin.setUpdatedAt(Instant.now());
        log.info("admin_profile_updated username={}", admin.getUsername());
        return toMeDto(admin);
    }

    @Transactional
    public void updatePassword(UpdatePasswordRequest request) {
        var admin = getAdmin();
        if (!passwordEncoder.matches(request.oldPassword(), admin.getPasswordHash())) {
            throw new IllegalArgumentException("invalid old password");
        }
        admin.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        admin.setUpdatedAt(Instant.now());
        log.info("admin_password_updated");
    }

    private AdminUserEntity getAdmin() {
        return adminUserRepository.findById(SINGLE_ADMIN_ID)
                .orElseThrow(() -> new EntityNotFoundException("admin user not initialized"));
    }

    private AdminMeDto toMeDto(AdminUserEntity admin) {
        return new AdminMeDto(
                admin.getId(),
                admin.getUsername(),
                admin.getCreatedAt(),
                admin.getUpdatedAt(),
                admin.getLastLoginAt(),
                admin.getLastLoginIp(),
                admin.getLastLoginLocation()
        );
    }

}
