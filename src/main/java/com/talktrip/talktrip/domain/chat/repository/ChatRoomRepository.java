package com.talktrip.talktrip.domain.chat.repository;

import com.talktrip.talktrip.domain.chat.dto.response.ChatRoomDTO;
import com.talktrip.talktrip.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {

    @Query("""
        SELECT NEW com.talktrip.talktrip.domain.chat.dto.response.ChatRoomDTO(
            crmt.roomId,
            crmt.roomAccountId,
            crt.createdAt,
            crt.updatedAt,
            CONCAT(
                CONCAT(COALESCE(m.name, ''), '_'),
                CONCAT(CONCAT(COALESCE(p.productName, ''), '_'), crt.roomId)
            ) as title,
            COALESCE((
                SELECT cm1.message
                FROM ChatMessage cm1
                WHERE cm1.roomId = crt.roomId
                  AND cm1.createdAt = (
                      SELECT MAX(cm1b.createdAt)
                      FROM ChatMessage cm1b
                      WHERE cm1b.roomId = crt.roomId
                  )
            ), '')AS lastMessage,
            (
                SELECT COUNT(cm2)
                FROM ChatMessage cm2
                WHERE cm2.roomId = crt.roomId
                  AND cm2.createdAt > COALESCE(crmt.lastMemberReadTime, '1970-01-01 00:00:00')
                  AND cm2.accountEmail <> :memberId
            )AS notReadMessageCount
        )
        FROM ChatRoomAccount crmt
        JOIN ChatRoom crt ON crt.roomId = crmt.roomId
        LEFT JOIN ChatRoomAccount other
               ON other.roomId = crmt.roomId
              AND other.accountEmail <> :memberId
              AND other.isDel = 0
        LEFT JOIN Member m
               ON m.accountEmail = other.accountEmail
        LEFT JOIN Product p
               ON p.id = crt.productId
        WHERE crmt.accountEmail = :memberId
          AND crmt.isDel = 0
    """)
    List<ChatRoomDTO> findRoomsWithLastMessageByMemberId(
            @Param("memberId") String memberId
    );
}
