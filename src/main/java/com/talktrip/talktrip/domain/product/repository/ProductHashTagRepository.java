package com.talktrip.talktrip.domain.product.repository;

import com.talktrip.talktrip.domain.product.entity.HashTag;
import com.talktrip.talktrip.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductHashTagRepository extends JpaRepository<HashTag, Long> {
    void deleteAllByProduct(Product product);
    List<HashTag> findAllByProduct(Product product);
}
