package com.kk.kblog.filter;

import com.kk.kblog.util.IpUtil;
import com.kk.kblog.util.ratelimit.TokenBucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final long BUCKET_CAPACITY = 20;
    private static final long BUCKET_REFILL_RATE_PER_SEC = 1;

    private static final int BLACKLIST_THRESHOLD_PER_MINUTE = 100;
    private static final long WINDOW_MS = 60_000L;

    private final ConcurrentMap<String, RateState> stateByKey = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> blacklistUntilMillis = new ConcurrentHashMap<>();
    private final AtomicReference<LocalDate> lastResetDate = new AtomicReference<>(LocalDate.now());

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return uri == null || !uri.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        resetIfNewDay();

        String ip = IpUtil.getClientIp(request);
        String fp = resolveFingerprint(request);
        String key = ip + "|" + sha256Hex(fp);

        Long blacklistUntil = blacklistUntilMillis.get(key);
        long now = System.currentTimeMillis();
        if (blacklistUntil != null) {
            if (now < blacklistUntil) {
                write429(response, "你由于频繁操作被列入黑名单！");
                return;
            }
            blacklistUntilMillis.remove(key, blacklistUntil);
        }

        RateState state = stateByKey.computeIfAbsent(key, k -> new RateState(new TokenBucket(BUCKET_REFILL_RATE_PER_SEC, BUCKET_CAPACITY)));

        boolean blacklistedNow = false;
        synchronized (state) {
            if (now - state.windowStartAtMillis >= WINDOW_MS) {
                state.windowStartAtMillis = now;
                state.windowCount = 0;
            }
            state.windowCount++;
            if (state.windowCount >= BLACKLIST_THRESHOLD_PER_MINUTE) {
                blacklistUntilMillis.put(key, nextMidnightMillis());
                blacklistedNow = true;
                log.info("[限流黑名单-新增] ip={} fp={}", ip, fp);
            }
        }

        if (blacklistedNow) {
            write429(response, "你由于频繁操作被列入黑名单！");
            return;
        }

        if (!state.bucket.tryAcquire()) {
            write429(response, "请勿频繁操作");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void resetIfNewDay() {
        LocalDate today = LocalDate.now();
        LocalDate prev = lastResetDate.get();
        if (!Objects.equals(today, prev) && lastResetDate.compareAndSet(prev, today)) {
            stateByKey.clear();
            blacklistUntilMillis.clear();
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    void resetAtMidnight() {
        lastResetDate.set(LocalDate.now());
        stateByKey.clear();
        blacklistUntilMillis.clear();
    }

    private long nextMidnightMillis() {
        ZonedDateTime zdt = ZonedDateTime.now(ZoneId.systemDefault()).toLocalDate().plusDays(1).atStartOfDay(ZoneId.systemDefault());
        return zdt.toInstant().toEpochMilli();
    }

    private String resolveFingerprint(HttpServletRequest request) {
        String fp = request.getHeader("X-Device-Fingerprint");
        if (fp == null || fp.isBlank()) {
            fp = request.getHeader("X-Device-Id");
        }
        if (fp == null || fp.isBlank()) {
            fp = request.getHeader("User-Agent");
        }
        if (fp == null) fp = "";
        fp = fp.trim();
        if (fp.length() > 512) {
            fp = fp.substring(0, 512);
        }
        return fp;
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private void write429(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String json = "{\"message\":\"" + escapeJson(message) + "\"}";
        response.getWriter().write(json);
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static final class RateState {
        private final TokenBucket bucket;
        private long windowStartAtMillis = System.currentTimeMillis();
        private int windowCount = 0;

        private RateState(TokenBucket bucket) {
            this.bucket = bucket;
        }
    }
}
