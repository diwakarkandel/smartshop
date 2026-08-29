package com.smartshop.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MILLIS = 60_000L;

    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    public boolean isAllowed(String ip) {
        long now = System.currentTimeMillis();
        Deque<Instant> window = attempts.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst().toEpochMilli() > WINDOW_MILLIS) {
                window.pollFirst();
            }
            if (window.size() >= MAX_ATTEMPTS) {
                return false;
            }
            window.addLast(Instant.ofEpochMilli(now));
            return true;
        }
    }

    public void clear(String ip) {
        attempts.remove(ip);
    }
}