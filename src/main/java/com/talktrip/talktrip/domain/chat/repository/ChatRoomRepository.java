package com.talktrip.talktrip.domain.chat.repository;

import com.talktrip.talktrip.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query(value = """
        SELECT
          crmt.room_id,
          crmt.room_member_id  ,
          crt.created_at,
          crt.updated_at,
          crt.last_message_id,
          CONCAT('채팅방 ', crt.room_id) AS title,
          cmht.message as last_message,
          (
            SELECT COUNT(*)
            FROM chating_message_history_tab msg
            WHERE msg.room_id = crt.room_id
              AND msg.created_at > (
                SELECT sub.created_at
                FROM chating_message_history_tab sub
                WHERE sub.message_id = crmt.last_read_message_id
              )
          ) AS not_read_message_count
        FROM chating_room_member_tab crmt
        JOIN chating_room_tab crt ON crt.room_id = crmt.room_id
        JOIN chating_message_history_tab cmht ON cmht.message_id = crt.last_message_id
        WHERE crmt.member_id  = :memberId;
    """, nativeQuery = true)
    List<ChatRoom> findRoomsWithLastMessageByMemberId(@Param("memberId") String memberId);
}
