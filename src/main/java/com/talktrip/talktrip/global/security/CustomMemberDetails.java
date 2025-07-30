package com.talktrip.talktrip.global.security;

import com.talktrip.talktrip.domain.member.entity.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public record CustomMemberDetails(Member member) implements UserDetails {

    public Long getId() {
        return member.getId();
    }

    public String getEmail() {
        return member.getAccountEmail();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return member.getName();
    }
}
