package com.talktrip.talktrip.global;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class TestConst {

    private TestConst() {}

    // ===== IDs =====
    public static final long USER_ID = 1L;
    public static final long USER_ID2 = 6L;
    public static final long SELLER_ID = 2L;
    public static final long PRODUCT_ID = 3L;
    public static final long OTHER_PRODUCT_ID = 100L;
    public static final long ORDER_ID = 4L;
    public static final long REVIEW_ID = 5L;
    public static final long NON_EXIST_ORDER_ID = 999_999L;

    // ===== Emails =====
    public static final String USER_EMAIL = "user@gmail.com";
    public static final String USER2_EMAIL = "user2@gmail.com";
    public static final String SELLER_EMAIL = "seller@gmail.com";

    // ===== Texts =====
    public static final String DESC = "test description";
    public static final String PRODUCT_NAME_1 = "P1";
    public static final String PRODUCT_NAME_2 = "P2";
    public static final String PRODUCT_NAME_3 = "P3";

    // ===== Stars (review values) =====
    public static final float STAR_0_0 = 0.0f;
    public static final float STAR_2_0 = 2.0f;
    public static final float STAR_3_0 = 3.0f;
    public static final float STAR_4_0 = 4.0f;
    public static final float STAR_4_5 = 4.5f;
    public static final float STAR_5_0 = 5.0f;
    // Commonly asserted averages
    public static final float AVG_3_0 = 3.0f;
    public static final float AVG_5_0 = 5.0f;

    // ===== Sorting / Pageable =====
    public static final String SORT_UPDATED_AT = "updatedAt";
    public static final String SORT_PRICE = "price";
    public static final String SORT_REVIEW_STAR = "reviewStar";

    public static final Sort DEFAULT_SORT_UPDATED_DESC = Sort.by(Sort.Order.desc(SORT_UPDATED_AT));
    public static final Sort SORT_BY_PRICE_DESC = Sort.by(Sort.Order.desc(SORT_PRICE));
    public static final Sort SORT_BY_UPDATED_DESC = Sort.by(Sort.Order.desc(SORT_UPDATED_AT));
    public static final Sort SORT_BY_REVIEW_STAR_DESC = Sort.by(Sort.Order.desc(SORT_REVIEW_STAR));

    public static final int PAGE_0 = 0;
    public static final int PAGE_1 = 1;
    public static final int PAGE_2 = 2;
    public static final int PAGE_3 = 3;
    public static final int PAGE_4 = 4;
    public static final int PAGE_5 = 5;
    public static final int PAGE_7 = 7;

    public static final int SIZE_1 = 1;
    public static final int SIZE_2 = 2;
    public static final int SIZE_5 = 5;
    public static final int SIZE_9 = 9;
    public static final int SIZE_10 = 10;

    public static final Pageable PAGE_0_SIZE_9 = PageRequest.of(PAGE_0, SIZE_9, DEFAULT_SORT_UPDATED_DESC);
    public static final Pageable PAGE_0_SIZE_10 = PageRequest.of(PAGE_0, SIZE_10, DEFAULT_SORT_UPDATED_DESC);

    // ===== Endpoints (for readability) =====
    // Like
    public static final String EP_TOGGLE_LIKE = "/api/products/{productId}/like";
    public static final String EP_GET_MY_LIKES = "/api/me/likes";
    // Review
    public static final String EP_CREATE_REVIEW = "/api/orders/{orderId}/review";
    public static final String EP_UPDATE_REVIEW = "/api/reviews/{reviewId}";
    public static final String EP_DELETE_REVIEW = "/api/reviews/{reviewId}";
    public static final String EP_GET_MY_REVIEWS = "/api/me/reviews";
    public static final String EP_GET_CREATE_FORM = "/api/orders/{orderId}/review/form";
    public static final String EP_GET_UPDATE_FORM = "/api/reviews/{reviewId}/form";
    public static final String EP_GET_ADMIN_PRODUCT_REVIEWS = "/api/admin/products/{productId}/reviews";

    // ===== JSON path fields =====
    public static final String JSON_ERROR_CODE = "$.errorCode";
    public static final String JSON_MESSAGE = "$.message";
    public static final String JSON_CONTENT_LEN = "$.content.length()";
    public static final String JSON_TOTAL_ELEMENTS = "$.totalElements";
    public static final String JSON_NUMBER = "$.number";
    public static final String JSON_SIZE = "$.size";
    public static final String JSON_CONTENT_0_PRODUCT_ID = "$.content[0].productId";
    public static final String JSON_CONTENT_0_PRODUCT_NAME = "$.content[0].productName";
    public static final String JSON_CONTENT_0_AVG_STAR = "$.content[0].averageReviewStar";
    public static final String JSON_CONTENT_0_REVIEW_ID = "$.content[0].reviewId";
    public static final String JSON_PRODUCT_NAME = "$.productName";
    public static final String JSON_REVIEW_ID = "$.reviewId";
    public static final String JSON_THUMBNAIL_URL = "$.thumbnailUrl";
    public static final String JSON_MY_STAR = "$.myStar";
    public static final String JSON_MY_COMMENT = "$.myComment";

    // ===== Error codes/messages =====
    public static final String ERR_USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String ERR_PRODUCT_NOT_FOUND = "PRODUCT_NOT_FOUND";
    public static final String ERR_ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
    public static final String ERR_REVIEW_NOT_FOUND = "REVIEW_NOT_FOUND";
    public static final String ERR_ACCESS_DENIED = "ACCESS_DENIED";
    public static final String ERR_ORDER_EMPTY = "ORDER_EMPTY";
    public static final String ERR_ORDER_NOT_COMPLETED = "ORDER_NOT_COMPLETED";
    public static final String ERR_ALREADY_REVIEWED = "ALREADY_REVIEWED";

    public static final String MSG_USER_NOT_FOUND = "사용자를 찾을 수 없습니다.";
    public static final String MSG_PRODUCT_NOT_FOUND = "상품을 찾을 수 없습니다.";
    public static final String MSG_REVIEW_NOT_FOUND = "리뷰를 찾을 수 없습니다.";
    public static final String MSG_ACCESS_DENIED = "접근 권한이 없습니다.";

    // ===== Misc domain defaults used in stubs =====
    public static final int PRICE_10000 = 10000;
    public static final int QUANTITY_1 = 1;
    public static final String ORDER_CODE_PREFIX = "orderCode-";
    public static final String ROLE_USER = "ROLE_USER";
    public static final String COMMENT_TEST = "test review";

    // ===== Dates / times (string asserts) =====
    public static final String FIXED_REVIEW_TIME = "2025-08-18T12:00";
}