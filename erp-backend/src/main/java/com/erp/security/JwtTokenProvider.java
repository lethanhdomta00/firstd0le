package com.erp.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    @Value("${app.jwtExpirationInMs:86400000}")
    private int jwtExpirationInMs;

    private SecretKey getSigningKey() {
        try {
            // Dùng SHA-512 băm secret thành đúng chuẩn 64 bytes (512 bits) an toàn tuyệt đối
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] keyBytes = md.digest(jwtSecret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            // Fallback sang bytes trực tiếp
            byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            return Keys.hmacShaKeyFor(keyBytes);
        }
    }

    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token đã hết hạn: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("JWT token không được hỗ trợ: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("JWT token sai định dạng: {}", ex.getMessage());
        } catch (io.jsonwebtoken.security.SecurityException ex) {
            log.warn("JWT chữ ký hoặc bảo mật không hợp lệ: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims rỗng hoặc null: {}", ex.getMessage());
        } catch (Exception ex) {
            log.warn("Lỗi xác thực JWT token: {}", ex.getMessage());
        }
        return false;
    }
}
