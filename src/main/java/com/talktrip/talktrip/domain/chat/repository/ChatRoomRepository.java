package com.talktrip.talktrip.domain.chat.repository;

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
           SELECT
                crmt.room_id,
                crmt.room_member_id,
                crt.created_at,
                crt.updated_at,
                CONCAT('채팅방 ', crt.room_id) AS title,
                COALESCE((
                      SELECT cmht.message
                      FROM chating_message_history_tab cmht
                      WHERE cmht.room_id = crt.room_id
                      ORDER BY cmht.created_at DESC
                      LIMIT 1
                  ), '') AS last_message,
                (
                    SELECT COUNT(*)
                    FROM chating_message_history_tab msg
                    WHERE msg.room_id = crt.room_id
                      AND msg.created_at > COALESCE(crmt.last_member_read_time, '1970-01-01 00:00:00')
                      AND msg.member_id != :memberId -- ✅ 나 자신이 보낸 메시지는 제외
                ) AS not_read_message_count
            FROM chating_room_member_tab crmt
            JOIN chating_room_tab crt ON crt.room_id = crmt.room_id
            WHERE crmt.member_id  = :memberId
    """, nativeQuery = true)
    List<ChatRoom> findRoomsWithLastMessageByMemberId(@Param("memberId") String memberId);
}
