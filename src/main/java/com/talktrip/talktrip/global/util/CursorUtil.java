package com.talktrip.talktrip.global.util;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * 무한 스크롤 커서 인코딩/디코딩 유틸 (String messageId 버전)
 * createdAt + messageId 를 Base64 URL-safe로 인코딩/디코딩
 */
public class CursorUtil {

    // DATETIME(6) 과 맞추기 위해 마이크로초까지 표현
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    /**
     * 커서 인코딩
     *
     * @param createdAt 메시지 생성 시각
     * @param messageId 메시지 PK (String)
     * @return Base64 URL-safe 커서
     */
    public static String encode(LocalDateTime createdAt, String messageId) {
        String raw = createdAt.format(FORMATTER) + "|" + messageId;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 커서 디코딩
     *
     * @param cursor Base64 URL-safe 커서
     * @return Cursor(createdAt, messageId)
     */
    public static Cursor decode(String cursor) {
        String raw = new String(
                Base64.getUrlDecoder().decode(cursor),
                StandardCharsets.UTF_8
        );
        String[] parts = raw.split("\\|", 2); // messageId 안에 | 가 들어갈 수 있으니 2개만 split
        LocalDateTime t = LocalDateTime.parse(parts[0], FORMATTER);
        String id = parts[1];
        return new Cursor(t, id);
    }

    /**
     * 커서 데이터 구조체
     */
    public record Cursor(LocalDateTime createdAt, String messageId) {}
}
