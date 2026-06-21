package com.cropdeal.adminservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        if (keyBytes.length < 32) throw new IllegalArgumentException("JWT secret must be at least 256 bits");
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) { return extractClaim(token, Claims::getSubject); }
    public String extractRole(String token)     { return extractClaim(token, c -> c.get("role", String.class)); }
    public Long   extractUserId(String token)   { return extractClaim(token, c -> c.get("userId", Long.class)); }

    public boolean isTokenValid(String token) {
        try { return !extractClaim(token, Claims::getExpiration).before(new Date()); }
        catch (JwtException e) { return false; }
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload());
    }
}
