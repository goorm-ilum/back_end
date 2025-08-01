package com.talktrip.talktrip.global.security;

import com.talktrip.talktrip.domain.member.entity.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record CustomMemberDetails(Member member) implements UserDetails {

    public Long getId() {
        return member.getId();
    }

    public String getEmail() {
        return member.getAccountEmail();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(member.getMemberRole().name()));
    }

    @Override
    public String getPassword() {
        return null;  // 소셜 로그인이라 password 없음
    }

    @Override
    public String getUsername() {
        return member.getAccountEmail();  // 이메일로 변경
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
