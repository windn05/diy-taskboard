package com.taskboard.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** JWT 발급·검증 (HMAC 서명). 만료 시간은 application.yml의 jwt.* 설정 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    /** 토큰 용도. 클레임으로 박아 두고 검증 시 확인 — 리프레시 토큰을 API 호출에 쓰는 것을 차단. */
    public enum TokenType { ACCESS, REFRESH }

    private static final String TYPE_CLAIM = "type";

    public String generateAccessToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, TokenType.ACCESS, accessTokenExpirationMs);
    }

    public String generateRefreshToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, TokenType.REFRESH, refreshTokenExpirationMs);
    }

    private String generateToken(Long userId, String username, String role, TokenType type, long expirationMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role)
                .claim(TYPE_CLAIM, type.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /** 서명·만료·용도 검증. 실패 사유는 구분하지 않음. 용도 클레임이 없는 예전 토큰도 거부. */
    public boolean isValid(String token, TokenType expected) {
        try {
            return expected.name().equals(parseClaims(token).get(TYPE_CLAIM, String.class));
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return parseClaims(token).get("userId", Long.class);
    }
}
