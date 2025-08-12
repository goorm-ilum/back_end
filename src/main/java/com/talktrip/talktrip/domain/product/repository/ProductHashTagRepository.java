package com.talktrip.talktrip.domain.product.repository;

import com.talktrip.talktrip.domain.product.entity.Product;
import com.talktrip.talktrip.domain.product.entity.HashTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductHashTagRepository extends JpaRepository<HashTag, Long> {
    void deleteAllByProduct(Product product);
}
