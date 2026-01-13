package com.kk.kblog.service.admin;

import com.kk.kblog.repository.site.AdminUserRepository;
import java.util.List;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository adminUserRepository;

    public AdminUserDetailsService(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // website has only one admin; login username is fixed "admin"
        if (!"admin".equals(username)) {
            throw new UsernameNotFoundException("not found");
        }
        var admin = adminUserRepository.findById(1L).orElseThrow(() -> new UsernameNotFoundException("not found"));
        return new User("admin", admin.getPasswordHash(), List.of(() -> "ROLE_ADMIN"));
    }
}

