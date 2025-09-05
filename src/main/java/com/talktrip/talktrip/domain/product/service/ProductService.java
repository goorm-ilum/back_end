package com.talktrip.talktrip.domain.product.service;

import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.product.dto.ProductWithAvgStarAndLike;
import com.talktrip.talktrip.domain.product.dto.response.ProductDetailResponse;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.domain.review.dto.response.ReviewResponse;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.domain.review.repository.ReviewRepository;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.MemberException;
import com.talktrip.talktrip.global.exception.ProductException;
import com.talktrip.talktrip.global.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final String ALL_COUNTRIES = "전체";

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final CountryRepository countryRepository;
    private final RestTemplate restTemplate;

    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;

    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> searchProducts(
            String keyword,
            String countryName,
            Long memberId,
            Pageable pageable
    ) {
        validateMember(memberId);
        validateCountry(countryName);
        Page<ProductWithAvgStarAndLike> searchResults = productRepository.searchProductsWithAvgStarAndLike(
                keyword, countryName, memberId, pageable
        );
        
        if (searchResults.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<ProductSummaryResponse> productResponses = searchResults.getContent().stream()
                .map(result -> {
                    Product product = result.getProduct();
                    Double avgStar = result.getAvgStar();
                    Boolean isLiked = result.getIsLiked();
                    return ProductSummaryResponse.from(product, avgStar, isLiked);
                })
                .toList();

        return new PageImpl<>(productResponses, pageable, searchResults.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(
            Long productId,
            Long memberId,
            Pageable pageable
    ) {
        validateMember(memberId);
        validateProduct(productId);
        ProductWithAvgStarAndLike productWithDetails = findProductWithDetailsAndAvgStarAndLike(productId, memberId);

        Double avgStar = productWithDetails.getAvgStar();
        boolean isLiked = productWithDetails.getIsLiked();

        Page<Review> reviewPage = reviewRepository.findByProductIdWithPaging(productId, pageable);
        List<ReviewResponse> reviewResponses = ReviewResponse.to(reviewPage.getContent(), productWithDetails.getProduct());

        return ProductDetailResponse.from(productWithDetails.getProduct(), avgStar, reviewResponses, isLiked);
    }

    private void validateMember(Long memberId) {
        if (memberId != null && !memberRepository.existsById(memberId)) {
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

    private void validateCountry(String countryName) {
        if (countryName == null || countryName.isBlank() || ALL_COUNTRIES.equals(countryName)) {
            return;
        }

        if (!countryRepository.existsByName(countryName.trim())) {
            throw new ProductException(ErrorCode.COUNTRY_NOT_FOUND);
        }
    }

    private ProductWithAvgStarAndLike findProductWithDetailsAndAvgStarAndLike(Long productId, Long memberId) {
        return productRepository.findByIdWithDetailsAndAvgStarAndLike(productId, memberId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> aiSearchProducts(
            String query,
            Long memberId,
            Pageable pageable
    ) {
        validateMember(memberId);
        validateQuery(query);
        
        List<Long> productIds = fetchProductIdsFromAI(query);
        
        if (productIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 모든 데이터를 한 번에 조회 (단일 쿼리 + 페이징)
        Page<ProductWithAvgStarAndLike> productsWithDetails = productRepository.findProductsWithAvgStarAndLikeByIds(productIds, memberId, pageable);

        // AI 응답 순서 유지를 위한 인덱스 맵
        Map<Long, Integer> idOrder = createIdOrderMap(productIds);

        List<ProductSummaryResponse> content = productsWithDetails.getContent().stream()
                .sorted(Comparator.comparing(result -> idOrder.getOrDefault(result.getProduct().getId(), Integer.MAX_VALUE)))
                .map(this::createProductSummaryResponseFromDetails)
                .toList();

        return new PageImpl<>(content, pageable, productsWithDetails.getTotalElements());
    }

    private List<Long> fetchProductIdsFromAI(String query) {
        try {
            String fastApiUrl = fastApiBaseUrl + "/query";
            Map<String, String> requestBody = Map.of("query", query);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    fastApiUrl,
                    requestBody,
                    Map.class
            );

            if (response == null || !response.containsKey("product_ids")) {
                return List.of();
            }

            Object productIdsObj = response.get("product_ids");
            if (!(productIdsObj instanceof List<?>)) {
                return List.of();
            }

            return ((List<?>) productIdsObj).stream()
                    .map(Object::toString)
                    .map(Long::parseLong)
                    .toList();

        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<Long, Integer> createIdOrderMap(List<Long> productIds) {
        Map<Long, Integer> idOrder = new HashMap<>();
        for (int i = 0; i < productIds.size(); i++) {
            idOrder.put(productIds.get(i), i);
        }
        return idOrder;
    }

    private ProductSummaryResponse createProductSummaryResponseFromDetails(ProductWithAvgStarAndLike details) {
        return ProductSummaryResponse.from(details.getProduct(), details.getAvgStar(), details.getIsLiked());
    }

    private void validateQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }
}
