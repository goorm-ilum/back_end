package com.talktrip.talktrip.domain.chat.dto.response;

import java.time.LocalDateTime;

public record ChatRoomDetailScalar(
        String roomId,
        String title,
        Integer productId

) {}