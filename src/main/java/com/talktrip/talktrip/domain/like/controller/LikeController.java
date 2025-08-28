package com.talktrip.talktrip.domain.like.controller;

import com.talktrip.talktrip.domain.like.service.LikeService;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.talktrip.talktrip.global.util.SortUtil.buildSort;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Like", description = "좋아요 관련 API")
public class LikeController {

    private final LikeService likeService;

    @Operation(summary = "상품 좋아요 토글")
    @PostMapping("/products/{productId}/like")
    public ResponseEntity<Void> toggleLike(@PathVariable @NotNull(message = "상품 ID는 필수입니다") Long productId,
                                           @AuthenticationPrincipal CustomMemberDetails memberDetails) {

        likeService.toggleLike(productId, memberDetails.getId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "내 좋아요 상품 목록")
    @GetMapping("/me/likes")
    public ResponseEntity<Page<ProductSummaryResponse>> getMyLikes(
            @AuthenticationPrincipal CustomMemberDetails memberDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "updatedAt,desc") List<String> sort) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        Page<ProductSummaryResponse> result = likeService.getLikedProducts(memberDetails.getId(), pageable);
        return ResponseEntity.ok(result);
    }
}
