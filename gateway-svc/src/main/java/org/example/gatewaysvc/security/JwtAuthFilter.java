package org.example.gatewaysvc.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ClaimJwtException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.example.gatewaysvc.config.JwtProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String USER_PATH_PREFIX = "/api/users/";
    private static final String USER_ADMIN_PATH_PREFIX = "/api/users/admin";
    private static final String SOCIAL_PATH_PREFIX = "/api/social/";
    private static final String SOCIAL_ADMIN_PATH_PREFIX = "/api/social/admin/";
    private static final String MESSAGE_PATH_PREFIX = "/api/message/";
    private static final String TRADING_PATH_PREFIX = "/api/trading/";
    private static final Set<String> WHITELIST = Set.of(
            "/api/users/auth/login",
            "/api/users/auth/register",
            "/api/social/content/list",
            "/api/social/categories",
            "/api/social/content-tags"
    );

    private static final String AUTHORIZATION = "Authorization";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USERNAME = "X-Username";

    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(jwtProperties.getSecret())) {
            throw new IllegalStateException("gateway jwt.secret is empty.");
        }
        byte[] bytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("gateway jwt.secret must be at least 32 bytes.");
        }
        this.signingKey = Keys.hmacShaKeyFor(bytes);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!StringUtils.hasText(path)) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (path.startsWith(USER_ADMIN_PATH_PREFIX)
                || "/api/social/admin".equals(path)
                || path.startsWith(SOCIAL_ADMIN_PATH_PREFIX)) {
            return true;
        }
        boolean protectedPath = path.startsWith(USER_PATH_PREFIX)
                || path.startsWith(MESSAGE_PATH_PREFIX)
                || path.startsWith(SOCIAL_PATH_PREFIX)
                || path.startsWith(TRADING_PATH_PREFIX);
        if (!protectedPath) {
            return true;
        }
        return WHITELIST.contains(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String authorization = request.getHeader(AUTHORIZATION);
            if (!StringUtils.hasText(authorization)) {
                if (allowUserIdHeaderFallback(request)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                writeUnauthorized(response, "missing Authorization header");
                return;
            }

            String token = extractToken(authorization);
            if (!StringUtils.hasText(token)) {
                writeUnauthorized(response, "invalid Authorization header");
                return;
            }

            Claims claims = parseClaims(token);
            Long userId = parseUserId(claims.get("uid"));
            if (userId == null || userId <= 0) {
                writeUnauthorized(response, "invalid user id in token");
                return;
            }

            MutableHttpServletRequest mutable = new MutableHttpServletRequest(request);
            mutable.putHeader(HEADER_USER_ID, String.valueOf(userId));
            if (StringUtils.hasText(claims.getSubject())) {
                mutable.putHeader(HEADER_USERNAME, claims.getSubject());
            }

            filterChain.doFilter(mutable, response);
        } catch (JwtException | IllegalArgumentException ex) {
            writeUnauthorized(response, "invalid token");
        }
    }

    private boolean allowUserIdHeaderFallback(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!StringUtils.hasText(path) || !path.startsWith(USER_PATH_PREFIX) || WHITELIST.contains(path)) {
            return false;
        }
        String userIdHeader = request.getHeader(HEADER_USER_ID);
        Long userId = parseUserId(userIdHeader);
        return userId != null && userId > 0;
    }

    private String extractToken(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        String value = authorization.trim();
        if (!StringUtils.hasText(value)) {
            return null;
        }

        if (value.regionMatches(true, 0, "Bearer", 0, "Bearer".length())) {
            String token = value.substring("Bearer".length()).trim();
            return StringUtils.hasText(token) ? token : null;
        }
        if (value.chars().filter(ch -> ch == '.').count() == 2) {
            return value;
        }
        return null;
    }

    private Claims parseClaims(String token) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ClaimJwtException ex) {
            // Ignore exp/nbf time-window checks at gateway and continue with verified claims.
            claims = ex.getClaims();
            if (claims == null) {
                throw ex;
            }
        }

        if (StringUtils.hasText(jwtProperties.getIssuer())
                && !jwtProperties.getIssuer().equals(claims.getIssuer())) {
            throw new JwtException("issuer mismatch");
        }
        return claims;
    }

    private Long parseUserId(Object uid) {
        if (uid == null) {
            return null;
        }
        if (uid instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(uid.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 401);
        body.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
