package com.talktrip.talktrip.domain.chat.repository;

import com.talktrip.talktrip.domain.chat.entity.ChatRoomMember;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, String>{

    @Modifying
    @Transactional
    @Query("UPDATE ChatRoomMember crm SET crm.lastMemberReadTime = CURRENT_TIMESTAMP " +
            "WHERE crm.roomId = :roomId AND crm.memberId = :memberId")
    int updateLastReadTime(@Param("roomId") String roomId,
                           @Param("memberId") String memberId);
}


