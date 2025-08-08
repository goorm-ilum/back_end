package com.talktrip.talktrip.domain.chat.repository;

import com.talktrip.talktrip.domain.chat.entity.ChatRoomMember;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, String>{

    @Modifying
    @Transactional
    @Query("UPDATE ChatRoomMember crm SET crm.lastMemberReadTime = CURRENT_TIMESTAMP " +
            "WHERE crm.roomId = :roomId AND crm.memberId = :memberId")
    int updateLastReadTime(@Param("roomId") String roomId,
                           @Param("memberId") String memberId);

    @Query(value = """
        SELECT crm1.room_id
        FROM chating_room_member_tab crm1
        JOIN chating_room_member_tab crm2 ON crm1.room_id = crm2.room_id
        WHERE crm1.member_id = :buyerId
          AND crm2.member_id = :sellerId
        LIMIT 1
    """, nativeQuery = true)
    Optional<String> findRoomIdByBuyerIdAndSellerId(
            @Param("buyerId") String buyerId,
            @Param("sellerId") String sellerId
    );

    @Modifying
    @Transactional
    @Query("UPDATE ChatRoomMember crm " +
            "SET crm.isDel = :isDel " +
            "WHERE crm.memberId = :memberId AND crm.roomId = :roomId")
    void updateIsDelByMemberIdAndRoomId(@Param("memberId") String memberId,
                                        @Param("roomId") String roomId,
                                        @Param("isDel") int isDel);

}


