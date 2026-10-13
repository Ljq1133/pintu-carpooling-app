package com.pintu.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pintu.common.api.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final int maxRequests;
    private final long windowMillis;
    private final Clock clock;
    private final Map<String, Deque<Long>> requests = new ConcurrentHashMap<>();

    @Autowired
    public AuthRateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${security.rate-limit.auth-max-requests:30}") int maxRequests,
            @Value("${security.rate-limit.window-seconds:60}") long windowSeconds) {
        this(objectMapper, maxRequests, windowSeconds, Clock.systemUTC());
    }

    AuthRateLimitFilter(ObjectMapper objectMapper, int maxRequests, long windowSeconds, Clock clock) {
        this.objectMapper = objectMapper;
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long now = clock.millis();
        String key = clientIp(request) + ":" + request.getRequestURI();
        Deque<Long> timestamps = requests.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        boolean allowed;
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= now - windowMillis) {
                timestamps.removeFirst();
            }
            allowed = timestamps.size() < maxRequests;
            if (allowed) {
                timestamps.addLast(now);
            }
        }
        if (!allowed) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", String.valueOf(Math.max(1, windowMillis / 1000)));
            objectMapper.writeValue(response.getWriter(), Result.fail(1016, "Too many requests"));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr()
                : forwarded.split(",")[0].trim();
    }
}
