package com.edulearn.security;

import com.edulearn.dto.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final List<RateLimitRule> RULES = List.of(
            new RateLimitRule(HttpMethod.POST.name(), "/api/auth/login", 5, Duration.ofMinutes(1)),
            new RateLimitRule(HttpMethod.POST.name(), "/api/auth/register", 5, Duration.ofMinutes(10)),
            new RateLimitRule(HttpMethod.POST.name(), "/api/auth/forgot-password", 3, Duration.ofMinutes(15)),
            new RateLimitRule(HttpMethod.POST.name(), "/api/auth/reset-password", 5, Duration.ofMinutes(15)),
            new RateLimitRule(HttpMethod.PUT.name(), "/api/auth/change-password", 5, Duration.ofMinutes(15))
    );

    private final ObjectMapper objectMapper;

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterAccess(Duration.ofHours(1))
            .build();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return resolveRule(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        RateLimitRule rule = resolveRule(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = buildKey(rule, request);
        Bucket bucket = buckets.get(key, k -> createBucket(rule));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long retryAfterSeconds = Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.error("Too many requests, please try again later")
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitRule resolveRule(HttpServletRequest request) {
        String requestMethod = request.getMethod();
        String requestPath = request.getRequestURI();
        return RULES.stream()
                .filter(rule -> rule.method().equals(requestMethod) && rule.path().equals(requestPath))
                .findFirst()
                .orElse(null);
    }

    private Bucket createBucket(RateLimitRule rule) {
        Refill refill = Refill.intervally(rule.capacity(), rule.duration());
        Bandwidth limit = Bandwidth.classic(rule.capacity(), refill);
        return Bucket.builder().addLimit(limit).build();
    }

    private String buildKey(RateLimitRule rule, HttpServletRequest request) {
        String ip = resolveClientIp(request);
        return rule.method() + ":" + rule.path() + ":" + ip;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private record RateLimitRule(String method, String path, long capacity, Duration duration) {
    }
}
