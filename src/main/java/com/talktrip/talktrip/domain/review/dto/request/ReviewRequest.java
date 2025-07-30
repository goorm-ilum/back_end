package com.talktrip.talktrip.domain.review.dto.request;

public record ReviewRequest(
        String comment,
        float reviewStar
) {}
