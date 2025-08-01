package com.talktrip.talktrip.domain.member.service;

import com.talktrip.talktrip.domain.member.dto.request.MemberUpdateRequestDTO;
import com.talktrip.talktrip.domain.member.dto.response.MemberResponseDTO;
import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.global.s3.S3Uploader;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final S3Uploader s3Uploader;

    @Transactional
    public void updateMemberProfile(Long memberId, MemberUpdateRequestDTO dto, MultipartFile profileImage) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."));

        member.updateInfo(dto.getName(), dto.getGender(), dto.getBirthday(), dto.getPhoneNum());

        if (profileImage != null && !profileImage.isEmpty()) {
            String imageUrl = s3Uploader.upload(profileImage, "profile");
            member.updateProfileImage(imageUrl);
        }
    }

    public MemberResponseDTO getMemberInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."));

        return MemberResponseDTO.from(member); // 아래에 설명
    }
}

