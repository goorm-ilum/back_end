package com.talktrip.talktrip.global.security;

import com.talktrip.talktrip.global.util.JWTUtil;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Map;
@Getter
@Component
public class JwtProvider {

    // 키를 직접 쓰지 않고, JWTUtil 내부에서 관리하므로 필드는 남겨두지 않아도 됩니다.
    // 필요 시 다른 용도로 사용할 수 있어 보관 형태만 유지합니다.
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
        Object sub = claims.get("sub");
        if (sub == null || String.valueOf(sub).isBlank()) {
            throw new IllegalArgumentException("subject(sub) not found in token");
        }
        return String.valueOf(sub);
    }


}
