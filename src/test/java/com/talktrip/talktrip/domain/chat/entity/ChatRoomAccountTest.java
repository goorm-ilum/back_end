package com.talktrip.talktrip.domain.chat.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomAccountTest {

    @Test
    @DisplayName("ChatRoomAccount Builder를 통해 객체를 정상적으로 생성한다")
    void createChatRoomAccount() {
        // Given
        String roomId = "ROOM_001";
        String accountEmail = "user@test.com";

        // When
        ChatRoomAccount chatRoomAccount = ChatRoomAccount.create(roomId, accountEmail);

        // Then
        assertThat(chatRoomAccount.getRoomId()).isEqualTo(roomId);
        assertThat(chatRoomAccount.getAccountEmail()).isEqualTo(accountEmail);
        assertThat(chatRoomAccount.getLastMemberReadTime()).isNull(); // 초기값은 null
        assertThat(chatRoomAccount.getIsDel()).isEqualTo(0); // 초기값은 0
    }

    @Test
    @DisplayName("마지막 읽은 시간을 업데이트할 수 있다")
    void updateLastReadTime() {
        // Given
        ChatRoomAccount chatRoomAccount = ChatRoomAccount.create("ROOM_001", "user@test.com");
        LocalDateTime now = LocalDateTime.now();

        // When
        chatRoomAccount.updateLastReadTime(now);

        // Then
        assertThat(chatRoomAccount.getLastMemberReadTime()).isEqualTo(now);
    }
}