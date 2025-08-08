package com.talktrip.talktrip.domain.chat.repository;

import com.talktrip.talktrip.domain.chat.dto.response.ChatRoomDTO;
import com.talktrip.talktrip.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


    @Repository
    public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {

        @Query(value = """
           SELECT NEW com.talktrip.talktrip.domain.chat.dto.response.ChatRoomDTO(
                crmt.roomId,
                crmt.roomMemberId,
                crt.createdAt,
                crt.updatedAt,
                CONCAT('채팅방 ', crt.roomId),
                COALESCE((
                      SELECT cmht.message
                      FROM ChatMessage cmht
                      WHERE cmht.roomId = crt.roomId
                      ORDER BY cmht.createdAt DESC
                      LIMIT 1
                  ), ''),
                (
                    SELECT COUNT(msg)
                    FROM ChatMessage msg
                    WHERE msg.roomId = crt.roomId
                      AND msg.createdAt > COALESCE(crmt.lastMemberReadTime, '1970-01-01 00:00:00')
                      AND msg.memberId != :memberId
                )
            )
            FROM ChatRoomMember crmt
            JOIN ChatRoom crt ON crt.roomId = crmt.roomId
            WHERE crmt.memberId = :memberId
    """, nativeQuery = false)
        List<ChatRoomDTO> findRoomsWithLastMessageByMemberId(@Param("memberId") String memberId);
    }

