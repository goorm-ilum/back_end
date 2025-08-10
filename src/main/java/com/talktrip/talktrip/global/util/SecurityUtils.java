package com.talktrip.talktrip.global.util;

import com.talktrip.talktrip.domain.member.entity.Member;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

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
}