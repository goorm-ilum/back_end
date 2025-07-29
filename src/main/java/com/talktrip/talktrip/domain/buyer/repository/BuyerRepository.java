package com.talktrip.talktrip.domain.buyer.repository;

import com.talktrip.talktrip.domain.buyer.entity.Buyer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyerRepository extends JpaRepository<Buyer, Long> {
}