package com.kk.kblog.service.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IpLocationConfig {

    @Bean
    public IpLocationResolver ipLocationResolver(
            @Value("${app.ip.fastapi.enabled:true}") boolean enabled,
            @Value("${app.ip.fastapi.url-template}") String urlTemplate,
            @Value("${app.ip.fastapi.connect-timeout-ms:800}") int connectTimeoutMs,
            @Value("${app.ip.fastapi.read-timeout-ms:1200}") int readTimeoutMs
    ) {
        IpLocationResolver fallback = new BasicIpLocationResolver();
        if (!enabled) {
            return fallback;
        }
        return new FastApiIpLocationResolver(urlTemplate, connectTimeoutMs, readTimeoutMs, fallback);
    }
}
