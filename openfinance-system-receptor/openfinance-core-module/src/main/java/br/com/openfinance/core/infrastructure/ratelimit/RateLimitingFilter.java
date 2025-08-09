package br.com.openfinance.core.infrastructure.ratelimit;


import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    @Value("${openfinance.api.rate-limit.max-requests-per-minute:1000}")
    private int maxRequestsPerMinute;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String clientId = extractClientId(request);
        Bucket bucket = buckets.computeIfAbsent(clientId, k -> createBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Rate limit exceeded. Please try again later.");
            log.warn("Rate limit exceeded for client: {}", clientId);
        }
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(
                maxRequestsPerMinute,
                Refill.intervally(maxRequestsPerMinute, Duration.ofMinutes(1))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    private String extractClientId(HttpServletRequest request) {
        // Try to get from JWT subject
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Extract client from JWT
            return "jwt-client"; // Simplified - should decode JWT
        }

        // Fall back to IP address
        return request.getRemoteAddr();
    }
}
