package com.talktrip.talktrip.domain.product.repository;

import com.talktrip.talktrip.domain.product.entity.Product;

import java.util.List;

public interface ProductRepositoryCustom {
    List<Product> searchByKeywords(List<String> keywords, int offset, int limit);
    List<Product> searchByKeywordsAndMemberId(List<String> keywords, Long memberId);
}
