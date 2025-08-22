package com.talktrip.talktrip.domain.product.service;

import com.talktrip.talktrip.domain.like.repository.LikeRepository;
import com.talktrip.talktrip.domain.product.dto.response.ProductDetailResponse;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.domain.review.dto.response.ReviewResponse;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.domain.review.repository.ReviewRepository;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.ProductException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final String ALL_COUNTRIES = "전체";
    private static final int DAYS_TO_ADD = 1;

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final LikeRepository likeRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;

    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> searchProducts(
            String keyword,
            String countryName,
            Long memberId,
            Pageable pageable
    ) {
        LocalDate tomorrow = getTomorrow();
        String searchKeyword = normalizeKeyword(keyword);
        String searchCountry = normalizeCountryName(countryName);

        Page<ProductSummaryResponse> searchResults = productRepository.searchProductsWithStock(
                searchKeyword, searchCountry, tomorrow, pageable
        );
        
        if (searchResults.isEmpty()) {
            return searchResults;
        }

        return updateLikeInfo(searchResults, memberId);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(
            Long productId,
            Long memberId,
            Pageable pageable
    ) {
        LocalDate tomorrow = getTomorrow();
        Product product = findProductWithDetailsAndStock(productId, tomorrow);

        Page<Review> reviewPage = reviewRepository.findByProductId(productId, pageable);
        float avgStar = product.getAverageReviewStar();
        List<ReviewResponse> reviewResponses = ReviewResponse.to(reviewPage.getContent(), product);
        boolean isLiked = checkLikeStatus(productId, memberId);

        return ProductDetailResponse.from(product, avgStar, reviewResponses, isLiked);
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> aiSearchProducts(String query, Long memberId) {
        try {
            List<Long> productIds = callAiSearchApi(query);
            
            if (productIds.isEmpty()) {
                return List.of();
            }

            List<ProductSummaryResponse> products = productRepository.findProductSummariesByIds(productIds);
            Set<Long> likedProductIds = getLikedProductIds(memberId, productIds);

            return updateLikeInfoWithOrder(products, productIds, likedProductIds);

        } catch (Exception e) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    private LocalDate getTomorrow() {
        return LocalDate.now().plusDays(DAYS_TO_ADD);
    }

    private String normalizeKeyword(String keyword) {
        return (keyword == null || keyword.isBlank()) ? null : keyword.trim();
    }

    private String normalizeCountryName(String countryName) {
        return (ALL_COUNTRIES.equals(countryName) || countryName == null || countryName.isBlank()) ? null : countryName;
    }

    private Page<ProductSummaryResponse> updateLikeInfo(Page<ProductSummaryResponse> searchResults, Long memberId) {
        if (memberId == null) {
            return searchResults;
        }

        List<Long> productIds = extractProductIds(searchResults);
        Set<Long> likedProductIds = getLikedProductIds(memberId, productIds);
        List<ProductSummaryResponse> updatedContent = updateLikeStatus(searchResults.getContent(), likedProductIds);

        return new PageImpl<>(updatedContent, searchResults.getPageable(), searchResults.getTotalElements());
    }

    private List<Long> extractProductIds(Page<ProductSummaryResponse> searchResults) {
        return searchResults.getContent().stream()
                .map(ProductSummaryResponse::productId)
                .toList();
    }

    private List<ProductSummaryResponse> updateLikeStatus(List<ProductSummaryResponse> products, Set<Long> likedProductIds) {
        return products.stream()
                .map(response -> ProductSummaryResponse.from(response, likedProductIds.contains(response.productId())))
                .toList();
    }

    private Set<Long> getLikedProductIds(Long memberId, List<Long> productIds) {
        if (memberId == null || productIds == null || productIds.isEmpty()) {
            return Set.of();
        }
        return likeRepository.findLikedProductIds(memberId, productIds);
    }

    private Product findProductWithDetailsAndStock(Long productId, LocalDate tomorrow) {
        return productRepository.findByIdWithDetailsAndStock(productId, tomorrow)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private boolean checkLikeStatus(Long productId, Long memberId) {
        if (memberId == null) {
            return false;
        }
        return likeRepository.existsByProductIdAndMemberId(productId, memberId);
    }

    @SuppressWarnings("unchecked")
    private List<Long> callAiSearchApi(String query) {
        String fastApiUrl = fastApiBaseUrl + "/query";
        Map<String, String> requestBody = Map.of("query", query);

        Map<String, Object> responses = restTemplate.postForObject(fastApiUrl, requestBody, Map.class);

        if (responses == null || !responses.containsKey("product_ids")) {
            return List.of();
        }

        return (List<Long>) responses.get("product_ids");
    }

    private List<ProductSummaryResponse> updateLikeInfoWithOrder(
            List<ProductSummaryResponse> products, 
            List<Long> productIds, 
            Set<Long> likedProductIds
    ) {
        Map<Long, Integer> idOrder = createIdOrderMap(productIds);

        return products.stream()
                .sorted(Comparator.comparing(p -> idOrder.getOrDefault(p.productId(), Integer.MAX_VALUE)))
                .map(response -> ProductSummaryResponse.from(response, likedProductIds.contains(response.productId())))
                .toList();
    }

    private Map<Long, Integer> createIdOrderMap(List<Long> productIds) {
        Map<Long, Integer> idOrder = new HashMap<>();
        for (int i = 0; i < productIds.size(); i++) {
            idOrder.put(productIds.get(i), i);
        }
        return idOrder;
    }
}
