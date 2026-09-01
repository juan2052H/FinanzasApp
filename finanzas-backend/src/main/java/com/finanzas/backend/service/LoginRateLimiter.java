package com.finanzas.backend.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiter {
    private static final int MAX_FAILURES = 5;
    private static final long LOCK_SECONDS = 300L;
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String email) {
        Attempt attempt = attempts.get(key(email));
        if (attempt == null || attempt.lockedUntil == null) {
            return false;
        }
        if (Instant.now().isAfter(attempt.lockedUntil)) {
            attempts.remove(key(email));
            return false;
        }
        return true;
    }

    public void recordSuccess(String email) {
        attempts.remove(key(email));
    }

    public void recordFailure(String email) {
        Attempt attempt = attempts.computeIfAbsent(key(email), ignored -> new Attempt());
        attempt.failures++;
        if (attempt.failures >= MAX_FAILURES) {
            attempt.lockedUntil = Instant.now().plusSeconds(LOCK_SECONDS);
        }
    }

    private String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Attempt {
        int failures;
        Instant lockedUntil;
    }
}
