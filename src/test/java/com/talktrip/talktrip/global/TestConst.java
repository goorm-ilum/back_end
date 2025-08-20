// src/test/java/com/talktrip/talktrip/global/TestConst.java
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
    public static final long OTHER_MEMBER_ID = 22L;
    public static final long PRODUCT_ID = 3L;
    public static final long OTHER_PRODUCT_ID = 100L;
    public static final long ORDER_ID = 4L;
    public static final long REVIEW_ID = 5L;
    public static final long NON_EXIST_ORDER_ID = 999_999L;
    public static final long OPTION_ID_1 = 11L;
    public static final long OPTION_ID_2 = 12L;
    public static final long IMAGE_ID_1 = 41L;
    public static final long IMAGE_ID_10 = 50L;
    public static final long COUNTRY_ID_1 = 1L;
    public static final long COUNTRY_ID_2 = 2L;

    // ===== Emails / Names / Phones =====
    public static final String USER_EMAIL = "user@gmail.com";
    public static final String USER2_EMAIL = "user2@gmail.com";
    public static final String USER_NAME = "userName";
    public static final String USER2_NAME = "user2Name";
    public static final String SELLER_EMAIL = "seller@gmail.com";
    public static final String SELLER_NAME = "sellerName";
    public static final String PHONE_NUMBER = "010-0000-0000";

    // ===== Texts / Names =====
    public static final String DESC = "test description";
    public static final String PRODUCT_NAME_1 = "P1";
    public static final String PRODUCT_NAME_2 = "P2";
    public static final String PRODUCT_NAME_3 = "P3";
    public static final String OPTION_NAME = "옵션A";
    public static final String OPTION_NAME_2 = "옵션B";

    // RepositoryImpl 전용 테스트용 이름/설명
    public static final String PRODUCT_NAME_SEA_TOUR = "SEA sea Food Tour";
    public static final String PRODUCT_NAME_TOKYO = "Tokyo Adventure";
    public static final String PRODUCT_NAME_MOUNTAIN = "Mountain Walk";
    public static final String DESC_SEA = "best food near sea";
    public static final String DESC_CITY = "city tour";
    public static final String DESC_MOUNTAIN = "fresh air";

    // ===== URLs / Tags / Hash =====
    public static final String THUMBNAIL_URL = "https://t.com/thumb.png";
    public static final String THUMBNAIL_HASH = "thumbhash-x";
    public static final String IMAGE_URL_1 = "https://t.com/1.png";
    public static final String IMAGE_URL_2 = "https://t.com/2.png";
    public static final String IMAGE_URL_3 = "https://t.com/3.png";
    public static final String HASHTAG_SEA = "sea";
    public static final String HASHTAG_FOOD = "food";

    // ===== Countries / Continents =====
    public static final String COUNTRY_ALL = "전체";
    public static final String COUNTRY_KOREA = "대한민국";
    public static final String COUNTRY_JAPAN = "일본";
    public static final String CONTINENT_ASIA = "ASIA";

    // ===== Stars (review values) =====
    public static final float STAR_0_0 = 0.0f;
    public static final float STAR_2_0 = 2.0f;
    public static final float STAR_3_0 = 3.0f;
    public static final float STAR_4_0 = 4.0f;
    public static final float STAR_4_5 = 4.5f;
    public static final float STAR_5_0 = 5.0f;
    public static final float AVG_3_0 = 3.0f;
    public static final float AVG_5_0 = 5.0f;

    // ===== Prices / Stock / Order =====
    public static final int STOCK_3 = 3;
    public static final int STOCK_5 = 5;
    public static final int QUANTITY_1 = 1;
    public static final int PRICE_10000 = 10_000;
    public static final int PRICE_12000 = 12_000;
    public static final int DISC_9000 = 9_000;
    public static final int DISC_9500 = 9_500;
    public static final String ORDER_CODE_PREFIX = "ORD-";

    // ===== Sorting keys =====
    public static final String SORT_UPDATED_AT = "updatedAt";
    public static final String SORT_PRODUCT_NAME = "productName";
    public static final String SORT_PRICE = "price";
    public static final String SORT_DISCOUNT_PRICE = "discountPrice";
    public static final String SORT_AVERAGE_STAR = "averageStar";
    public static final String SORT_REVIEW_STAR = "reviewStar";
    public static final String SORT_TOTAL_STOCK = "totalStock";
    public static final String NOT_EXISTS_PROPERTY = "notExistsProperty";

    // ===== Sort objects =====
    public static final Sort DEFAULT_SORT_UPDATED_DESC = Sort.by(Sort.Order.desc(SORT_UPDATED_AT));
    public static final Sort SORT_BY_UPDATED_DESC = DEFAULT_SORT_UPDATED_DESC;
    public static final Sort SORT_BY_PRODUCT_NAME_DESC = Sort.by(Sort.Order.desc(SORT_PRODUCT_NAME));
    public static final Sort SORT_BY_PRICE_DESC = Sort.by(Sort.Order.desc(SORT_PRICE));
    public static final Sort SORT_BY_REVIEW_STAR_DESC = Sort.by(Sort.Order.desc(SORT_REVIEW_STAR));

    // ===== Pageable / Paging =====
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

    // ===== Security / Misc =====
    public static final String ROLE_USER = "ROLE_USER";
    public static final String COMMENT_TEST = "test review";
    public static final String BLANK = "   ";

    // ===== Status / Keyword (Admin 리스트) =====
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DELETED = "DELETED";
    public static final String KEYWORD_P = "P";
    public static final String KEYWORD_MULTI = "p sea";
    public static final String KEYWORD_SEA = "sea";
    public static final String KEYWORD_SEA_MIXED_CASE = "SeA";
    public static final String KEYWORD_FOOD = "food";

    // ===== Multipart / File constants =====
    public static final String MF_T_NAME = "t";
    public static final String MF_T_FILENAME = "t.png";
    public static final String MF_D_NAME = "d";
    public static final String MF_D1_FILENAME = "d1.png";
    public static final String MEDIA_IMAGE_PNG = "image/png";
    public static final byte[] BYTES_X = "x".getBytes();

    // ===== Endpoints (controller tests에서 사용) =====
    // Public Product
    public static final String EP_SEARCH_PRODUCTS = "/api/products";
    public static final String EP_GET_PRODUCT_DETAIL = "/api/products/{productId}";
    // Admin Product
    public static final String EP_ADMIN_BASE = "/api/admin/products";
    public static final String EP_ADMIN_CREATE_PRODUCT = EP_ADMIN_BASE;
    public static final String EP_ADMIN_UPDATE_PRODUCT = EP_ADMIN_BASE + "/{productId}";
    public static final String EP_ADMIN_DELETE_PRODUCT = EP_ADMIN_BASE + "/{productId}";
    public static final String EP_ADMIN_EDIT_FORM = EP_ADMIN_BASE + "/{productId}";
    public static final String EP_ADMIN_LIST_PRODUCTS = EP_ADMIN_BASE;
    public static final String EP_ADMIN_RESTORE_PRODUCT = EP_ADMIN_BASE + "/{productId}/restore";
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

    // ===== Headers =====
    public static final String HDR_LOCATION = "Location";

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

    public static final String JSON_PRODUCT_ID = "$.productId";
    public static final String JSON_PRODUCT_NAME = "$.productName";
    public static final String JSON_IS_LIKED = "$.isLiked";

    public static final String JSON_REVIEW_ID = "$.reviewId";
    public static final String JSON_PRODUCT_NAME_IN_REVIEW = "$.productName";
    public static final String JSON_THUMBNAIL_URL = "$.thumbnailUrl";
    public static final String JSON_MY_STAR = "$.myStar";
    public static final String JSON_MY_COMMENT = "$.myComment";

    public static final String JSON_CONTENT_0_REVIEW_ID = "$.content[0].reviewId";

    // ===== Error codes/messages =====
    public static final String ERR_USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String ERR_ADMIN_NOT_FOUND = "ADMIN_NOT_FOUND";
    public static final String ERR_PRODUCT_NOT_FOUND = "PRODUCT_NOT_FOUND";
    public static final String ERR_ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
    public static final String ERR_REVIEW_NOT_FOUND = "REVIEW_NOT_FOUND";
    public static final String ERR_ACCESS_DENIED = "ACCESS_DENIED";
    public static final String ERR_ORDER_EMPTY = "ORDER_EMPTY";
    public static final String ERR_ORDER_NOT_COMPLETED = "ORDER_NOT_COMPLETED";
    public static final String ERR_ALREADY_REVIEWED = "ALREADY_REVIEWED";
    public static final String ERR_IMAGE_UPLOAD_FAILED = "IMAGE_UPLOAD_FAILED";
    public static final String ERR_COUNTRY_NOT_FOUND = "COUNTRY_NOT_FOUND";

    public static final String MSG_USER_NOT_FOUND = "사용자를 찾을 수 없습니다.";
    public static final String MSG_PRODUCT_NOT_FOUND = "상품을 찾을 수 없습니다.";
    public static final String MSG_REVIEW_NOT_FOUND = "리뷰를 찾을 수 없습니다.";
    public static final String MSG_ACCESS_DENIED = "접근 권한이 없습니다.";

    public static final String ERROR_CODE = "errorCode";

    // ===== Dates / times =====
    public static final String TIME = "2025-08-18T12:00";
}
