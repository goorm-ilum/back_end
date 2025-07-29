package com.talktrip.talktrip.domain.like.service;

import com.talktrip.talktrip.domain.buyer.entity.Buyer;
import com.talktrip.talktrip.domain.buyer.repository.BuyerRepository;
import com.talktrip.talktrip.domain.like.entity.Like;
import com.talktrip.talktrip.domain.like.repository.LikeRepository;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.global.exception.BuyerException;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.ProductException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;
    private final BuyerRepository buyerRepository;

    @Transactional
    public void toggleLike(Long productId, Object principal) {
        //Long buyerId = extractUserId(principal);
        Long buyerId = 1L;

        if (likeRepository.existsByProductIdAndBuyerId(productId, buyerId)) {
            likeRepository.deleteByProductIdAndBuyerId(productId, buyerId);
        } else {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));
            Buyer buyer = buyerRepository.findById(buyerId)
                    .orElseThrow(() -> new BuyerException(ErrorCode.USER_NOT_FOUND));
            likeRepository.save(Like.builder().product(product).buyer(buyer).build());
        }
    }

    public List<ProductSummaryResponse> getLikedProducts(Object principal) {
        //Long buyerId = extractUserId(principal);
        Long buyerId = 1L;

        List<Like> likes = likeRepository.findByBuyerId(buyerId);
        return likes.stream().map(like -> {
            Product product = like.getProduct();
            float avgStar = (float) product.getReviews().stream()
                    .mapToDouble(Review::getReviewStar).average().orElse(0.0);
            return ProductSummaryResponse.from(product, avgStar, true);
        }).toList();
    }
}
