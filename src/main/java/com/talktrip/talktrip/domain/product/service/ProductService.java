package com.talktrip.talktrip.domain.product.service;

import com.talktrip.talktrip.domain.like.repository.LikeRepository;
import com.talktrip.talktrip.domain.product.dto.response.ProductDetailResponse;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.domain.review.dto.response.ReviewResponse;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.domain.review.repository.ReviewRepository;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.ProductException;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import com.talktrip.talktrip.global.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final LikeRepository likeRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;

    @Transactional
    public List<ProductSummaryResponse> searchProducts(String keyword, CustomMemberDetails memberDetails, int page, int size) {
        List<Product> products;

        if (keyword == null || keyword.trim().isEmpty()) {
            products = productRepository.findAll();
        } else {
            List<String> keywords = Arrays.stream(keyword.trim().split("\\s+")).toList();
            List<Product> candidates = productRepository.searchByKeywords(keywords, 0, Integer.MAX_VALUE);

            products = candidates.stream()
                    .filter(product -> {
                        String combined = (product.getProductName() + " " + product.getDescription()).toLowerCase();
                        List<String> combinedWords = Arrays.asList(combined.split("\\s+"));

                        for (String k : keywords) {
                            long count = combinedWords.stream().filter(word -> word.equals(k.toLowerCase())).count();
                            long required = keywords.stream().filter(s -> s.equalsIgnoreCase(k)).count();
                            if (count < required) return false;
                        }
                        return true;
                    })
                    .sorted(Comparator.comparing(Product::getUpdatedAt).reversed()) // 정렬 (예: 최신순)
                    .toList();
        }

        int offset = page * size;
        int toIndex = Math.min(offset + size, products.size());

        List<Product> pagedProducts = (offset > products.size()) ? List.of() : products.subList(offset, toIndex);

        return pagedProducts.stream()
                .filter(product -> product.getProductOptions().stream()
                        .mapToInt(ProductOption::getStock)
                        .sum() > 0)
                .map(product -> {
                    List<Review> allReviews = reviewRepository.findByProductId(product.getId());
                    float avgStar = (float) allReviews.stream()
                            .mapToDouble(Review::getReviewStar)
                            .average()
                            .orElse(0.0);

                    boolean isLiked = false;
                    if (memberDetails != null) {
                        isLiked = likeRepository.existsByProductIdAndMemberId(product.getId(), memberDetails.getId());
                    }

                    return ProductSummaryResponse.from(product, avgStar, isLiked);
                })
                .toList();
    }

    @Transactional
    public ProductDetailResponse getProductDetail(Long productId, CustomMemberDetails memberDetails, int page, int size) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        int futureStock = product.getProductOptions().stream()
                .filter(option -> !option.getStartDate().isBefore(LocalDate.now()))
                .mapToInt(ProductOption::getStock)
                .sum();

        if (futureStock == 0) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        List<Review> reviews = reviewRepository.findByProductId(productId);
        float avgStar = (float) reviews.stream()
                .mapToDouble(Review::getReviewStar)
                .average()
                .orElse(0.0);

        List<ReviewResponse> reviewResponses = reviews.stream()
                .map(ReviewResponse::from)
                .toList();

        List<ReviewResponse> pagedReviews = PaginationUtil.paginate(reviewResponses, page, size);

        boolean isLiked = false;
        if (memberDetails != null) {
            isLiked = likeRepository.existsByProductIdAndMemberId(productId, memberDetails.getId());
        }

        return ProductDetailResponse.from(product, avgStar, pagedReviews, isLiked);
    }


    public List<ProductSummaryResponse> aiSearchProducts(String query) {
        try {
            String fastApiUrl = fastApiBaseUrl + "/query";
            
            Map<String, String> requestBody = Map.of("query", query);
            
            // FastAPI에서 상품 ID 목록 받기
            Map<String, Object> response = restTemplate.postForObject(
                    fastApiUrl,
                    requestBody,
                    Map.class
            );
            
            if (response == null || !response.containsKey("product_ids")) {
                return List.of();
            }
            
            // 안전한 타입 변환
            Object productIdsObj = response.get("product_ids");
            List<String> productIdStrings;
            
            if (productIdsObj instanceof List<?>) {
                productIdStrings = ((List<?>) productIdsObj).stream()
                        .map(Object::toString)
                        .toList();
            } else {
                return List.of();
            }
            

            
            if (productIdStrings.isEmpty()) {
                return List.of();
            }
            
            // 문자열 ID를 Long으로 변환하고 상품 정보 조회
            List<Long> productIds = productIdStrings.stream()
                    .map(Long::parseLong)
                    .toList();
            
            List<Product> products = productRepository.findAllById(productIds);
            
            // 상품 ID 순서대로 정렬
            Map<Long, Integer> idOrder = new HashMap<>();
            for (int i = 0; i < productIds.size(); i++) {
                idOrder.put(productIds.get(i), i);
            }
            
            List<ProductSummaryResponse> result = products.stream()
                    .sorted(Comparator.comparing(product -> idOrder.get(product.getId())))
                    .map(product -> {
                        List<Review> allReviews = reviewRepository.findByProductId(product.getId());
                        float avgStar = (float) allReviews.stream()
                                .mapToDouble(Review::getReviewStar)
                                .average()
                                .orElse(0.0);
                        
                        return ProductSummaryResponse.from(product, avgStar, false);
                    })
                    .toList();
            
            return result;
                    
        } catch (Exception e) {
            // AI 검색 실패 시 일반 검색으로 fallback
            return searchProducts(query, null, 0, 9);
        }
    }
}
