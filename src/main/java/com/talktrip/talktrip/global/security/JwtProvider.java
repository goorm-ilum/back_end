package com.talktrip.talktrip.global.security;

import com.talktrip.talktrip.global.util.JWTUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JwtProvider {

    private final String secret;

    public JwtProvider(@Value("${jwt.secret-key}") String secretKey) {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT Secret key is missing or invalid. Please set 'jwt.secret-key'.");
        }
        if (secretKey.length() < 32) {
            throw new IllegalArgumentException("JWT Secret key must be at least 32 characters long.");
        }
        this.secret = secretKey; // JWTUtil이 생성자에서 동일 값을 받아 static으로 보관
    }

    public boolean validateToken(String token) {
        try {
            JWTUtil.validateToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserId(String token) {
        Map<String, Object> claims = JWTUtil.validateToken(token);
        String userId = firstNonBlank(
                toStr(claims.get("sub")),          // 표준
                toStr(claims.get("email")),        // 현재 토큰에 존재
                toStr(claims.get("userId")),
                toStr(claims.get("username")),
                toStr(claims.get("accountEmail"))
        );
        if (userId == null) {
            throw new IllegalArgumentException("subject(sub) not found in token");
        }
        return userId;
    }

    private static String toStr(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o);
        return s.isBlank() ? null : s;
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return null;
    }
}
