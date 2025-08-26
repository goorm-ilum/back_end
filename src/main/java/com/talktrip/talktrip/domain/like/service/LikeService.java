package com.talktrip.talktrip.domain.like.service;

import com.talktrip.talktrip.domain.like.dto.ProductWithAvgStar;
import com.talktrip.talktrip.domain.like.entity.Like;
import com.talktrip.talktrip.domain.like.repository.LikeRepository;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.MemberException;
import com.talktrip.talktrip.global.exception.ProductException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void toggleLike(Long productId, Long memberId) {
        validateMember(memberId);
        validateProduct(productId);
        
        if (isLiked(productId, memberId)) {
            unlikeProduct(productId, memberId);
        } else {
            likeProduct(productId, memberId);
        }
    }

    private void validateMember(Long memberId) {
        if (memberId == null) {
            throw new MemberException(ErrorCode.USER_NOT_FOUND);
        }

        if (!memberRepository.existsById(memberId)) {
            throw new MemberException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validateProduct(Long productId) {
        if (productId == null) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        
        if (!productRepository.existsById(productId)) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    private boolean isLiked(Long productId, Long memberId) {
        return likeRepository.existsByProductIdAndMemberId(productId, memberId);
    }

    private void unlikeProduct(Long productId, Long memberId) {
        likeRepository.deleteByProductIdAndMemberId(productId, memberId);
    }

    private void likeProduct(Long productId, Long memberId) {
        likeRepository.save(Like.builder()
                .productId(productId)
                .memberId(memberId)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> getLikedProducts(Long memberId, Pageable pageable) {
        validateMember(memberId);
        
        Page<ProductWithAvgStar> likedProducts = likeRepository.findLikedProductsWithAvgStar(memberId, pageable);
        
        List<ProductSummaryResponse> productResponses = likedProducts.getContent().stream()
                .map(result -> {
                    Product product = result.getProduct();
                    Double avgStar = result.getAvgStar();
                    return ProductSummaryResponse.from(product, avgStar, true);
                })
                .toList();
        
        return new PageImpl<>(productResponses, pageable, likedProducts.getTotalElements());
    }
}
