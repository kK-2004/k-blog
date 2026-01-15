package com.kk.kblog.config;

import com.kk.kblog.service.admin.AdminLoginAuditService;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AdminLoginAuditService adminLoginAuditService) throws Exception {
        http
                .cors(cors -> {})
//                .csrf(csrf -> csrf
//                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
//                        // allow session login/logout without CSRF token; keep CSRF for other state-changing admin APIs
//                        .ignoringRequestMatchers("/api/admin/login", "/api/admin/logout")
//                )
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/login", "/api/admin/logout").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/admin/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/site/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/posts").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/posts/*").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/posts/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/posts/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/posts/*/comments/**").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            res.getWriter().write("{\"message\":\"未登录\"}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            res.getWriter().write("{\"message\":\"无权限\"}");
                        })
                )
                .formLogin(form -> form
                        .loginProcessingUrl("/api/admin/login")
                        .successHandler((req, res, authentication) -> {
                            adminLoginAuditService.onLoginSuccess(req);
                            res.setStatus(HttpServletResponse.SC_OK);
                            res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            res.getWriter().write("{\"message\":\"ok\"}");
                        })
                        .failureHandler((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            res.getWriter().write("{\"message\":\"用户名或密码错误\"}");
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/api/admin/logout")
                        .logoutSuccessHandler((req, res, auth) -> {
                            res.setStatus(HttpServletResponse.SC_OK);
                            res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            res.getWriter().write("{\"message\":\"ok\"}");
                        })
                );

        return http.build();
    }
}
