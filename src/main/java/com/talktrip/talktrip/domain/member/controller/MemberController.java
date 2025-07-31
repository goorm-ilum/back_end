package com.talktrip.talktrip.domain.member.controller;

import com.talktrip.talktrip.global.util.JWTUtil;
import com.talktrip.talktrip.domain.member.dto.response.MemberResponseDTO;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.member.service.KakaoAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "회원", description = "회원 관련 API")
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final KakaoAuthService kakaoAuthService;
    private final MemberRepository memberRepository;

    @Operation(summary = "카카오 로그인 URL 요청", description = "카카오 로그인 인가 URL을 반환합니다.")
    @GetMapping("/kakao-login-url")
    public ResponseEntity<?> getKakaoLoginUrl() {
        String kakaoUrl = kakaoAuthService.getKakaoAuthorizeUrl();
        return ResponseEntity.ok(Map.of("url", kakaoUrl));
    }

    @Operation(summary = "카카오 로그인 콜백", description = "인가 코드를 통해 로그인 처리를 수행합니다.")
    @PostMapping("/kakao")
    public Map<String, Object> kakaoLogin(String code) {

        MemberResponseDTO memberDTO = kakaoAuthService.loginWithKakao(code);

        Map<String, Object> claims = memberDTO.getClaims();

        String accessToken = JWTUtil.generateToken(claims, 60 * 24);
        String refreshToken = JWTUtil.generateToken(claims, 60 * 24 * 30);

        claims.put("accessToken", accessToken);
        claims.put("refreshToken", refreshToken);

        return claims;
    }
}