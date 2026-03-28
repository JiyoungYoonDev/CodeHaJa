package com.codehaja.auth.cms.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    private record AttemptInfo(int count, LocalDateTime lockedUntil) {}

    private final ConcurrentHashMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String ip) {
        AttemptInfo info = attempts.get(ip);
        if (info == null) return false;
        if (info.lockedUntil() != null && info.lockedUntil().isAfter(LocalDateTime.now())) return true;
        if (info.lockedUntil() != null) {
            attempts.remove(ip); // lock expired, clean up
        }
        return false;
    }

    public long getLockedMinutesLeft(String ip) {
        AttemptInfo info = attempts.get(ip);
        if (info == null || info.lockedUntil() == null) return 0;
        long left = java.time.Duration.between(LocalDateTime.now(), info.lockedUntil()).toMinutes() + 1;
        return Math.max(left, 1);
    }

    public void recordFailure(String ip) {
        AttemptInfo current = attempts.getOrDefault(ip, new AttemptInfo(0, null));
        int newCount = current.count() + 1;
        if (newCount >= MAX_ATTEMPTS) {
            attempts.put(ip, new AttemptInfo(0, LocalDateTime.now().plusMinutes(LOCK_MINUTES)));
        } else {
            attempts.put(ip, new AttemptInfo(newCount, null));
        }
    }

    public int getRemainingAttempts(String ip) {
        AttemptInfo info = attempts.getOrDefault(ip, new AttemptInfo(0, null));
        return Math.max(MAX_ATTEMPTS - info.count(), 0);
    }

    public void recordSuccess(String ip) {
        attempts.remove(ip);
    }
}
