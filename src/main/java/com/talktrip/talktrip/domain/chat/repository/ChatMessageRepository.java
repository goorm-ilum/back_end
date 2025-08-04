package com.talktrip.talktrip.domain.chat.repository;

import com.talktrip.talktrip.domain.chat.entity.ChatMessageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageHistory, String> {

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
     SELECT SUM(unread_count) AS total_unread_message_count
     FROM (
         SELECT COUNT(*) AS unread_count
         FROM chating_room_member_tab crmt
         JOIN chating_message_history_tab msg ON msg.room_id = crmt.room_id
         WHERE crmt.member_id = :userId
           AND msg.created_at > (
               SELECT sub.created_at
               FROM chating_message_history_tab sub
               WHERE sub.message_id = crmt.last_read_message_id
           )
         GROUP BY crmt.room_id
     ) AS unread_counts;
   """, nativeQuery = true)
    int countUnreadMessages(
            @Param("userId") String userId
    );
}
