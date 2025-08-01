package com.talktrip.talktrip.domain.member.repository;

import com.talktrip.talktrip.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByAccountEmail(String accountEmail);

}
