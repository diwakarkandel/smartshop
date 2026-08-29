package com.smartshop.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${app.jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(UUID userId, String email, List<String> roleNames,
                                      List<BranchRoleGrant> grants) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roleNames)
                .claim("branchRoles", grants.stream().map(g -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("branchId", g.branchId() == null ? null : g.branchId().toString());
                    m.put("shopId", g.shopId() == null ? null : g.shopId().toString());
                    m.put("role", g.roleName());
                    return m;
                }).toList())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiration))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenExpiration))
                .signWith(key)
                .compact();
    }

    public String getJtiFromToken(String token) {
        return parse(token).getId();
    }

    public Date getExpirationDateFromToken(String token) {
        return parse(token).getExpiration();
    }

    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(parse(token).getSubject());
    }

    public String getEmailFromToken(String token) {
        return parse(token).get("email", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<BranchRoleGrant> getBranchRoleGrantsFromToken(String token) {
        Claims claims = parse(token);
        Object raw = claims.get("branchRoles");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(item -> {
            Map<String, Object> m = (Map<String, Object>) item;
            String branchId = (String) m.get("branchId");
            String shopId = (String) m.get("shopId");
            return new BranchRoleGrant(
                    branchId == null ? null : UUID.fromString(branchId),
                    shopId == null ? null : UUID.fromString(shopId),
                    (String) m.get("role"));
        }).toList();
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}