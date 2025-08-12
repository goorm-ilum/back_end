package com.talktrip.talktrip.domain.chat.repository;

import com.talktrip.talktrip.domain.chat.entity.ChatRoomAccount;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomAccount, String>{

    @Modifying
    @Transactional
    @Query("UPDATE ChatRoomAccount crm SET crm.lastMemberReadTime = CURRENT_TIMESTAMP " +
            "WHERE crm.roomId = :roomId AND crm.accountEmail = :memberId")
    int updateLastReadTime(@Param("roomId") String roomId,
                           @Param("memberId") String memberId);

    @Query(value = """
        SELECT crm1.room_id
        FROM chatting_room_account_tab crm1
        JOIN chatting_room_account_tab crm2 ON crm1.room_id = crm2.room_id
        WHERE crm1.account_email = :buyerId
          AND crm2.account_email = :sellerId
        LIMIT 1
    """, nativeQuery = true)
    Optional<String> findRoomIdByBuyerIdAndSellerId(
            @Param("buyerId") String buyerId,
            @Param("sellerId") String sellerId
    );

    @Modifying
    @Transactional
    @Query("UPDATE ChatRoomAccount crm " +
            "SET crm.isDel = :isDel " +
            "WHERE crm.accountEmail = :memberId AND crm.roomId = :roomId")
    void updateIsDelByMemberIdAndRoomId(@Param("memberId") String memberId,
                                        @Param("roomId") String roomId,
                                        @Param("isDel") int isDel);
    @Modifying
    @Transactional
    @Query("UPDATE ChatRoomAccount crm SET crm.isDel = 0 WHERE crm.roomId = :roomId")
    void resetIsDelByRoomId(@Param("roomId") String roomId);



}


