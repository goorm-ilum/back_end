package com.talktrip.talktrip.domain.like.service;

import com.talktrip.talktrip.domain.like.entity.Like;
import com.talktrip.talktrip.domain.like.repository.LikeRepository;
import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.MemberException;
import com.talktrip.talktrip.global.exception.ProductException;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void toggleLike(Long productId, CustomMemberDetails memberDetails) {
        if (memberDetails == null) {
            throw new MemberException(ErrorCode.UNAUTHORIZED_MEMBER);
        }

        Long memberId = memberDetails.getId();

        if (likeRepository.existsByProductIdAndMemberId(productId, memberId)) {
            likeRepository.deleteByProductIdAndMemberId(productId, memberId);
        } else {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new MemberException(ErrorCode.USER_NOT_FOUND));

            likeRepository.save(Like.builder()
                    .product(product)
                    .member(member)
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getLikedProducts(CustomMemberDetails memberDetails, int page, int size) {
        if (memberDetails == null) {
            throw new MemberException(ErrorCode.UNAUTHORIZED_MEMBER);
        }

        Long memberId = memberDetails.getId();
        List<Like> likes = likeRepository.findByMemberId(memberId);

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, likes.size());

        if (fromIndex >= likes.size()) {
            return List.of();
        }

        return likes.subList(fromIndex, toIndex).stream().map(like -> {
            Product product = like.getProduct();
            float avgStar = (float) product.getReviews().stream()
                    .mapToDouble(Review::getReviewStar)
                    .average()
                    .orElse(0.0);

            return ProductSummaryResponse.from(product, avgStar, true);
        }).toList();
    }

}
