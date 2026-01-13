package com.kk.kblog.service.admin;

import com.kk.kblog.repository.site.AdminUserRepository;
import com.kk.kblog.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AdminLoginAuditService {

    private final AdminUserRepository adminUserRepository;
    private final IpLocationResolver ipLocationResolver;

    public AdminLoginAuditService(AdminUserRepository adminUserRepository, IpLocationResolver ipLocationResolver) {
        this.adminUserRepository = adminUserRepository;
        this.ipLocationResolver = ipLocationResolver;
    }

    @Transactional
    public void onLoginSuccess(HttpServletRequest request) {
        String ip = IpUtil.getClientIp(request);
        String location = ipLocationResolver.resolveLocation(ip);
        adminUserRepository.updateLastLogin(1L, Instant.now(), ip, location);
        log.info("admin_login_success ip={} location={}", ip, location);
    }
}

