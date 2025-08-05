package com.talktrip.talktrip.domain.member.controller;

import com.talktrip.talktrip.domain.member.service.AdminMemberService;
import com.talktrip.talktrip.domain.member.dto.request.MemberUpdateRequestDTO;
import com.talktrip.talktrip.domain.member.dto.response.MemberResponseDTO;
import com.talktrip.talktrip.domain.member.service.MemberService;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Member", description = "회원 관련 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AdminMemberContoller {

    private final AdminMemberService adminmemberService;

    @Operation(summary = "어드민 내 정보 조회", description = "로그인한 사용자의 정보를 반환합니다.")
    @GetMapping("/adminmember/me")
    public ResponseEntity<?> getMyInfo(@AuthenticationPrincipal CustomMemberDetails memberDetails) {
        if (memberDetails == null) {
            return ResponseEntity.status(401).body("인증되지 않은 사용자입니다.");
        }

        Long memberId = memberDetails.getId();
        MemberResponseDTO myInfo = adminmemberService.getMemberInfo(memberId);

        return ResponseEntity.ok(myInfo);
    }

    @Operation(summary = "어드민 내 정보 수정", description = "로그인한 사용자의 정보를 수정합니다.")
    @PutMapping(value = "/adminmember/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateMyInfo(
            @AuthenticationPrincipal CustomMemberDetails memberDetails,
            @RequestPart("info") MemberUpdateRequestDTO updateDTO,
            @RequestPart(value = "profile_image", required = false) MultipartFile profileImage
    ) {
        if (memberDetails == null) {
            return ResponseEntity.status(401).body("인증되지 않은 사용자입니다.");
        }

        Long memberId = memberDetails.getId();
        adminmemberService.updateMemberProfile(memberId, updateDTO, profileImage);
        return ResponseEntity.ok("프로필이 성공적으로 수정되었습니다.");
    }
}
