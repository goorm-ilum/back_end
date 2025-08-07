package com.talktrip.talktrip.domain.chat.dto.response;

import com.talktrip.talktrip.domain.chat.entity.ChatRoom;
import com.talktrip.talktrip.global.entity.BaseEntity;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ChatRoomResponseDto extends BaseEntity {
    @Id
    private final String roomId;

    public ChatRoomResponseDto(String roomId) {
        this.roomId = roomId;
    }
    public static ChatRoomResponseDto fromEntity(ChatRoom chatRoom) {
        return ChatRoomResponseDto.builder()
                .roomId(chatRoom.getRoomId())
                .build();
    }
    public static ChatRoomResponseDto createNew() {
        String newRoomId = "ROOM_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        return ChatRoomResponseDto.builder()
                .roomId(newRoomId)
                .build();
    }
}
