package com.talktrip.talktrip.domain.product.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ProductOptionRequest(
        @NotNull(message = "시작일은 필수입니다")
        @Future(message = "시작일은 미래 날짜여야 합니다")
        LocalDate startDate,
        
        @NotBlank(message = "옵션명은 필수입니다")
        @Size(min = 1, max = 50, message = "옵션명은 1자 이상 50자 이하여야 합니다")
        String optionName,
        
        @Min(value = 0, message = "재고는 0 이상이어야 합니다")
        int stock,
        
        @Min(value = 0, message = "가격은 0 이상이어야 합니다")
        int price,
        
        @Min(value = 0, message = "할인가격은 0 이상이어야 합니다")
        int discountPrice
) {}

