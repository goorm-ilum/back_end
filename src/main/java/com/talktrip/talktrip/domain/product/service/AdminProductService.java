package com.talktrip.talktrip.domain.product.service;

import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.domain.product.dto.request.AdminProductCreateRequest;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductEditResponse;
import com.talktrip.talktrip.domain.product.dto.response.AdminProductSummaryResponse;
import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.repository.ProductRepository;
import com.talktrip.talktrip.global.entity.Country;
import com.talktrip.talktrip.global.exception.AdminException;
import com.talktrip.talktrip.global.exception.ErrorCode;
import com.talktrip.talktrip.global.exception.ProductException;
import com.talktrip.talktrip.global.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CountryRepository countryRepository;
    private final MemberRepository memberRepository;

    public void createProduct(AdminProductCreateRequest request, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AdminException(ErrorCode.ADMIN_NOT_FOUND));

        Country country = countryRepository.findByName(request.countryName())
                .orElseThrow(() -> new ProductException(ErrorCode.COUNTRY_NOT_FOUND));

        Product product = request.to(member, country);
        product.getHashtags().addAll(request.toHashTags(product));
        product.getProductStocks().addAll(request.toProductStocks(product));

        productRepository.save(product);
    }

    public List<AdminProductSummaryResponse> getMyProducts(Long memberId) {
        List<Product> products = productRepository.findByMemberId(memberId);
        return products.stream()
                .map(AdminProductSummaryResponse::from)
                .toList();
    }

    public AdminProductEditResponse getMyProductEditForm(Long productId, Long memberId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getMember().getId().equals(memberId)) {
            throw new ProductException(ErrorCode.ACCESS_DENIED);
        }

        return AdminProductEditResponse.from(product);
    }

    @Transactional
    public void updateProduct(Long productId, AdminProductCreateRequest request, Long memberId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getMember().getId().equals(memberId)) {
            throw new ProductException(ErrorCode.ACCESS_DENIED);
        }

        Country country = countryRepository.findByName(request.countryName())
                .orElseThrow(() -> new ProductException(ErrorCode.COUNTRY_NOT_FOUND));

        product.updateBasicInfo(
                request.productName(),
                request.description(),
                request.price(),
                request.discountPrice(),
                request.thumbnailImageUrl(),
                country
        );

        product.getProductStocks().clear();
        product.getImages().clear();
        product.getHashtags().clear();

        product.getProductStocks().addAll(request.toProductStocks(product));
        product.getHashtags().addAll(request.toHashTags(product));
        product.getImages().addAll(request.toProductImages(product));
    }

}
