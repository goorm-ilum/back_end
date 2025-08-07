package com.talktrip.talktrip.domain.product.dto.request;

import java.time.LocalDate;

public record ProductOptionRequest(
        LocalDate startDate,
        String optionName,
        int stock,
        int price,
        int discountPrice
) {}

