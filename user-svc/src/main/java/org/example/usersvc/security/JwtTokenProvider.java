package org.example.usersvc.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.example.usersvc.config.JwtProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private Key signingKey;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(jwtProperties.getSecret())) {
            throw new IllegalStateException("JWT secret is empty.");
        }
        byte[] secretBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public TokenInfo generateToken(Long userId, String username) {
        Instant issuedAt = Instant.now();
        Instant expireAt = issuedAt.plusSeconds(jwtProperties.getExpireSeconds());

        String token = Jwts.builder()
                .subject(username)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expireAt))
                .claim("uid", userId)
                .signWith(signingKey)
                .compact();

        LocalDateTime expireDateTime = LocalDateTime.ofInstant(expireAt, ZoneId.systemDefault());
        return new TokenInfo(token, expireDateTime);
    }

    public record TokenInfo(String token, LocalDateTime expireAt) {
    }
}
