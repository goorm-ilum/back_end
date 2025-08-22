package com.talktrip.talktrip.domain.review.dto.response;

public record ReviewPolarityStatsDto(
        long totalCount,
        long positiveCount,
        long negativeCount,
        long neutralCount,
        double positiveRate,
        double negativeRate,
        double neutralRate
) {
}
