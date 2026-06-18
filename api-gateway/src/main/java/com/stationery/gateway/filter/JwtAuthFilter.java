package com.stationery.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Global JWT Authentication Filter for the API Gateway.
 * <p>
 * This filter intercepts all incoming requests and performs JWT validation
 * on protected routes. It extracts user identity (username and role) from
 * valid tokens and propagates them as headers to downstream microservices.
 * </p>
 *
 * <h3>Behavior:</h3>
 * <ul>
 *   <li>Skips authentication for public paths ({@code /api/auth/**})</li>
 *   <li>Extracts Bearer token from the Authorization header</li>
 *   <li>Validates the JWT signature and expiration</li>
 *   <li>Adds {@code X-User-Name} and {@code X-User-Role} headers to downstream requests</li>
 *   <li>Returns HTTP 401 Unauthorized for invalid or missing tokens</li>
 * </ul>
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_USER_NAME = "X-User-Name";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    /** Public paths that bypass JWT authentication. */
    private static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/validate"
    );

    @Value("${jwt.secret:stationeryManagementSecretKey2024StationeryApp}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip authentication for public endpoints
        if (isOpenEndpoint(path)) {
            logger.debug("Skipping JWT validation for open endpoint: {}", path);
            return chain.filter(exchange);
        }

        // Check for Authorization header
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            logger.warn("Missing Authorization header for path: {}", path);
            return onUnauthorized(exchange, "Missing Authorization header");
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            logger.warn("Invalid Authorization header format for path: {}", path);
            return onUnauthorized(exchange, "Invalid Authorization header format");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            Claims claims = extractClaims(token);
            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            if (username == null || username.isBlank()) {
                logger.warn("JWT token has no subject for path: {}", path);
                return onUnauthorized(exchange, "Invalid token: missing subject");
            }

            // Mutate the request to add user identity headers for downstream services
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(HEADER_USER_NAME, username)
                    .header(HEADER_USER_ROLE, role != null ? role : "ROLE_USER")
                    .build();

            logger.debug("JWT validated for user '{}' with role '{}' on path: {}",
                    username, role, path);

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            logger.error("JWT validation failed for path {}: {}", path, e.getMessage());
            return onUnauthorized(exchange, "Invalid or expired token");
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    /**
     * Checks whether the given path matches any of the open (public) API endpoints.
     *
     * @param path the request URI path
     * @return {@code true} if the path is public and should skip authentication
     */
    private boolean isOpenEndpoint(String path) {
        return OPEN_API_ENDPOINTS.stream()
                .anyMatch(endpoint -> path.startsWith(endpoint.replace("/**", "")));
    }

    /**
     * Extracts JWT claims by parsing the token with the configured secret key.
     *
     * @param token the JWT token string
     * @return the parsed claims
     */
    private Claims extractClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Completes the response with HTTP 401 Unauthorized status.
     *
     * @param exchange the current server web exchange
     * @param message  the reason for the unauthorized response (logged only)
     * @return a completed {@code Mono<Void>}
     */
    private Mono<Void> onUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        logger.debug("Returning 401 Unauthorized: {}", message);
        return response.setComplete();
    }
}
