package com.talktrip.talktrip.domain.product.service;

import com.talktrip.talktrip.domain.like.repository.LikeRepository;
import com.talktrip.talktrip.domain.product.dto.response.ProductDetailResponse;
import com.talktrip.talktrip.domain.product.dto.response.ProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.ProductOption;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.domain.review.dto.response.ReviewResponse;
import com.talktrip.talktrip.domain.review.dto.response.ReviewPolarityStatsDto;
import com.talktrip.talktrip.domain.review.entity.Review;
import com.talktrip.talktrip.domain.review.repository.ReviewRepository;
import com.talktrip.talktrip.domain.review.service.ReviewService;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.ProductException;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import com.talktrip.talktrip.global.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final LikeRepository likeRepository;
    private final ReviewService reviewService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;

    @Transactional
    public Page<ProductSummaryResponse> searchProducts(String keyword, String countryName, CustomMemberDetails memberDetails, Pageable pageable) {
        List<Product> products;

        if (keyword == null || keyword.isBlank()) {
            products = "전체".equals(countryName)
                    ? productRepository.findAll()
                    : productRepository.findByCountryName(countryName);
        } else {
            List<String> keywords = Arrays.stream(keyword.trim().split("\\s+"))
                    .filter(s -> !s.isBlank())
                    .toList();

            products = productRepository.searchByKeywords(
                    keywords,
                    countryName,
                    0,
                    Integer.MAX_VALUE
            );
        }

        LocalDate today = LocalDate.now();
        List<Product> filtered = products.stream()
                .filter(product -> product.getProductOptions().stream()
                        .filter(option -> !option.getStartDate().isBefore(today))
                        .mapToInt(ProductOption::getStock).sum() > 0)
                .sorted(getComparator(pageable.getSort()))
                .toList();

        int offset = (int) pageable.getOffset();
        int toIndex = Math.min(offset + pageable.getPageSize(), filtered.size());
        List<Product> paged = (offset > filtered.size()) ? List.of() : filtered.subList(offset, toIndex);

        List<ProductSummaryResponse> responseList = paged.stream()
                .map(product -> {
                    float avgStar = (float) reviewRepository.findByProductId(product.getId()).stream()
                            .mapToDouble(Review::getReviewStar).average().orElse(0.0);
                    boolean liked = memberDetails != null && likeRepository.existsByProductIdAndMemberId(product.getId(), memberDetails.getId());
                    return ProductSummaryResponse.from(product, avgStar, liked);
                })
                .toList();

        return new PageImpl<>(responseList, pageable, filtered.size());
    }



    @Transactional
    public ProductDetailResponse getProductDetail(Long productId, CustomMemberDetails memberDetails, Pageable pageable) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        int futureStock = product.getProductOptions().stream()
                .filter(option -> !option.getStartDate().isBefore(LocalDate.now()))
                .mapToInt(ProductOption::getStock)
                .sum();

        if (futureStock == 0) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        Page<Review> reviewPage = reviewRepository.findByProductId(productId, pageable);

        float avgStar = (float) reviewPage.getContent().stream()
                .mapToDouble(Review::getReviewStar)
                .average()
                .orElse(0.0);

        List<ReviewResponse> reviewResponses = reviewPage.stream()
                .map(review -> ReviewResponse.from(review, product))
                .toList();


        boolean isLiked = false;
        if (memberDetails != null) {
            isLiked = likeRepository.existsByProductIdAndMemberId(productId, memberDetails.getId());
        }

        ReviewPolarityStatsDto stats =
                reviewService.calcPolarityStats(productId.intValue());

        return ProductDetailResponse.from(product, avgStar, reviewResponses, stats, isLiked);
    }



    public List<ProductSummaryResponse> aiSearchProducts(String query, CustomMemberDetails memberDetails) {
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
                        
                        // 사용자 인증 정보가 있는 경우 좋아요 상태 확인
                        boolean liked = memberDetails != null && 
                            likeRepository.existsByProductIdAndMemberId(product.getId(), memberDetails.getId());
                        
                        return ProductSummaryResponse.from(product, avgStar, liked);
                    })
                    .toList();
            
            return result;
                    
        } catch (Exception e) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    private Comparator<Product> getComparator(Sort sort) {
        Comparator<Product> comparator = Comparator.comparing(Product::getUpdatedAt); // default

        for (Sort.Order order : sort) {
            switch (order.getProperty()) {
                case "discountPrice" -> comparator = Comparator.comparing(
                        p -> Optional.ofNullable(p.getMinPriceOption())
                                .map(ProductOption::getDiscountPrice)
                                .orElse(0)
                );
                case "updatedAt" -> comparator = Comparator.comparing(Product::getUpdatedAt);
                case "averageStar" -> comparator = Comparator.comparing(
                        p -> (float) p.getReviews().stream()
                                .mapToDouble(Review::getReviewStar)
                                .average()
                                .orElse(0.0)
                );
                default -> throw new IllegalArgumentException("Invalid sort property: " + order.getProperty());
            }

            if (order.getDirection().isDescending()) {
                comparator = comparator.reversed();
            }
        }

        return comparator;
    }
}
