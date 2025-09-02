package com.talktrip.talktrip.domain.chat.entity;

import com.talktrip.talktrip.global.entity.BaseEntity;
import jakarta.persistence.*;
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
    @Column(name = "room_id", nullable = false, unique = true)
    private String roomId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "product_id", nullable = false)
    private int productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false)
    private RoomType roomType;
}
