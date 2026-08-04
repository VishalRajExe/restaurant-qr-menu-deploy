package com.restaurantqr.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP-based rate limiter using Bucket4j token-bucket algorithm.
 *
 * Limits:
 *  - /auth/**  →  20 requests / minute  (brute-force protection)
 *  - /public/** → 120 requests / minute (customer menu scans)
 *  - everything else → no limit (protected by JWT anyway)
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    // One bucket per IP per endpoint group
    private final Map<String, Bucket> authBuckets   = new ConcurrentHashMap<>();
    private final Map<String, Bucket> publicBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip   = getClientIp(request);

        Bucket bucket = null;

        if (path.startsWith("/api/v1/auth/")) {
            bucket = authBuckets.computeIfAbsent(ip, k -> newBucket(20, Duration.ofMinutes(1)));
        } else if (path.startsWith("/api/v1/public/")) {
            bucket = publicBuckets.computeIfAbsent(ip, k -> newBucket(120, Duration.ofMinutes(1)));
        }

        if (bucket != null && !bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for IP={} path={}", ip, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"success":false,"message":"Too many requests. Please slow down and try again later."}
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Bucket newBucket(int capacity, Duration refillPeriod) {
        var refill    = Refill.greedy(capacity, refillPeriod);
        var bandwidth = Bandwidth.classic(capacity, refill);
        return Bucket.builder().addLimit(bandwidth).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP"};
        for (String h : headers) {
            String ip = request.getHeader(h);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
