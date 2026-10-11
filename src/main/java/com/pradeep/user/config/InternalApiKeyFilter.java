package com.pradeep.user.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Authenticates trusted backend callers of the internal User Service API.
 * The expected key is supplied only through USER_SERVICE_API_KEY configuration.
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    static final String HEADER_NAME = "X-Internal-Api-Key";

    private final byte[] expectedApiKey;

    public InternalApiKeyFilter(@Value("${security.internal-api-key:}") String apiKey) {
        this.expectedApiKey = apiKey == null ? new byte[0] : apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (expectedApiKey.length == 0) {
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Internal API authentication is not configured");
            return;
        }

        String suppliedApiKey = request.getHeader(HEADER_NAME);
        boolean valid = suppliedApiKey != null && MessageDigest.isEqual(
                expectedApiKey,
                suppliedApiKey.getBytes(StandardCharsets.UTF_8)
        );
        if (!valid) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Internal API authentication is required");
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "internal-service",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
