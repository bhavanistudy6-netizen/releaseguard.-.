package com.releaseguard.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;
    public static final long LOCKOUT_DURATION_SECONDS = 900; // 15 minutes

    private static class Attempt {
        int count;
        Instant firstFailedAt;
        Instant lockedUntil;

        Attempt(Instant now) {
            this.count = 1;
            this.firstFailedAt = now;
            this.lockedUntil = null;
        }
    }

    private final Map<String, Attempt> attemptsCache = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        if (key == null || key.isBlank()) return false;
        Attempt attempt = attemptsCache.get(key.toLowerCase());
        if (attempt == null) return false;

        Instant now = Instant.now();
        if (attempt.lockedUntil != null) {
            if (now.isBefore(attempt.lockedUntil)) {
                return true;
            } else {
                // Lockout expired
                attemptsCache.remove(key.toLowerCase());
                return false;
            }
        }

        // Expire attempt window if older than lockout duration
        if (now.isAfter(attempt.firstFailedAt.plusSeconds(LOCKOUT_DURATION_SECONDS))) {
            attemptsCache.remove(key.toLowerCase());
            return false;
        }

        return attempt.count >= MAX_ATTEMPTS;
    }

    public void loginFailed(String key) {
        if (key == null || key.isBlank()) return;
        Instant now = Instant.now();
        attemptsCache.compute(key.toLowerCase(), (k, existing) -> {
            if (existing == null || now.isAfter(existing.firstFailedAt.plusSeconds(LOCKOUT_DURATION_SECONDS))) {
                return new Attempt(now);
            }
            existing.count++;
            if (existing.count >= MAX_ATTEMPTS && existing.lockedUntil == null) {
                existing.lockedUntil = now.plusSeconds(LOCKOUT_DURATION_SECONDS);
            }
            return existing;
        });
    }

    public void loginSucceeded(String key) {
        if (key != null) {
            attemptsCache.remove(key.toLowerCase());
        }
    }

    public void clearAll() {
        attemptsCache.clear();
    }
}

