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
        String fastApiUrl = fastApiBaseUrl + "/query";

        Map<String, String> requestBody = Map.of("query", query);

        ProductSummaryResponse[] result = restTemplate.postForObject(
                fastApiUrl,
                requestBody,
                ProductSummaryResponse[].class
        );

        if (result == null) return List.of();

        return List.of(result);
    }
}
