package com.talktrip.talktrip.domain.product.repository;

import com.talktrip.talktrip.domain.product.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {

    List<ProductOption> findByProductId(Long productId);

    Optional<ProductOption> findByProductIdAndOptionName(Long productId, String optionName);
}
