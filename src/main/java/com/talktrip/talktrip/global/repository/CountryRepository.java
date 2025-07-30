package com.talktrip.talktrip.global.repository;

import com.talktrip.talktrip.global.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CountryRepository extends JpaRepository<Country, String> {
    Optional<Country> findByName(String name);
}

