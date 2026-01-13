package com.kk.kblog.util.ratelimit;

public class RateLimitedException extends RuntimeException {
    public RateLimitedException(String message) {
        super(message);
    }
}

