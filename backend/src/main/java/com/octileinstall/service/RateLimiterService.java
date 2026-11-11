package com.octileinstall.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A simple in-memory, fixed-window rate limiter keyed by an arbitrary string (currently: the
 * caller's IP address, for the public inquiry endpoint - see PublicInquiryController).
 *
 * <p><b>Single-instance only.</b> State lives in this process's heap, not in a shared store.
 * That is fine for this application today (one Spring Boot instance, one MySQL, no load
 * balancer), but if this ever runs behind more than one instance, each instance would track
 * its own counters and the effective limit would multiply by the instance count. Move to a
 * shared store (e.g. Redis) at that point instead of trusting this class.
 *
 * <p>Fixed windows (not sliding) are a deliberate simplification: a caller could in theory
 * send {@code maxRequests} right before a window boundary and another {@code maxRequests}
 * right after, briefly exceeding the nominal rate. That's an acceptable trade for a form that
 * gets, at most, a handful of legitimate submissions per visitor - not a payment or auth
 * endpoint where that slack would matter.
 */
@Service
public class RateLimiterService {

    private final int maxRequests;
    private final long windowMillis;

    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiterService(
            @Value("${app.public-inquiry.rate-limit.max-requests:5}") int maxRequests,
            @Value("${app.public-inquiry.rate-limit.window-minutes:15}") long windowMinutes) {
        this.maxRequests = maxRequests;
        this.windowMillis = Duration.ofMinutes(windowMinutes).toMillis();
    }

    /**
     * Records one attempt for {@code key} and reports whether it's within the limit.
     *
     * @return {@code true} if the caller is still under the limit for the current window
     *         (the attempt is counted either way), {@code false} if they've exceeded it
     */
    public boolean tryAcquire(String key) {
        Window window = windows.computeIfAbsent(key, k -> new Window(now()));

        synchronized (window) {
            if (now() - window.startMillis >= windowMillis) {
                window.startMillis = now();
                window.count = 0;
            }
            if (window.count >= maxRequests) {
                return false;
            }
            window.count++;
            return true;
        }
    }

    /**
     * Sweeps windows that expired at least one full window ago, so a caller who hit the
     * endpoint once and never returned doesn't sit in the map forever. Runs independently of
     * any request - callers that are actively rate-limited get reset the next time they try,
     * regardless of this sweep.
     */
    @Scheduled(fixedRateString = "${app.public-inquiry.rate-limit.cleanup-interval-ms:1800000}")
    void evictExpiredWindows() {
        long cutoff = now() - windowMillis;
        windows.entrySet().removeIf(entry -> entry.getValue().startMillis < cutoff);
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static final class Window {
        private volatile long startMillis;
        private int count;

        private Window(long startMillis) {
            this.startMillis = startMillis;
        }
    }
}
