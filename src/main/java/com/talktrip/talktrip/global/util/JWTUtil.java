package com.talktrip.talktrip.global.util;

import javax.crypto.SecretKey;
import io.jsonwebtoken.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

@Log4j2
@Component
public class JWTUtil {

    private static SecretKey secretKey;

    //secret-key 주입
    public JWTUtil(@Value("${jwt.secret-key}") String key) {
        try{this.secretKey = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));}
        catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static String generateToken(Map<String, Object> valueMap, int min) {
        try {
            return Jwts.builder()
                    .setHeader(Map.of("typ", "JWT"))
                    .setClaims(valueMap)
                    .setIssuedAt(Date.from(ZonedDateTime.now().toInstant()))
                    .setExpiration(Date.from(ZonedDateTime.now().plusMinutes(min).toInstant()))
                    .signWith(secretKey)
                    .compact();
        } catch (Exception e) {
            log.error("Error generating JWT token: {}", e.getMessage());
            throw new RuntimeException("Error generating token", e);
        }
    }

    public static Map<String, Object> validateToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token) // 파싱 및 검증
                    .getBody();
        } catch (MalformedJwtException malformedJwtException) {
            throw new CustomJWTException("MalFormed", malformedJwtException);
        } catch (ExpiredJwtException expiredJwtException) {
            throw new CustomJWTException("Expired", expiredJwtException);
        } catch (InvalidClaimException invalidClaimException) {
            throw new CustomJWTException("Invalid", invalidClaimException);
        } catch (JwtException jwtException) {
            throw new CustomJWTException("JWTError", jwtException);
        } catch (Exception e) {
            throw new CustomJWTException("Error", e);
        }
    }
}


