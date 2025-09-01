package com.talktrip.talktrip.domain.chat.dto.response;

// com.talktrip.talktrip.domain.chat.dto.ChatRoomDetailDto

import java.time.LocalDateTime;
import java.util.List;

public record ChatRoomDetailDto(
        String roomId,
        String title,
        Integer productId,
        String ownerEmail,           // 방 개설자/판매자 등
        LocalDateTime myLastReadAt,  // 내가 마지막으로 읽은 시각
        Integer memberCount,         // 참여자 수
        List<String> participants    // (선택) 참여자 이메일 목록
) {

}
