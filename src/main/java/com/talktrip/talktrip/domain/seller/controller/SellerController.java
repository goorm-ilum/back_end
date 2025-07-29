package com.talktrip.talktrip.domain.seller.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "유저 관련 API")
@RestController
@RequestMapping("/api/seller")
public class SellerController {

    @Operation(summary = "로그인", description = "카카오 계정으로 로그인합니다.")
    @PostMapping("/login")
    public void login() {}

    @Operation(summary = "로그아웃", description = "로그아웃 처리합니다.")
    @DeleteMapping("/logout")
    public void logout() {}

    @Operation(summary = "내 정보 수정", description = "내 프로필 정보를 수정합니다.")
    @PatchMapping("/me")
    public void updateMyInfo() {}
}
