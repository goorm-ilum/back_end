package com.talktrip.talktrip.domain.product.service;

import com.talktrip.talktrip.domain.like.repository.LikeRepository;
import com.talktrip.talktrip.domain.product.dto.response.ProductDetailResponse;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductStock;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.domain.review.dto.response.ReviewResponse;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.domain.review.repository.ReviewRepository;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.ProductException;
import com.talktrip.talktrip.global.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final LikeRepository likeRepository;

    @Transactional
    public List<ProductSummaryResponse> searchProducts(String keyword, Long memberId, int page, int size) {
        int offset = page * size;
        List<Product> products;

        if (keyword == null || keyword.trim().isEmpty()) {
            products = productRepository.findAll(PageRequest.of(page, size)).getContent();
        } else {
            List<String> keywords = Arrays.stream(keyword.trim().split("\\s+")).toList();
            products = productRepository.searchByKeywords(keywords, offset, size);
        }

        return products.stream()
                .filter(product -> product.getProductStocks().stream()
                        .mapToInt(ProductStock::getStock)
                        .sum() > 0)
                .map(product -> {
                    List<Review> allReviews = reviewRepository.findByProductId(product.getId());
                    float avgStar = (float) allReviews.stream()
                            .mapToDouble(Review::getReviewStar)
                            .average()
                            .orElse(0.0);

                    boolean isLiked = memberId != null &&
                            likeRepository.existsByProductIdAndMemberId(product.getId(), memberId);

                    return ProductSummaryResponse.from(product, avgStar, isLiked);
                })
                .toList();
    }


    @Transactional
    public ProductDetailResponse getProductDetail(Long productId, Long memberId, int page, int size) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        int totalStock = product.getProductStocks().stream()
                .mapToInt(ProductStock::getStock)
                .sum();
        if (totalStock == 0) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        List<Review> reviews = reviewRepository.findByProductId(productId);
        float avgStar = (float) reviews.stream()
                .mapToDouble(Review::getReviewStar).average().orElse(0.0);

        List<ReviewResponse> reviewResponses = reviews.stream()
                .map(ReviewResponse::from)
                .toList();

        List<ReviewResponse> pagedReviews = PaginationUtil.paginate(reviewResponses, page, size);

        boolean isLiked = memberId != null && likeRepository.existsByProductIdAndMemberId(productId, memberId);

        return ProductDetailResponse.from(product, avgStar, pagedReviews, isLiked);
    }
}
