package com.talktrip.talktrip.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * 무한 스크롤 전용 응답 DTO.
 * - items: 실제 데이터 목록
 * - nextCursor: 다음 페이지를 요청할 때 사용할 커서 (없으면 null)
 * - hasNext: 다음 페이지 존재 여부
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SliceResponse<T>(
        List<T> items,
        String nextCursor,
        boolean hasNext
) {
    public static <T> SliceResponse<T> of(List<T> items, String nextCursor, boolean hasNext) {
        return new SliceResponse<>(items, nextCursor, hasNext);
    }
}