package com.sem4.openapispringboot.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
public class JwtTokenService {

    private final SecretKey key;
    private final long expirationMillis = 86400000L; // 24 hours

    public JwtTokenService(@Value("${app.jwt.secret:supersecretkeyforhmacsha256techshopsem4application2026!}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String email, Long customerId, String role) {
        long now = System.currentTimeMillis();
        var builder = Jwts.builder()
            .subject(email)
            .claim("role", role)
            .issuedAt(new Date(now))
            .expiration(new Date(now + expirationMillis));

        if (customerId != null) {
            builder.claim("customerId", customerId);
        }

        return builder.signWith(key).compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
