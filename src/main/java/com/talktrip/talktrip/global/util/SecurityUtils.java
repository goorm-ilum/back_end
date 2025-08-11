package com.talktrip.talktrip.global.util;

import com.talktrip.talktrip.domain.member.entity.Member;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Method;

public final class SecurityUtils {
    private SecurityUtils() {}

    public static String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Member m && m.getAccountEmail() != null) {
            return m.getAccountEmail();
        }
        if (principal instanceof UserDetails ud && ud.getUsername() != null) {
            return ud.getUsername();
        }
        if (principal instanceof String s && !s.isBlank()) {
            return s;
        }
        String name = auth.getName();
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("인증 사용자 식별자를 확인할 수 없습니다.");
        }
        return name;
    }

    /**
     * 현재 인증 컨텍스트에서 account_email(이메일)을 추출합니다.
     * - Member: getAccountEmail()
     * - CustomMemberDetails: getEmail() (리플렉션)
     * - UserDetails: getUsername()가 이메일 형태일 경우만 인정
     * - String/Authentication name: 이메일 형태일 경우만 인정
     * 성공적으로 추출하지 못하면 IllegalStateException을 던집니다.
     */
    public static String currentAccountEmail() {
        String email = currentAccountEmailOrNull();
        if (email == null) {
            throw new IllegalStateException("인증된 사용자의 이메일을 확인할 수 없습니다.");
        }
        return email;
    }

    /**
     * 현재 인증 컨텍스트에서 account_email(이메일)을 추출합니다.
     * 실패 시 null을 반환합니다.
     */
    public static String currentAccountEmailOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;

        Object principal = auth.getPrincipal();
        // 1) 도메인 Member
        if (principal instanceof Member m) {
            return toEmailOrNull(m.getAccountEmail());
        }

        // 2) CustomMemberDetails.getEmail() (메서드 존재 시 사용)
        String emailFromCustomDetails = tryInvokeEmailMethod(principal);
        if (emailFromCustomDetails != null) {
            return toEmailOrNull(emailFromCustomDetails);
        }

        // 3) UserDetails.getUsername()가 이메일 형태인 경우
        if (principal instanceof UserDetails ud) {
            String username = ud.getUsername();
            String email = toEmailOrNull(username);
            if (email != null) return email;
        }

        // 4) Principal이 문자열인 경우
        if (principal instanceof String s) {
            String email = toEmailOrNull(s);
            if (email != null) return email;
        }

        // 5) Authentication.getName()이 이메일 형태인 경우
        return toEmailOrNull(auth.getName());
    }

    // getEmail() 메서드가 있으면 호출 시도
    private static String tryInvokeEmailMethod(Object principal) {
        if (principal == null) return null;
        try {
            Method m = principal.getClass().getMethod("getEmail");
            Object val = m.invoke(principal);
            return val != null ? val.toString() : null;
        } catch (NoSuchMethodException e) {
            return null; // getEmail 없음
        } catch (Exception e) {
            return null; // 호출 실패 시 무시
        }
    }

    // 문자열이 이메일 형태로 보이면 trim하여 반환, 아니면 null
    private static String toEmailOrNull(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;
        // 매우 단순한 형태 검증: '@' 포함
        if (v.contains("@")) return v;
        return null;
    }
}