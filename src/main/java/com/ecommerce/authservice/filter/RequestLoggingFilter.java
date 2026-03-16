package com.ecommerce.authservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet request/response logging filter for Auth Service.
 *
 * <p>Logs one structured line on request arrival and one on completion:
 * <pre>
 *   →  [CID:abc] INCOMING  POST /auth/register  ip=127.0.0.1
 *   ←  [CID:abc] COMPLETED POST /auth/register  STATUS:201  TIME:38ms  USER:anonymous
 * </pre>
 *
 * <p>Order {@code HIGHEST_PRECEDENCE + 1} guarantees this runs immediately after
 * {@link CorrelationIdFilter} (which has {@code HIGHEST_PRECEDENCE}) so the
 * MDC {@code correlationId} is already populated when we read it here.
 *
 * <p>Actuator health and Swagger endpoints are excluded to avoid log noise.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        long startTime  = System.currentTimeMillis();
        String method   = request.getMethod();
        String path     = request.getServletPath();
        String cid      = resolveCid(request);
        String clientIp = resolveClientIp(request);

        log.info("→  [CID:{}] INCOMING  {} {}  ip={}", cid, method, path, clientIp);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            int status      = response.getStatus();
            String userId   = resolveUserId(request);
            log.info("←  [CID:{}] COMPLETED {} {}  STATUS:{}  TIME:{}ms  USER:{}",
                    cid, method, path, status, durationMs, userId);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveCid(HttpServletRequest request) {
        // CorrelationIdFilter already placed it in request attribute
        Object attr = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        if (attr instanceof String s && !s.isBlank()) return s;
        String header = request.getHeader(CorrelationIdFilter.HEADER_NAME);
        return header != null ? header : "n/a";
    }

    private String resolveUserId(HttpServletRequest request) {
        // Injected as a header by the API Gateway's JwtAuthenticationFilter
        String userId = request.getHeader("X-User-Id");
        return (userId != null && !userId.isBlank()) ? userId : "anonymous";
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
