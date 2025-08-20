package com.talktrip.talktrip.domain.chat.entity;

import com.talktrip.talktrip.global.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "chatting_room_tab")
public class ChatRoom extends BaseEntity {
    @Id
    private String roomId;
    private String roomAccountId;
    private String title;
    private String lastMessage;
    private int notReadMessageCount;
    private int productId;

}
