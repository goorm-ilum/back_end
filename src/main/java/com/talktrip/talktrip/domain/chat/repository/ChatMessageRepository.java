package com.talktrip.talktrip.domain.chat.repository;

import com.talktrip.talktrip.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    @Query(value = """
      SELECT COUNT(*)
      FROM chating_message_history_tab
      WHERE room_id = :roomId
        AND created_at > (
          SELECT created_at
          FROM chating_message_history_tab
          WHERE message_id = (
            SELECT last_read_message_id
            FROM chating_room_users_tab
            WHERE user_id = :userId
              AND room_id = :roomId
          )
        )
     """, nativeQuery = true)
    int countUnreadMessagesByRoomId(
            @Param("roomId") String roomId,
            @Param("userId") String userId
    );
    @Query(value = """
     SELECT COUNT(*) AS unread_room_count
        FROM chating_room_member_tab crmt
        WHERE crmt.member_id  = :userId
          AND EXISTS (
            SELECT 1
            FROM chating_message_history_tab msg
            WHERE msg.room_id = crmt.room_id
              AND msg.created_at > (
                SELECT sub.created_at
                FROM chating_message_history_tab sub
                WHERE sub.message_id = crmt.last_read_message_id
              )
          )
   """, nativeQuery = true)
    int countUnreadMessagesRooms(
            @Param("userId") String userId
    );

    @Query(value = """
     SELECT IFNULL(SUM(not_read_message_count), 0) AS total_unread_message_count
     FROM (
         SELECT COUNT(*) AS not_read_message_count
         FROM chating_room_member_tab crmt
         JOIN chating_room_tab crt ON crt.room_id = crmt.room_id
         JOIN chating_message_history_tab msg ON msg.room_id = crmt.room_id
         WHERE crmt.member_id = :userId
           AND (
               crmt.last_member_read_time IS NULL
               OR msg.created_at > crmt.last_member_read_time
           )
           AND msg.member_id != :userId -- ✅ 나 자신이 보낸 메시지는 제외
         GROUP BY crmt.room_id
     ) AS unread_counts;
   """, nativeQuery = true)
    int countUnreadMessages(
            @Param("userId") String userId
    );
    List<ChatMessage> findByRoomIdOrderByCreatedAt(String userId);

    @Query(value = """
      SELECT count(*)
    FROM chating_message_history_tab cmht
    WHERE cmht.room_id = :roomId
      AND cmht.member_id != :memberId
      AND cmht.created_at > (
        SELECT cmht2.created_at
        FROM chating_message_history_tab cmht2
        WHERE cmht2.room_id = :roomId
          AND cmht2.member_id = :memberId
        ORDER BY cmht2.created_at DESC
        LIMIT 1
    )
""", nativeQuery = true)
    int countUnreadMessagesByRoomIdAndMemberId(
            @Param("roomId") String roomId,
            @Param("memberId") String memberId
    );
}
