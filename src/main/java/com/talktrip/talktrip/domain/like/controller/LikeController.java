package com.talktrip.talktrip.domain.like.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Like", description = "좋아요 관련 API")
@RestController
@RequestMapping("/api")
public class LikeController {

    @Operation(summary = "상품 좋아요 클릭")
    @PostMapping("/products/{productId}/like")
    public void likeProduct(@PathVariable Long productId) {
    }

    @Operation(summary = "내 좋아요 상품 목록")
    @GetMapping("/me/likes")
    public void getMyLikes() {
    }
}
